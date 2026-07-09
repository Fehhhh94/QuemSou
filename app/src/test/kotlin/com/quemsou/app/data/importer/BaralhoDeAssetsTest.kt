package com.quemsou.app.data.importer

import com.quemsou.app.domain.validacao.ResultadoValidacao
import com.quemsou.app.domain.validacao.ValidadorEditorial
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Valida o baralho real de `app/src/main/assets/cards.json` — garante que o
 * conteúdo editorial nunca quebra o app em runtime. As regras por card são
 * as do [ValidadorEditorial] (a mesma régua que a Fase 5 usará para cards
 * gerados por IA); a conversão do importador ([paraDominio]) cobre as regras
 * estruturais (10 dicas etc.) e este teste mantém as regras de baralho
 * inteiro (ids únicos, não vazio). O working directory dos testes JVM é o
 * diretório do módulo `app`, por isso o caminho relativo.
 */
class BaralhoDeAssetsTest {

    private val baralho: CardsJson by lazy {
        CardsJson.deJson(File("src/main/assets/cards.json").readText())
    }

    private val validador = ValidadorEditorial()

    @Test
    fun `todos os cards do baralho real passam na validacao de importacao`() {
        baralho.cards.forEach { it.paraDominio() }
    }

    @Test
    fun `todos os cards do baralho real passam na regua editorial`() {
        val reprovados = baralho.cards.mapNotNull { cardJson ->
            val resultado = validador.validar(cardJson.paraDominio())
            (resultado as? ResultadoValidacao.Reprovado)
                ?.let { reprovado -> cardJson.id to reprovado.violacoes.map { it.mensagem } }
        }

        assertTrue(
            "Cards reprovados na régua editorial: " +
                reprovados.joinToString("; ") { (id, mensagens) -> "$id → $mensagens" },
            reprovados.isEmpty(),
        )
    }

    @Test
    fun `ids do baralho real sao unicos`() {
        val ids = baralho.cards.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `baralho real nao esta vazio`() {
        assertTrue(baralho.cards.isNotEmpty())
    }
}
