package com.quemsou.app.domain.rules

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbaralhadorDeCardsTest {

    private fun card(id: String) = Card(
        id = id,
        type = CardType.PESSOA,
        category = CardCategory.LIVRE,
        answer = "Resposta $id",
        clues = List(Card.QUANTIDADE_DE_DICAS) { "Dica ${it + 1} de $id" },
    )

    private fun baralho(tamanho: Int) = List(tamanho) { card("card-$it") }

    @Test
    fun `mesma seed gera a mesma ordem`() {
        val cards = baralho(30)

        val primeira = EmbaralhadorDeCards.embaralhar(cards, seed = 42L)
        val segunda = EmbaralhadorDeCards.embaralhar(cards, seed = 42L)

        assertEquals(primeira, segunda)
    }

    @Test
    fun `seeds diferentes geram ordens diferentes`() {
        val cards = baralho(30)

        val comSeed1 = EmbaralhadorDeCards.embaralhar(cards, seed = 1L)
        val comSeed2 = EmbaralhadorDeCards.embaralhar(cards, seed = 2L)

        assertNotEquals(comSeed1, comSeed2)
    }

    @Test
    fun `resultado e permutacao exata da lista original`() {
        val cards = baralho(30)

        val embaralhado = EmbaralhadorDeCards.embaralhar(cards, seed = 42L)

        assertEquals(cards.size, embaralhado.size)
        assertEquals(
            cards.map { it.id }.sorted(),
            embaralhado.map { it.id }.sorted(),
        )
        assertTrue(embaralhado.map { it.id }.toSet().size == embaralhado.size)
    }

    @Test
    fun `lista vazia nao quebra`() {
        assertEquals(emptyList<Card>(), EmbaralhadorDeCards.embaralhar(emptyList<Card>(), seed = 42L))
    }

    @Test
    fun `lista de um card nao quebra e mantem o card`() {
        val cards = listOf(card("unico"))

        assertEquals(cards, EmbaralhadorDeCards.embaralhar(cards, seed = 42L))
    }

    @Test
    fun `seed zero nao quebra e ainda embaralha`() {
        val cards = baralho(30)

        val primeira = EmbaralhadorDeCards.embaralhar(cards, seed = 0L)
        val segunda = EmbaralhadorDeCards.embaralhar(cards, seed = 0L)

        assertEquals(primeira, segunda)
        assertNotEquals(cards, primeira)
    }
}
