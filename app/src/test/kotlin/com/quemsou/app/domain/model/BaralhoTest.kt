package com.quemsou.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BaralhoTest {

    private fun card(id: String, categoria: CardCategory = CardCategory.PERSONAGEM_FILME) = Card(
        id = id,
        type = CardType.PESSOA,
        category = categoria,
        answer = "Resposta $id",
        clues = List(Card.QUANTIDADE_DE_DICAS) { "Dica ${it + 1} de $id" },
    )

    private fun baralho(
        id: String,
        cards: List<Card>,
        categoria: CardCategory = CardCategory.PERSONAGEM_FILME,
        estado: EstadoDoBaralho = EstadoDoBaralho.FINALIZADO,
    ) = Baralho(
        id = id,
        nome = "Baralho $id",
        categoria = categoria,
        versao = 1,
        estado = estado,
        cards = cards,
    )

    @Test
    fun `contagem de cards e derivada da lista`() {
        assertEquals(3, baralho("b1", cards = listOf(card("c1"), card("c2"), card("c3"))).quantidadeDeCards)
    }

    @Test
    fun `id ou nome vazios e versao menor que 1 sao rejeitados`() {
        assertThrows(IllegalArgumentException::class.java) { baralho("  ", cards = listOf(card("c1"))) }
        assertThrows(IllegalArgumentException::class.java) {
            baralho("b1", cards = listOf(card("c1"))).copy(nome = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            baralho("b1", cards = listOf(card("c1"))).copy(versao = 0)
        }
    }

    @Test
    fun `ciclo de vida tem os dois estados e o baralho carrega o seu`() {
        val emEvolucao = baralho("b1", cards = listOf(card("c1")), estado = EstadoDoBaralho.EM_DESENVOLVIMENTO)
        val edicaoFinal = baralho("b2", cards = listOf(card("c2")), estado = EstadoDoBaralho.FINALIZADO)

        assertEquals(EstadoDoBaralho.EM_DESENVOLVIMENTO, emEvolucao.estado)
        assertEquals(EstadoDoBaralho.FINALIZADO, edicaoFinal.estado)
        assertEquals(2, EstadoDoBaralho.entries.size)
    }

    // region União determinística

    @Test
    fun `uniao ordena por id de baralho e id de card, independente da ordem de entrada`() {
        // Cards em ordem embaralhada dentro de cada baralho — simulando a
        // ordem de inserção no Room, que não é garantida.
        val cinema = baralho("a-cinema", cards = listOf(card("c3"), card("c1"), card("c2")))
        val musica = baralho(
            "b-musica",
            cards = listOf(card("m2", CardCategory.MUNDO_DA_MUSICA), card("m1", CardCategory.MUNDO_DA_MUSICA)),
            categoria = CardCategory.MUNDO_DA_MUSICA,
        )

        val monte = Baralho.uniaoDeterministica(listOf(musica, cinema))

        assertEquals(listOf("c1", "c2", "c3", "m1", "m2"), monte.map { it.id })
    }

    @Test
    fun `mesma selecao em ordens diferentes gera o mesmo monte`() {
        // O teste do determinismo prometido: inserção/seleção em ordens
        // diferentes → monte idêntico, card a card.
        val cinema = baralho("a-cinema", cards = listOf(card("c2"), card("c1")))
        val musica = baralho(
            "b-musica",
            cards = listOf(card("m1", CardCategory.MUNDO_DA_MUSICA)),
            categoria = CardCategory.MUNDO_DA_MUSICA,
        )
        val cinemaComOutraOrdemInterna = cinema.copy(cards = listOf(card("c1"), card("c2")))

        assertEquals(
            Baralho.uniaoDeterministica(listOf(cinema, musica)),
            Baralho.uniaoDeterministica(listOf(musica, cinemaComOutraOrdemInterna)),
        )
    }

    @Test
    fun `uniao de um unico baralho e a lista dos seus cards ordenada por id`() {
        val unico = baralho("b1", cards = listOf(card("c2"), card("c1"), card("c3")))

        assertEquals(listOf("c1", "c2", "c3"), Baralho.uniaoDeterministica(listOf(unico)).map { it.id })
    }

    @Test
    fun `baralho repetido na selecao conta uma unica vez`() {
        val unico = baralho("b1", cards = listOf(card("c1"), card("c2")))

        assertEquals(2, Baralho.uniaoDeterministica(listOf(unico, unico)).size)
    }

    // endregion
}
