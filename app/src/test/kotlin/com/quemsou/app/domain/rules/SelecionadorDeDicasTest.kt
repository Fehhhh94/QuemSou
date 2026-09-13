package com.quemsou.app.domain.rules

import com.quemsou.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class SelecionadorDeDicasTest {
    private fun card(n: Int = 60) = Card("hp", CardType.PESSOA, CardCategory.PERSONAGEM_FILME,
        "Harry Potter", List(10) { "Fato $it" }, "personagem-harry-potter",
        List(n) { DicaDoBanco("fato-$it", "Fato $it") })

    @Test fun `sessenta dicas permitem seis cartas sem repeticao e depois esgotam`() {
        val usadas = mutableSetOf<String>()
        val textos = mutableSetOf<String>()
        repeat(6) { rodada ->
            val nova = SelecionadorDeDicas.preparar(card(), usadas, rodada.toLong())!!
            assertEquals(10, nova.clues.size)
            assertTrue(nova.clues.all { textos.add(it) })
            novasChaves(nova).forEach(usadas::add)
        }
        assertNull(SelecionadorDeDicas.preparar(card(), usadas, 99))
        assertEquals(60, textos.size)
    }

    @Test fun `alterar id ou pontuacao nao recicla dica usada em outro baralho`() {
        val original = card(10)
        val usadas = novasChaves(original).toSet()
        val editado = original.copy(id = "outra-edicao", bancoDeDicas = original.bancoDeDicas.map {
            DicaDoBanco("novo-${it.id}", "  ${it.texto.uppercase()}!!!")
        })
        assertNull(SelecionadorDeDicas.preparar(editado, usadas, 1))
    }

    @Test fun `corrigir texto mantendo id nao transforma dica em inedita`() {
        val original = card(10)
        val editado = original.copy(bancoDeDicas = original.bancoDeDicas.map { it.copy(texto = "Revisado ${it.texto}") })
        assertNull(SelecionadorDeDicas.preparar(editado, novasChaves(original).toSet(), 1))
    }

    @Test fun `mesmo estado e seed independem da ordem do arquivo`() {
        val original = card()
        assertEquals(SelecionadorDeDicas.preparar(original, emptySet(), 72),
            SelecionadorDeDicas.preparar(original.copy(bancoDeDicas = original.bancoDeDicas.reversed()), emptySet(), 72))
    }

    @Test fun `acervo ampliado libera so fatos novos e legado recebe protecao`() {
        val legado = card(10).copy(respostaId = "", bancoDeDicas = emptyList())
        val utilizadas = SelecionadorDeDicas.banco(legado).flatMap { SelecionadorDeDicas.chaves(legado, it) }.toSet()
        val nova = SelecionadorDeDicas.preparar(card(20), utilizadas, 1)!!
        assertTrue(nova.clues.none(legado.clues::contains))
    }

    private fun novasChaves(card: Card) = card.bancoDeDicas.flatMap { SelecionadorDeDicas.chaves(card, it) }
}
