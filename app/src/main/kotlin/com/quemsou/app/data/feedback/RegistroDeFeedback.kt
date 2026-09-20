package com.quemsou.app.data.feedback

import com.quemsou.app.data.local.FeedbackComResposta
import com.quemsou.app.data.local.FeedbackDeCardDao
import com.quemsou.app.data.local.FeedbackDeCardEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Veredito do dev sobre um card jogado (modo dev de feedback). */
enum class VotoDeCard { BOM, FRACO }

/** Como o turno do card avaliado terminou. */
enum class ResultadoDoTurnoRegistrado { ACERTO, QUEIMADO, DICA_REVELADA }

/** Snapshot só da dica avaliada; nunca exporta as outras nove dicas ocultas. */
@kotlinx.serialization.Serializable
data class ContextoDeFeedbackDaDica(
    val sessaoId: String,
    val rodada: Int,
    val posicao: Int,
    val respostaId: String,
    val resposta: String,
    val dicaId: String,
    val texto: String,
    val versaoDoBaralho: Int,
)

/** Cópia editorial do que foi jogado, independente de alterações futuras no catálogo. */
@kotlinx.serialization.Serializable
data class ContextoDeFeedback(
    val card: com.quemsou.app.data.catalogo.CardDoBaralhoJson,
    val versaoDoBaralho: Int,
    val dicasReveladas: List<String>,
)

/**
 * Um feedback pronto para gravar — tudo menos o que a persistência gera
 * (id autogerado e `criadoEm`, carimbado pela implementação real).
 *
 * @property comentario comentário opcional; `null` se não houver.
 * @property numeroDaDicaDoAcerto dica em que houve o acerto; `null` se queimado.
 */
data class NovoFeedback(
    val baralhoId: String,
    val cardId: String,
    val voto: VotoDeCard,
    val comentario: String?,
    val rodada: Int,
    val resultadoDoTurno: ResultadoDoTurnoRegistrado,
    val numeroDaDicaDoAcerto: Int?,
    val contextoJson: String = "",
)

/**
 * Histórico de cartas e dicas. O voto de dica é substituído na mesma ocorrência;
 * outras partidas e rodadas mantêm registros independentes.
 */
interface RegistroDeFeedback {

    /** Grava [novo]; dica com o mesmo contexto substitui apenas o voto/comentário. */
    suspend fun registrar(novo: NovoFeedback)

    /** Contagem viva de registros (o "N" do export da Home). */
    fun quantidade(): Flow<Int>

    /** Todos os registros com a resposta do card junto, para o export. */
    suspend fun buscarTodosComResposta(): List<FeedbackComResposta>

    suspend fun buscarDica(contextoJson: String): FeedbackDeCardEntity? =
        buscarTodosComResposta().firstOrNull {
            it.feedback.resultadoDoTurno == ResultadoDoTurnoRegistrado.DICA_REVELADA.name &&
                it.feedback.contextoJson == contextoJson
        }?.feedback

    /** Apaga todo o histórico (ação "Limpar feedback", com confirmação). */
    suspend fun apagarTudo()
}

/** Implementação real: persiste no Room e carimba o `criadoEm` na gravação. */
class RegistroDeFeedbackLocal @Inject constructor(
    private val dao: FeedbackDeCardDao,
    private val agendador: AgendadorDaSincronizacaoDeFeedback = AgendadorDaSincronizacaoNulo,
) : RegistroDeFeedback {

    override suspend fun registrar(novo: NovoFeedback) {
        val registrado = dao.registrar(
            FeedbackDeCardEntity(
                baralhoId = novo.baralhoId,
                cardId = novo.cardId,
                voto = novo.voto.name,
                comentario = novo.comentario,
                rodada = novo.rodada,
                resultadoDoTurno = novo.resultadoDoTurno.name,
                numeroDaDicaDoAcerto = novo.numeroDaDicaDoAcerto,
                criadoEm = System.currentTimeMillis(),
                contextoJson = novo.contextoJson,
            ),
        )
        if (registrado.resultadoDoTurno == ResultadoDoTurnoRegistrado.DICA_REVELADA.name) {
            agendador.agendar()
        }
    }

    override fun quantidade(): Flow<Int> = dao.contar()

    override suspend fun buscarDica(contextoJson: String): FeedbackDeCardEntity? =
        dao.buscarDica(contextoJson)

    override suspend fun buscarTodosComResposta(): List<FeedbackComResposta> =
        dao.buscarTodosComResposta()

    override suspend fun apagarTudo() {
        dao.apagarTudo()
    }
}
