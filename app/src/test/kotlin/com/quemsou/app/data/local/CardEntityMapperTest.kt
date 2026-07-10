package com.quemsou.app.data.local

import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.Colecao
import com.quemsou.app.domain.model.EstadoDoBaralho
import org.junit.Assert.assertEquals
import org.junit.Test

class CardEntityMapperTest {

    private val card = Card(
        id = "card-teste",
        type = CardType.PESSOA,
        category = CardCategory.MUNDO_DA_MUSICA,
        answer = "Violão",
        clues = List(10) { "Dica ${it + 1}" },
    )

    private val baralho = Baralho(
        id = "musica-1",
        nome = "Mundo da Música — Edição 1",
        categoria = CardCategory.MUNDO_DA_MUSICA,
        colecao = Colecao(id = "mundo-da-musica", nome = "Mundo da Música", icone = "🎸"),
        versao = 2,
        estado = EstadoDoBaralho.EM_DESENVOLVIMENTO,
        cards = listOf(card),
    )

    @Test
    fun `ida e volta dominio - entidade - dominio preserva o card`() {
        assertEquals(card, card.paraEntidade(baralhoId = "musica-1").paraDominio())
    }

    @Test
    fun `ida e volta entidade - dominio - entidade preserva a entidade e o vinculo com o baralho`() {
        val entidade = card.paraEntidade(baralhoId = "musica-1")

        assertEquals(entidade, entidade.paraDominio().paraEntidade(baralhoId = entidade.baralhoId))
    }

    @Test
    fun `enums sao gravados pelo name`() {
        val entidade = card.paraEntidade(baralhoId = "musica-1")

        assertEquals("PESSOA", entidade.type)
        assertEquals("MUNDO_DA_MUSICA", entidade.category)
        assertEquals("musica-1", entidade.baralhoId)
    }

    @Test
    fun `ida e volta do baralho preserva metadados e cards`() {
        val entidade = baralho.paraEntidade()

        assertEquals("MUNDO_DA_MUSICA", entidade.categoria)
        assertEquals("EM_DESENVOLVIMENTO", entidade.estado)
        assertEquals(baralho, entidade.paraDominio(cards = listOf(card)))
    }
}
