package com.quemsou.app.data.importer

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Valida o baralho real de `app/src/main/assets/cards.json` com as mesmas
 * regras do importador — garante que o conteúdo editorial nunca quebra o app
 * em runtime. O working directory dos testes JVM é o diretório do módulo
 * `app`, por isso o caminho relativo.
 */
class BaralhoDeAssetsTest {

    private val baralho: CardsJson by lazy {
        CardsJson.deJson(File("src/main/assets/cards.json").readText())
    }

    @Test
    fun `todos os cards do baralho real passam na validacao de importacao`() {
        baralho.cards.forEach { it.paraDominio() }
    }

    @Test
    fun `ids do baralho real sao unicos`() {
        val ids = baralho.cards.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `nenhuma dica do baralho real nomeia a resposta do card`() {
        val infratores = baralho.cards.filter { card ->
            card.clues.any { dica -> dica.contains(card.answer, ignoreCase = true) }
        }

        assertTrue(
            "Cards com dica que nomeia a resposta: ${infratores.map { it.id }}",
            infratores.isEmpty(),
        )
    }

    @Test
    fun `baralho real nao esta vazio`() {
        assertTrue(baralho.cards.isNotEmpty())
    }
}
