package com.quemsou.app.domain.rules

import com.quemsou.app.domain.model.RegrasPartida
import com.quemsou.app.domain.model.ResultadoTurno
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculadoraDePontosTest {

    private val regrasPadrao = RegrasPartida()

    @Test
    fun `acerto na dica 1 vale 10 pontos`() {
        assertEquals(10, CalculadoraDePontos.calcular(dicasUsadas = 1, regras = regrasPadrao).pontosAcertador)
    }

    @Test
    fun `acerto na dica 10 vale 1 ponto`() {
        assertEquals(1, CalculadoraDePontos.calcular(dicasUsadas = 10, regras = regrasPadrao).pontosAcertador)
    }

    @Test
    fun `acerto na dica 5 vale 6 pontos`() {
        assertEquals(6, CalculadoraDePontos.calcular(dicasUsadas = 5, regras = regrasPadrao).pontosAcertador)
    }

    @Test
    fun `leitor recebe os mesmos pontos quando leitorPontua e true`() {
        val resultado = CalculadoraDePontos.calcular(
            dicasUsadas = 3,
            regras = RegrasPartida(leitorPontua = true),
        )

        assertEquals(resultado.pontosAcertador, resultado.pontosLeitor)
        assertEquals(8, resultado.pontosLeitor)
    }

    @Test
    fun `leitor recebe zero quando leitorPontua e false`() {
        val resultado = CalculadoraDePontos.calcular(
            dicasUsadas = 3,
            regras = RegrasPartida(leitorPontua = false),
        )

        assertEquals(8, resultado.pontosAcertador)
        assertEquals(0, resultado.pontosLeitor)
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
    fun `ninguem acertou pontua zero a zero`() {
        assertEquals(
            ResultadoTurno(pontosAcertador = 0, pontosLeitor = 0),
            CalculadoraDePontos.ninguemAcertou(),
        )
    }
}
