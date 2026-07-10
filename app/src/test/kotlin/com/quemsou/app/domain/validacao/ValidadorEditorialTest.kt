package com.quemsou.app.domain.validacao

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidadorEditorialTest {

    private val validador = ValidadorEditorial()

    private fun card(
        answer: String = "Curitiba",
        clues: List<String> = List(Card.QUANTIDADE_DE_DICAS) { "Dica ${it + 1}" },
    ) = Card(
        id = "sintetico-01",
        type = CardType.LUGAR,
        category = CardCategory.PERSONAGEM_FILME,
        answer = answer,
        clues = clues,
    )

    private fun violacoesDe(card: Card): List<ViolacaoEditorial> =
        (validador.validar(card) as ResultadoValidacao.Reprovado).violacoes

    @Test
    fun `card valido e aprovado`() {
        assertEquals(ResultadoValidacao.Aprovado, validador.validar(card()))
    }

    @Test
    fun `resposta vazia e reprovada sem indice de dica`() {
        val violacoes = violacoesDe(card(answer = "   "))

        assertEquals(1, violacoes.size)
        assertEquals(RegraEditorial.RESPOSTA_VAZIA, violacoes.single().regra)
        assertEquals(null, violacoes.single().indiceDaDica)
    }

    @Test
    fun `dica vazia e reprovada com o indice da dica`() {
        val dicas = List(Card.QUANTIDADE_DE_DICAS) { indice ->
            if (indice == 4) "   " else "Dica ${indice + 1}"
        }

        val violacoes = violacoesDe(card(clues = dicas))

        assertEquals(1, violacoes.size)
        assertEquals(RegraEditorial.DICA_VAZIA, violacoes.single().regra)
        assertEquals(4, violacoes.single().indiceDaDica)
    }

    @Test
    fun `dica que nomeia a resposta e reprovada com o indice, sem diferenciar maiusculas`() {
        val dicas = List(Card.QUANTIDADE_DE_DICAS) { indice ->
            if (indice == 7) "A capital paranaense é CURITIBA" else "Dica ${indice + 1}"
        }

        val violacoes = violacoesDe(card(answer = "Curitiba", clues = dicas))

        assertEquals(1, violacoes.size)
        assertEquals(RegraEditorial.DICA_NOMEIA_RESPOSTA, violacoes.single().regra)
        assertEquals(7, violacoes.single().indiceDaDica)
    }

    @Test
    fun `todas as violacoes sao acumuladas, nao so a primeira`() {
        // Dica 1 vazia + dica 3 nomeando a resposta: a tela de revisão da
        // Fase 5 precisa ver o card inteiro de uma vez.
        val dicas = List(Card.QUANTIDADE_DE_DICAS) { indice ->
            when (indice) {
                0 -> " "
                2 -> "Fica em curitiba"
                else -> "Dica ${indice + 1}"
            }
        }

        val violacoes = violacoesDe(card(answer = "Curitiba", clues = dicas))

        assertEquals(
            listOf(
                RegraEditorial.DICA_VAZIA to 0,
                RegraEditorial.DICA_NOMEIA_RESPOSTA to 2,
            ),
            violacoes.map { it.regra to it.indiceDaDica },
        )
    }
}
