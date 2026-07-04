package com.quemsou.app.data.local

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
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

    @Test
    fun `ida e volta dominio - entidade - dominio preserva o card`() {
        assertEquals(card, card.paraEntidade().paraDominio())
    }

    @Test
    fun `ida e volta entidade - dominio - entidade preserva a entidade`() {
        val entidade = card.paraEntidade()

        assertEquals(entidade, entidade.paraDominio().paraEntidade())
    }

    @Test
    fun `enums sao gravados pelo name`() {
        val entidade = card.paraEntidade()

        assertEquals("PESSOA", entidade.type)
        assertEquals("MUNDO_DA_MUSICA", entidade.category)
    }
}
