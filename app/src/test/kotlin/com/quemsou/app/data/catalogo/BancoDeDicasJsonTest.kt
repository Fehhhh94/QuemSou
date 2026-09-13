package com.quemsou.app.data.catalogo

import org.junit.Assert.*
import org.junit.Test

class BancoDeDicasJsonTest {
    private fun modelo() = BaralhoJson("b", "Cinema", "PERSONAGEM_FILME", ColecaoJson("c", "Cinema", "C"),
        1, "EM_DESENVOLVIMENTO", listOf(CardDoBaralhoJson("hp", "PESSOA", "Harry Potter",
            List(10) { "Fato $it" }, "personagem-harry-potter", List(60) { DicaJson("d$it", "Fato $it") })))

    @Test fun `banco de sessenta preserva dez dicas na carta e todos os ids no acervo`() {
        val resultado = ParserDoCatalogo().validarBaralho(modelo()) as ResultadoDoParse.Sucesso
        val card = resultado.valor.cards.single()
        assertEquals(10, card.clues.size)
        assertEquals(60, card.bancoDeDicas.size)
        assertEquals(modelo(), resultado.valor.paraJsonModelo())
    }

    @Test fun `banco rejeita ids repetidos textos equivalentes e dicas que nomeiam resposta`() {
        val valido = modelo()
        val original = valido.cards.single()
        val bancosInvalidos = listOf(
            original.bancoDeDicas.toMutableList().also { it[59] = DicaJson("d0", "Outro fato") },
            original.bancoDeDicas.toMutableList().also { it[59] = DicaJson("d59", "FATO 0!!!") },
            original.bancoDeDicas.toMutableList().also { it[59] = DicaJson("d59", "Sou Harry Potter") },
        )
        bancosInvalidos.forEach { banco ->
            assertTrue(ParserDoCatalogo().validarBaralho(valido.copy(cards = listOf(original.copy(bancoDeDicas = banco)))) is ResultadoDoParse.Falha)
        }
        assertTrue(ParserDoCatalogo().validarBaralho(valido.copy(cards = listOf(original.copy(respostaId = "")))) is ResultadoDoParse.Falha)
    }
}
