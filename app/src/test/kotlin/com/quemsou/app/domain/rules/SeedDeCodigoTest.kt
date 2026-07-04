package com.quemsou.app.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SeedDeCodigoTest {

    @Test
    fun `mesma string gera sempre a mesma seed`() {
        assertEquals(SeedDeCodigo.gerar("LOBO"), SeedDeCodigo.gerar("LOBO"))
    }

    @Test
    fun `normalizacao faz lobo minusculo e LOBO com espacos gerarem a mesma seed`() {
        assertEquals(SeedDeCodigo.gerar("lobo"), SeedDeCodigo.gerar(" LOBO "))
    }

    @Test
    fun `strings diferentes geram seeds diferentes`() {
        assertNotEquals(SeedDeCodigo.gerar("LOBO"), SeedDeCodigo.gerar("GATO"))
        assertNotEquals(SeedDeCodigo.gerar("LOBO"), SeedDeCodigo.gerar("LOBOS"))
        assertNotEquals(SeedDeCodigo.gerar("ABCD"), SeedDeCodigo.gerar("DCBA"))
    }
}
