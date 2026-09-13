package com.quemsou.app.data

import androidx.room.withTransaction
import com.quemsou.app.data.local.*
import com.quemsou.app.domain.model.*
import com.quemsou.app.domain.repository.RepositorioDeCards
import com.quemsou.app.domain.rules.AcervoDeRespostas
import com.quemsou.app.domain.rules.SelecionadorDeDicas
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Conteúdo editorial, reservas temporárias e histórico pessoal têm ciclos de vida distintos. */
class RepositorioDeCardsLocal @Inject constructor(
    private val baralhoDao: BaralhoDao,
    private val cardDao: CardDao,
    private val database: AppDatabase,
) : RepositorioDeCards {
    private val dao get() = database.historicoDeDicasDao()

    private suspend fun acervo(): List<Baralho> {
        val baralhos = baralhoDao.buscarTodos()
        val cards = cardDao.buscarPorBaralhos(baralhos.map { it.id }).groupBy { it.baralhoId }
        return AcervoDeRespostas.compartilhar(baralhos.map { b ->
            b.paraDominio(cards[b.id].orEmpty().map { it.paraDominio() })
        })
    }

    override suspend fun buscarPorIds(ids: List<String>): List<Baralho> = database.withTransaction {
        acervo().filter { it.id in ids }
    }

    override suspend fun buscarTodos(): List<Baralho> = database.withTransaction {
        disponiveis(acervo(), dao.usadas().toSet())
    }

    private fun disponiveis(baralhos: List<Baralho>, usadas: Set<String>): List<Baralho> =
        baralhos.map { b -> b.copy(cards = AcervoDeRespostas.semRepeticoes(b.cards.sortedBy { it.id }).filter {
            SelecionadorDeDicas.disponiveis(it, usadas).size >= Card.QUANTIDADE_DE_DICAS
        }) }

    override suspend fun prepararSessao(sessao: String, ids: List<String>): List<Baralho> = database.withTransaction {
        val existente = dao.sessao(sessao)
        if (existente != null) {
            check(!existente.encerrada) { "Esta partida já foi abandonada." }
            SnapshotsDaPartida.baralhos(existente.baralhosJson)
        } else {
            // Há uma partida ativa no celular. Começar outra libera reservas das sessões abandonadas.
            dao.sessoesAtivas().forEach { dao.liberarSessao(it); dao.encerrar(it) }
            val baralhos = disponiveis(acervo().filter { it.id in ids }, dao.usadas().toSet())
            val historico = dao.respostas().associate { it.chave to it.ordem }
            dao.inserirSessao(SessaoDeDicasEntity(sessao, SnapshotsDaPartida.salvar(baralhos), Json.encodeToString(historico)))
            baralhos
        }
    }

    override suspend fun historicoDaSessao(sessao: String): Map<String, Long> =
        Json.decodeFromString(requireNotNull(dao.sessao(sessao)).historicoJson)

    override suspend fun progressoDaSessao(sessao: String): ProgressoDaPartida? =
        dao.sessao(sessao)?.let { SnapshotsDaPartida.progresso(it.progressoJson) }

    override suspend fun prepararTurno(sessao: String, rodada: Int, card: Card, seed: Long): Card = database.withTransaction {
        check(dao.sessao(sessao)?.encerrada == false) { "Sessão indisponível." }
        val existente = dao.turno(sessao, rodada)
        if (existente != null) SnapshotsDaPartida.card(existente.cardJson, card.category)
        else {
            val preparado = SelecionadorDeDicas.preparar(card, (dao.usadas() + dao.reservadas()).toSet(), seed)
                ?: throw IllegalStateException("Dicas inéditas insuficientes")
            val chaves = preparado.bancoDeDicas.flatMap { SelecionadorDeDicas.chaves(preparado, it) }.distinct()
            dao.reservar(chaves.map { DicaReservadaEntity(it, sessao, rodada) })
            dao.inserirTurno(TurnoDeDicasEntity(sessao, rodada, SnapshotsDaPartida.salvar(preparado)))
            preparado
        }
    }

    override suspend fun salvarProgresso(sessao: String, progresso: ProgressoDaPartida,
        dicasReveladas: List<String>, encerrarTurno: Boolean) = database.withTransaction {
        val sessaoSalva = requireNotNull(dao.sessao(sessao))
        check(!sessaoSalva.encerrada) { "Esta partida já foi abandonada." }
        val anterior = SnapshotsDaPartida.progresso(sessaoSalva.progressoJson)
        val salvo = dao.turno(sessao, progresso.rodada)
        if (salvo != null && progresso.fase != "VEZ_DE_JOGAR" && progresso.fase != "PLACAR_FINAL") {
            val card = SnapshotsDaPartida.card(salvo.cardJson, CardCategory.PERSONAGEM_FILME)
            // Só conta uma aparição quando a rodada abre, nunca ao montar o monte.
            if (anterior == null || anterior.rodada != progresso.rodada || anterior.fase == "VEZ_DE_JOGAR") {
                val ordem = (dao.respostas().maxOfOrNull { it.ordem } ?: 0L) + 1L
                dao.registrarRespostas(AcervoDeRespostas.chaves(card).map { RespostaJogadaEntity(it, ordem) })
            }
            require(dicasReveladas.size == progresso.posicoes.size)
            val dicas = dicasReveladas.map { texto ->
                requireNotNull(SelecionadorDeDicas.banco(card).singleOrNull { it.texto == texto })
            }
            dao.registrar(dicas.flatMap { SelecionadorDeDicas.chaves(card, it) }.distinct().map { DicaUtilizadaEntity(it) })
        } else require(dicasReveladas.isEmpty())
        if (encerrarTurno) dao.liberarTurno(sessao, progresso.rodada)
        dao.salvarProgresso(sessao, SnapshotsDaPartida.salvar(progresso))
    }

    override suspend fun encerrarSessao(sessao: String) = database.withTransaction {
        dao.liberarSessao(sessao)
        dao.encerrar(sessao)
    }
}