package com.quemsou.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CardTest {

    private fun cardComDicas(quantidade: Int) = Card(
        id = "card-teste",
        type = CardType.COISA,
        category = CardCategory.LIVRE,
        answer = "Violão",
        clues = List(quantidade) { "Dica ${it + 1}" },
    )

    @Test
    fun `card com 10 dicas e valido`() {
        val card = cardComDicas(10)

        assertEquals(10, card.clues.size)
    }

    @Test
    fun `card com 9 dicas lanca excecao`() {
        assertThrows(IllegalArgumentException::class.java) { cardComDicas(9) }
    }

    @Test
    fun `card com 11 dicas lanca excecao`() {
        assertThrows(IllegalArgumentException::class.java) { cardComDicas(11) }
    }
}
