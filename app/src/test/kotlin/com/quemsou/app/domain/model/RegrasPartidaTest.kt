package com.quemsou.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RegrasPartidaTest {

    @Test
    fun `descartar card queimado e verdadeiro por padrao`() {
        val regras = RegrasPartida()

        assertTrue(regras.descartarCardQueimado)
    }

    @Test
    fun `descartar card queimado pode ser desativado`() {
        val regras = RegrasPartida(descartarCardQueimado = false)

        assertFalse(regras.descartarCardQueimado)
    }

    @Test
    fun `numero de rodadas zero lanca excecao`() {
        assertThrows(IllegalArgumentException::class.java) {
            RegrasPartida(numeroDeRodadas = 0)
        }
    }

    @Test
    fun `modo shot desligado por padrao com 2 shots`() {
        val regras = RegrasPartida()

        assertFalse(regras.modoShot)
        assertEquals(RegrasPartida.QUANTIDADE_PADRAO_DE_SHOTS, regras.quantidadeDeShots)
    }

    @Test
    fun `quantidade de shots fora da faixa 1 a 3 lanca excecao`() {
        assertThrows(IllegalArgumentException::class.java) { RegrasPartida(quantidadeDeShots = 0) }
        assertThrows(IllegalArgumentException::class.java) { RegrasPartida(quantidadeDeShots = 4) }
        assertEquals(1, RegrasPartida(quantidadeDeShots = 1).quantidadeDeShots)
        assertEquals(3, RegrasPartida(quantidadeDeShots = 3).quantidadeDeShots)
    }
}
