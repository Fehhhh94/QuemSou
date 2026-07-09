package com.quemsou.app.domain.rules

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.RegrasPartida
import com.quemsou.app.domain.model.ResultadoTurno
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Especificação v3 ("cabo de guerra"): o leitor ganha 1 ponto por dica
 * revelada sem acerto — acerto na dica N dá ao leitor N − 1 pontos; o card
 * queimado (10 dicas sem acerto) dá 10 pontos ao leitor. Todo turno distribui
 * exatamente 10 pontos no total quando o leitor pontua (ver docs/GAME_RULES.md).
 */
class CalculadoraDePontosTest {

    private val regrasPadrao = RegrasPartida()
    private val regrasSemLeitor = RegrasPartida(leitorPontua = false)

    @Test
    fun `acerto na dica 1 vale 10 pontos para o acertador e 0 para o leitor`() {
        val resultado = CalculadoraDePontos.calcular(dicasUsadas = 1, regras = regrasPadrao)

        assertEquals(10, resultado.pontosAcertador)
        assertEquals(0, resultado.pontosLeitor)
    }

    @Test
    fun `acerto na dica 10 vale 1 ponto para o acertador e 9 para o leitor`() {
        val resultado = CalculadoraDePontos.calcular(dicasUsadas = 10, regras = regrasPadrao)

        assertEquals(1, resultado.pontosAcertador)
        assertEquals(9, resultado.pontosLeitor)
    }

    @Test
    fun `acerto na dica 5 vale 6 pontos para o acertador e 4 para o leitor`() {
        val resultado = CalculadoraDePontos.calcular(dicasUsadas = 5, regras = regrasPadrao)

        assertEquals(6, resultado.pontosAcertador)
        assertEquals(4, resultado.pontosLeitor)
    }

    @Test
    fun `leitor ganha uma dica a menos que o acertador quando leitorPontua e true`() {
        val resultado = CalculadoraDePontos.calcular(dicasUsadas = 3, regras = RegrasPartida(leitorPontua = true))

        assertEquals(8, resultado.pontosAcertador)
        assertEquals(2, resultado.pontosLeitor)
    }

    @Test
    fun `leitor recebe zero quando leitorPontua e false`() {
        val resultado = CalculadoraDePontos.calcular(dicasUsadas = 3, regras = regrasSemLeitor)

        assertEquals(8, resultado.pontosAcertador)
        assertEquals(0, resultado.pontosLeitor)
    }

    @Test
    fun `leitor nao pontua em nenhum cenario quando leitorPontua e false`() {
        assertEquals(0, CalculadoraDePontos.calcular(dicasUsadas = 1, regras = regrasSemLeitor).pontosLeitor)
        assertEquals(0, CalculadoraDePontos.calcular(dicasUsadas = 10, regras = regrasSemLeitor).pontosLeitor)
        assertEquals(0, CalculadoraDePontos.ninguemAcertou(regrasSemLeitor).pontosLeitor)
    }

    @Test
    fun `zero dicas usadas lanca excecao`() {
        assertThrows(IllegalArgumentException::class.java) {
            CalculadoraDePontos.calcular(dicasUsadas = 0, regras = regrasPadrao)
        }
    }

    @Test
    fun `onze dicas usadas lanca excecao`() {
        assertThrows(IllegalArgumentException::class.java) {
            CalculadoraDePontos.calcular(dicasUsadas = 11, regras = regrasPadrao)
        }
    }

    @Test
    fun `card queimado da 10 pontos ao leitor e 0 ao acertador`() {
        assertEquals(
            ResultadoTurno(pontosAcertador = 0, pontosLeitor = 10),
            CalculadoraDePontos.ninguemAcertou(regrasPadrao),
        )
    }

    @Test
    fun `card queimado nao pontua o leitor quando leitorPontua e false`() {
        assertEquals(
            ResultadoTurno(pontosAcertador = 0, pontosLeitor = 0),
            CalculadoraDePontos.ninguemAcertou(regrasSemLeitor),
        )
    }

    @Test
    fun `todo turno com acerto distribui exatamente 10 pontos no total`() {
        for (dicasUsadas in 1..Card.QUANTIDADE_DE_DICAS) {
            val resultado = CalculadoraDePontos.calcular(dicasUsadas, regrasPadrao)
            assertEquals(10, resultado.pontosAcertador + resultado.pontosLeitor)
        }
    }

    @Test
    fun `card queimado tambem distribui exatamente 10 pontos, todos para o leitor`() {
        val resultado = CalculadoraDePontos.ninguemAcertou(regrasPadrao)

        assertEquals(10, resultado.pontosAcertador + resultado.pontosLeitor)
        assertEquals(10, resultado.pontosLeitor)
    }
}
