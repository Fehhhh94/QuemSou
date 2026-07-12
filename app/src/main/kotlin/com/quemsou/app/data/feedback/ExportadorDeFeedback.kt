package com.quemsou.app.data.feedback

import com.quemsou.app.data.local.FeedbackComResposta
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Monta o JSON de export do modo dev de feedback (formato
 * `quemsou-feedback` versão 1), disparado pela Home via Sharesheet. A
 * `resposta` vem do join com o card no Room ([FeedbackComResposta]) — `null`
 * se o card não existe mais no aparelho. Puro e determinístico ([exportadoEm]
 * entra por parâmetro): testável na JVM com JSON estável.
 */
object ExportadorDeFeedback {

    const val FORMATO = "quemsou-feedback"
    const val VERSAO = 1

    private val json = Json { prettyPrint = true }

    /** O JSON completo do export, com [exportadoEm] em ISO-8601 (UTC). */
    fun montarJson(itens: List<FeedbackComResposta>, exportadoEm: String): String =
        json.encodeToString(
            ExportDeFeedbackJson(
                formato = FORMATO,
                versao = VERSAO,
                exportadoEm = exportadoEm,
                itens = itens.map { item ->
                    ItemDeFeedbackJson(
                        baralhoId = item.feedback.baralhoId,
                        cardId = item.feedback.cardId,
                        resposta = item.resposta,
                        voto = item.feedback.voto,
                        comentario = item.feedback.comentario,
                        rodada = item.feedback.rodada,
                        resultadoDoTurno = item.feedback.resultadoDoTurno,
                        numeroDaDicaDoAcerto = item.feedback.numeroDaDicaDoAcerto,
                        criadoEm = Instant.ofEpochMilli(item.feedback.criadoEm).toString(),
                    )
                },
            ),
        )
}

/** Envelope do export (`formato` + `versao` identificam o arquivo na análise). */
@Serializable
data class ExportDeFeedbackJson(
    val formato: String,
    val versao: Int,
    val exportadoEm: String,
    val itens: List<ItemDeFeedbackJson>,
)

/** Um feedback no export — timestamps em ISO-8601 (UTC) para a análise. */
@Serializable
data class ItemDeFeedbackJson(
    val baralhoId: String,
    val cardId: String,
    val resposta: String?,
    val voto: String,
    val comentario: String?,
    val rodada: Int,
    val resultadoDoTurno: String,
    val numeroDaDicaDoAcerto: Int?,
    val criadoEm: String,
)
