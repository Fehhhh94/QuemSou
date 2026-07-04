package com.quemsou.app.domain.model

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
}
