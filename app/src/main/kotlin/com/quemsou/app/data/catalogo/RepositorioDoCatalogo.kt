package com.quemsou.app.data.catalogo

import com.quemsou.app.data.local.BaralhoDao
import com.quemsou.app.data.local.CardDao
import com.quemsou.app.data.local.paraEntidade
import com.quemsou.app.domain.model.Baralho
import java.io.IOException
import javax.inject.Inject

/**
 * Orquestra o catálogo: baixa o índice (com fallback para o [CacheDoIndice]
 * quando offline), cruza as entradas com o que já está no Room para derivar o
 * estado local de cada baralho, e baixa/valida/grava baralhos. Baralho
 * inválido NUNCA entra no Room — a falha volta legível para a tela.
 */
class RepositorioDoCatalogo @Inject constructor(
    private val fonte: FonteDoCatalogo,
    private val cache: CacheDoIndice,
    private val parser: ParserDoCatalogo,
    private val baralhoDao: BaralhoDao,
    private val cardDao: CardDao,
) {

    /**
     * Carrega o catálogo: rede primeiro (salvando o índice no cache);
     * sem rede, o último índice em cache com a flag [CatalogoCarregado.offline].
     * Índice remoto inválido também cai para o cache — conteúdo velho e
     * válido vale mais que novo e quebrado.
     */
    suspend fun carregarCatalogo(): ResultadoDoCatalogo {
        val daRede = try {
            fonte.buscarIndice()
        } catch (excecao: IOException) {
            null
        }
        if (daRede != null) {
            when (val resultado = parser.parseIndice(daRede)) {
                is ResultadoDoParse.Sucesso -> {
                    cache.salvar(daRede)
                    return ResultadoDoCatalogo.Disponivel(
                        CatalogoCarregado(itens = cruzarComLocal(resultado.valor), offline = false),
                    )
                }

                is ResultadoDoParse.Falha -> Unit // cai para o cache abaixo
            }
        }
        val doCache = cache.ler() ?: return ResultadoDoCatalogo.Indisponivel
        return when (val resultado = parser.parseIndice(doCache)) {
            is ResultadoDoParse.Sucesso -> ResultadoDoCatalogo.Disponivel(
                CatalogoCarregado(itens = cruzarComLocal(resultado.valor), offline = true),
            )

            is ResultadoDoParse.Falha -> ResultadoDoCatalogo.Indisponivel
        }
    }

    /**
     * Baixa, valida e grava o baralho da [entrada]. O download reporta
     * progresso em [aoProgresso] (0f–1f) e é cancelável pelo cancelamento da
     * coroutine. Só conteúdo aprovado pelo [ParserDoCatalogo] toca o Room.
     */
    suspend fun baixarBaralho(
        entrada: EntradaDoCatalogo,
        aoProgresso: (Float) -> Unit,
    ): ResultadoDoDownload {
        val conteudo = try {
            fonte.baixarBaralho(entrada.url, aoProgresso)
        } catch (excecao: IOException) {
            return ResultadoDoDownload.Falha(
                "Não deu para baixar \"${entrada.nome}\" — confira a conexão e tente de novo.",
            )
        }
        val baralho = when (val resultado = parser.parseBaralho(conteudo)) {
            is ResultadoDoParse.Sucesso -> resultado.valor
            is ResultadoDoParse.Falha -> return ResultadoDoDownload.Falha(
                "O baralho \"${entrada.nome}\" veio inválido do catálogo: " +
                    resultado.violacoes.joinToString("; ") { it.mensagem },
            )
        }
        if (baralho.id != entrada.id) {
            return ResultadoDoDownload.Falha(
                "O catálogo apontou para o baralho errado " +
                    "(esperava '${entrada.id}', veio '${baralho.id}').",
            )
        }
        gravar(baralho)
        return ResultadoDoDownload.Sucesso(baralho)
    }

    /** Grava/atualiza o [baralho]: substitui a linha e os cards dele — só dele. */
    private suspend fun gravar(baralho: Baralho) {
        baralhoDao.inserirTodos(listOf(baralho.paraEntidade()))
        cardDao.removerPorBaralho(baralho.id)
        cardDao.inserirTodos(baralho.cards.map { it.paraEntidade(baralho.id) })
    }

    private suspend fun cruzarComLocal(entradas: List<EntradaDoCatalogo>): List<ItemDoCatalogo> {
        val versaoLocalPorId = baralhoDao.buscarTodos().associate { it.id to it.versao }
        return entradas.map { entrada ->
            val versaoLocal = versaoLocalPorId[entrada.id]
            ItemDoCatalogo(
                entrada = entrada,
                versaoLocal = versaoLocal,
                estadoLocal = when {
                    versaoLocal == null -> EstadoLocalDoBaralho.NAO_BAIXADO
                    entrada.versao > versaoLocal -> EstadoLocalDoBaralho.ATUALIZACAO_DISPONIVEL
                    else -> EstadoLocalDoBaralho.BAIXADO
                },
            )
        }
    }
}

/** Desfecho de [RepositorioDoCatalogo.carregarCatalogo]. */
sealed interface ResultadoDoCatalogo {

    /** Catálogo em mãos — da rede ou do cache ([CatalogoCarregado.offline]). */
    data class Disponivel(val catalogo: CatalogoCarregado) : ResultadoDoCatalogo

    /** Sem rede e sem cache utilizável: nada a mostrar além do aviso. */
    data object Indisponivel : ResultadoDoCatalogo
}

/**
 * O catálogo pronto para a tela.
 *
 * @property offline `true` quando veio do cache por falta de rede — a UI
 *   mostra o banner e esmaece os não-baixados.
 */
data class CatalogoCarregado(
    val itens: List<ItemDoCatalogo>,
    val offline: Boolean,
)

/** Uma entrada do índice cruzada com o estado local do Room. */
data class ItemDoCatalogo(
    val entrada: EntradaDoCatalogo,
    val versaoLocal: Int?,
    val estadoLocal: EstadoLocalDoBaralho,
)

/**
 * Estado local de um baralho do catálogo. O estado transitório "baixando"
 * (com progresso e cancelamento) vive no ViewModel, não aqui.
 */
enum class EstadoLocalDoBaralho {
    NAO_BAIXADO,
    BAIXADO,

    /**
     * Há versão mais nova no índice do que no aparelho — na prática só
     * acontece com baralhos `EM_DESENVOLVIMENTO` (um `FINALIZADO` nunca muda
     * de versão; a regra é editorial, não do app).
     */
    ATUALIZACAO_DISPONIVEL,
}

/** Desfecho de [RepositorioDoCatalogo.baixarBaralho]. */
sealed interface ResultadoDoDownload {

    /** Baralho validado e gravado no Room. */
    data class Sucesso(val baralho: Baralho) : ResultadoDoDownload

    /** Nada foi gravado; [mensagem] legível para a tela. */
    data class Falha(val mensagem: String) : ResultadoDoDownload
}
