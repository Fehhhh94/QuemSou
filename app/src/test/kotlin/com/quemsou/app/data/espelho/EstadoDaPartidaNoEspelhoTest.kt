package com.quemsou.app.data.espelho

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class EstadoDaPartidaNoEspelhoTest {

    @Test
    fun `somente o leitor recebe resposta e dica durante a rodada`() {
        val estado = EstadoDaPartidaNoEspelho(
            fase = FaseDoEspelho.DICA_REVELADA,
            leitorId = "j2",
            leitorNome = "Bia",
            resposta = "Harry Potter",
            dica = "Estudou em Hogwarts",
            valor = 8,
        )

        val leitor = estado.paraJogador("j2")
        val adivinhador = estado.paraJogador("j1")

        assertEquals("Harry Potter", leitor.resposta)
        assertEquals("Estudou em Hogwarts", leitor.dica)
        assertEquals(true, leitor.suaVez)
        assertNull(adivinhador.resposta)
        assertNull(adivinhador.dica)
        assertFalse(adivinhador.suaVez)
        val json = Json.encodeToString(adivinhador)
        assertFalse(json.contains("Harry Potter"))
        assertFalse(json.contains("Estudou em Hogwarts"))
    }

    @Test
    fun `resposta se torna publica apenas no anuncio`() {
        val anuncio = EstadoDaPartidaNoEspelho(
            fase = FaseDoEspelho.ANUNCIO,
            leitorId = "j2",
            resposta = "Harry Potter",
        )

        assertEquals("Harry Potter", anuncio.paraJogador("j1").resposta)
        assertEquals("anuncio", anuncio.paraJogador("j1").fase)
    }

    @Test
    fun `placar preserva ordem e pontos`() {
        val estado = EstadoDaPartidaNoEspelho(
            fase = FaseDoEspelho.PLACAR_FINAL,
            ranking = listOf(
                LinhaDoPlacarNoEspelho("Ana", 12),
                LinhaDoPlacarNoEspelho("Bia", 8),
            ),
        )

        val entregue = estado.paraJogador("j1")

        assertEquals(listOf("Ana", "Bia"), entregue.ranking.map { it.nome })
        assertEquals(listOf(12, 8), entregue.ranking.map { it.pontos })
    }
}
