package com.quemsou.app.data.importer

import com.quemsou.app.data.catalogo.ParserDoCatalogo
import com.quemsou.app.data.catalogo.ResultadoDoParse
import com.quemsou.app.data.local.BaralhoDao
import com.quemsou.app.data.local.CardDao
import com.quemsou.app.data.local.paraEntidade
import javax.inject.Inject

/**
 * Importa os baralhos embarcados de `assets/cards.json` para o banco Room,
 * com versionamento: só recarrega quando a versão do asset é maior que a
 * última importada.
 *
 * A importação é **cirúrgica**: substitui cada baralho embarcado (linha +
 * cards dele), sem tocar nos baralhos baixados do catálogo — um update do
 * app nunca apaga downloads do usuário.
 *
 * Cada baralho passa pela validação do [ParserDoCatalogo] (estrutura +
 * `ValidadorDeBaralho`); um baralho inválido interrompe a importação com
 * [IllegalArgumentException] carregando as violações legíveis — falha
 * ruidosa por design: asset embarcado quebrado é bug de release.
 */
class CardsImporter @Inject constructor(
    private val fonte: FonteDeCardsJson,
    private val parser: ParserDoCatalogo,
    private val baralhoDao: BaralhoDao,
    private val cardDao: CardDao,
    private val versionStore: CardsVersionStore,
) {

    /**
     * Compara a versão do asset com a última importada e recarrega as tabelas
     * `baralhos` e `cards` se o asset for mais novo; caso contrário não toca
     * no banco.
     */
    suspend fun importarSeNecessario(): ResultadoImportacao {
        val cardsJson = CardsJson.deJson(fonte.ler())
        val versaoImportada = versionStore.versaoImportada()
        if (cardsJson.version <= versaoImportada) {
            return ResultadoImportacao.NadaAFazer(versaoImportada)
        }

        val baralhos = cardsJson.baralhos.map { baralhoJson ->
            when (val resultado = parser.validarBaralho(baralhoJson)) {
                is ResultadoDoParse.Sucesso -> resultado.valor
                is ResultadoDoParse.Falha -> throw IllegalArgumentException(
                    "Baralho embarcado '${baralhoJson.id}' inválido: " +
                        resultado.violacoes.joinToString("; ") { it.mensagem },
                )
            }
        }

        baralhos.forEach { baralho ->
            baralhoDao.inserirTodos(listOf(baralho.paraEntidade()))
            cardDao.removerPorBaralho(baralho.id)
            cardDao.inserirTodos(baralho.cards.map { it.paraEntidade(baralho.id) })
        }
        versionStore.salvarVersaoImportada(cardsJson.version)
        return ResultadoImportacao.Importado(
            quantidade = baralhos.sumOf { it.quantidadeDeCards },
            versao = cardsJson.version,
        )
    }
}

/** Desfecho de uma chamada a [CardsImporter.importarSeNecessario]. */
sealed interface ResultadoImportacao {

    /** As tabelas foram recarregadas com [quantidade] cards da versão [versao]. */
    data class Importado(val quantidade: Int, val versao: Int) : ResultadoImportacao

    /** O banco já estava na versão [versao] (ou mais nova); nada foi alterado. */
    data class NadaAFazer(val versao: Int) : ResultadoImportacao
}
