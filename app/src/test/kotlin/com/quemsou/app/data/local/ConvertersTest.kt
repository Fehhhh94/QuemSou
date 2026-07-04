package com.quemsou.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `ida e volta preserva a lista de dicas`() {
        val dicas = listOf("Dica com \"aspas\"", "Dica com acentuação", "Dica, com, vírgulas")

        assertEquals(dicas, converters.paraListaDeStrings(converters.deListaDeStrings(dicas)))
    }

    @Test
    fun `lista vazia e preservada`() {
        assertEquals(emptyList<String>(), converters.paraListaDeStrings(converters.deListaDeStrings(emptyList())))
    }
}
