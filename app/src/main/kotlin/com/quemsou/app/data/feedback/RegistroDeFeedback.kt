package com.quemsou.app.data.feedback

import com.quemsou.app.data.local.FeedbackComResposta
import com.quemsou.app.data.local.FeedbackDeCardDao
import com.quemsou.app.data.local.FeedbackDeCardEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Veredito do dev sobre um card jogado (modo dev de feedback). */
enum class VotoDeCard { BOM, FRACO }

/** Como o turno do card avaliado terminou. */
enum class ResultadoDoTurnoRegistrado { ACERTO, QUEIMADO }

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
)

/**
 * Registro dos feedbacks do modo dev (5B parte 2). Histórico completo:
 * gravar **sempre insere** — o mesmo card pode ser avaliado de novo em outra
 * partida. Abstraído para os testes JVM dos ViewModels usarem um fake.
 */
interface RegistroDeFeedback {

    /** Grava [novo] como uma linha nova do histórico. */
    suspend fun registrar(novo: NovoFeedback)

    /** Contagem viva de registros (o "N" do export da Home). */
    fun quantidade(): Flow<Int>

    /** Todos os registros com a resposta do card junto, para o export. */
    suspend fun buscarTodosComResposta(): List<FeedbackComResposta>

    /** Apaga todo o histórico (ação "Limpar feedback", com confirmação). */
    suspend fun apagarTudo()
}

/** Implementação real: persiste no Room e carimba o `criadoEm` na gravação. */
class RegistroDeFeedbackLocal @Inject constructor(
    private val dao: FeedbackDeCardDao,
) : RegistroDeFeedback {

    override suspend fun registrar(novo: NovoFeedback) {
        dao.inserir(
            FeedbackDeCardEntity(
                baralhoId = novo.baralhoId,
                cardId = novo.cardId,
                voto = novo.voto.name,
                comentario = novo.comentario,
                rodada = novo.rodada,
                resultadoDoTurno = novo.resultadoDoTurno.name,
                numeroDaDicaDoAcerto = novo.numeroDaDicaDoAcerto,
                criadoEm = System.currentTimeMillis(),
            ),
        )
    }

    override fun quantidade(): Flow<Int> = dao.contar()

    override suspend fun buscarTodosComResposta(): List<FeedbackComResposta> =
        dao.buscarTodosComResposta()

    override suspend fun apagarTudo() {
        dao.apagarTudo()
    }
}
