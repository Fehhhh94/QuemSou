package com.quemsou.app.data.feedback

import com.quemsou.app.data.local.FeedbackComResposta
import com.quemsou.app.data.local.FeedbackDeCardEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportadorDeFeedbackTest {

    private fun feedback(
        id: Long,
        cardId: String,
        voto: String,
        comentario: String? = null,
        resultadoDoTurno: String = "ACERTO",
        numeroDaDicaDoAcerto: Int? = 3,
        criadoEm: Long = 1_752_192_000_000, // 2025-07-11T00:00:00Z
    ) = FeedbackDeCardEntity(
        id = id,
        baralhoId = "cinema-classico-1",
        cardId = cardId,
        voto = voto,
        comentario = comentario,
        rodada = 2,
        resultadoDoTurno = resultadoDoTurno,
        numeroDaDicaDoAcerto = numeroDaDicaDoAcerto,
        criadoEm = criadoEm,
    )

    @Test
    fun `mapeia entidade e resposta do join para o formato quemsou-feedback v1`() {
        val itens = listOf(
            FeedbackComResposta(
                feedback = feedback(id = 1, cardId = "cc1-01", voto = "BOM", comentario = "dica 3 entrega"),
                resposta = "CHAPLIN",
            ),
            FeedbackComResposta(
                feedback = feedback(
                    id = 2,
                    cardId = "cc1-02",
                    voto = "FRACO",
                    resultadoDoTurno = "QUEIMADO",
                    numeroDaDicaDoAcerto = null,
                ),
                // Card removido do aparelho: o LEFT JOIN devolve resposta nula.
                resposta = null,
            ),
        )

        val json = ExportadorDeFeedback.montarJson(itens, exportadoEm = "2026-07-11T12:00:00Z")

        assertEquals(
            """
            {
                "formato": "quemsou-feedback",
                "versao": 1,
                "exportadoEm": "2026-07-11T12:00:00Z",
                "itens": [
                    {
                        "baralhoId": "cinema-classico-1",
                        "cardId": "cc1-01",
                        "resposta": "CHAPLIN",
                        "voto": "BOM",
                        "comentario": "dica 3 entrega",
                        "rodada": 2,
                        "resultadoDoTurno": "ACERTO",
                        "numeroDaDicaDoAcerto": 3,
                        "criadoEm": "2025-07-11T00:00:00Z"
                    },
                    {
                        "baralhoId": "cinema-classico-1",
                        "cardId": "cc1-02",
                        "resposta": null,
                        "voto": "FRACO",
                        "comentario": null,
                        "rodada": 2,
                        "resultadoDoTurno": "QUEIMADO",
                        "numeroDaDicaDoAcerto": null,
                        "criadoEm": "2025-07-11T00:00:00Z"
                    }
                ]
            }
            """.trimIndent(),
            json,
        )
    }

    @Test
    fun `sem registros o export e um envelope com itens vazios`() {
        val json = ExportadorDeFeedback.montarJson(emptyList(), exportadoEm = "2026-07-11T12:00:00Z")

        assertEquals(
            """
            {
                "formato": "quemsou-feedback",
                "versao": 1,
                "exportadoEm": "2026-07-11T12:00:00Z",
                "itens": []
            }
            """.trimIndent(),
            json,
        )
    }
}
