package com.quemsou.app.data.importer

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Parse do envelope de `assets/cards.json` (versão + baralhos embarcados).
 * A validação do conteúdo de cada baralho é do `ParserDoCatalogo` e está
 * coberta em `ParserDoCatalogoTest`.
 */
class CardsJsonTest {

    @Test
    fun `json valido e parseado com versao e baralhos`() {
        val conteudo = """
            {
              "version": 3,
              "baralhos": [
                {
                  "id": "cinema-1",
                  "nome": "Cinema — Edição 1",
                  "categoria": "PERSONAGEM_FILME",
                  "colecao": { "id": "cinema", "nome": "Cinema", "icone": "🎬" },
                  "versao": 1,
                  "estado": "FINALIZADO",
                  "cards": [
                    {
                      "id": "teste-01",
                      "type": "PESSOA",
                      "answer": "TESTE_01",
                      "clues": ["d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8", "d9", "d10"]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val cardsJson = CardsJson.deJson(conteudo)

        assertEquals(3, cardsJson.version)
        val baralho = cardsJson.baralhos.single()
        assertEquals("cinema-1", baralho.id)
        assertEquals("PERSONAGEM_FILME", baralho.categoria)
        assertEquals("teste-01", baralho.cards.single().id)
    }

    @Test
    fun `json sem campo obrigatorio lanca excecao - falha ruidosa do asset embarcado`() {
        val semBaralhos = """{ "version": 3 }"""

        assertThrows(SerializationException::class.java) { CardsJson.deJson(semBaralhos) }
    }
}
