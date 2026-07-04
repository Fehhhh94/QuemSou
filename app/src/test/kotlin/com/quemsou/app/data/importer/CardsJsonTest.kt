package com.quemsou.app.data.importer

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CardsJsonTest {

    // region Parsing

    @Test
    fun `json valido e parseado com versao e cards`() {
        val conteudo = """
            {
              "version": 3,
              "cards": [
                {
                  "id": "teste-01",
                  "type": "PESSOA",
                  "category": "PERSONAGEM_FILME",
                  "answer": "TESTE_01",
                  "clues": ["d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8", "d9", "d10"]
                }
              ]
            }
        """.trimIndent()

        val cardsJson = CardsJson.deJson(conteudo)

        assertEquals(3, cardsJson.version)
        assertEquals(1, cardsJson.cards.size)
        val card = cardsJson.cards.first()
        assertEquals("teste-01", card.id)
        assertEquals(CardType.PESSOA, card.type)
        assertEquals(CardCategory.PERSONAGEM_FILME, card.category)
        assertEquals("TESTE_01", card.answer)
        assertEquals(10, card.clues.size)
    }

    @Test
    fun `json sem campo obrigatorio lanca excecao`() {
        val semAnswer = """
            {
              "version": 1,
              "cards": [
                {
                  "id": "teste-01",
                  "type": "PESSOA",
                  "category": "LIVRE",
                  "clues": ["d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8", "d9", "d10"]
                }
              ]
            }
        """.trimIndent()

        assertThrows(SerializationException::class.java) { CardsJson.deJson(semAnswer) }
    }

    // endregion

    // region Validação na conversão para domínio

    private fun cardJson(
        answer: String = "TESTE_01",
        clues: List<String> = List(10) { "dica ${it + 1}" },
    ) = CardJson(
        id = "teste-01",
        type = CardType.COISA,
        category = CardCategory.LIVRE,
        answer = answer,
        clues = clues,
    )

    @Test
    fun `card valido e convertido para o dominio`() {
        val card: Card = cardJson().paraDominio()

        assertEquals("teste-01", card.id)
        assertEquals("TESTE_01", card.answer)
    }

    @Test
    fun `card com 9 dicas falha com o id na mensagem`() {
        val excecao = assertThrows(IllegalArgumentException::class.java) {
            cardJson(clues = List(9) { "dica ${it + 1}" }).paraDominio()
        }

        assertTrue(excecao.message!!.contains("teste-01"))
    }

    @Test
    fun `card com dica vazia falha com o id na mensagem`() {
        val dicas = List(9) { "dica ${it + 1}" } + " "
        val excecao = assertThrows(IllegalArgumentException::class.java) {
            cardJson(clues = dicas).paraDominio()
        }

        assertTrue(excecao.message!!.contains("teste-01"))
    }

    @Test
    fun `card com answer vazio falha com o id na mensagem`() {
        val excecao = assertThrows(IllegalArgumentException::class.java) {
            cardJson(answer = "  ").paraDominio()
        }

        assertTrue(excecao.message!!.contains("teste-01"))
    }

    // endregion
}
