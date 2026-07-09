package com.quemsou.app.domain.usecase

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.Jogador
import com.quemsou.app.domain.model.RegrasPartida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CriarPartidaTest {

    private val jogadores = listOf(
        Jogador(id = "j1", nome = "Ana"),
        Jogador(id = "j2", nome = "Bia"),
    )

    private val cards = List(20) { indice ->
        Card(
            id = "card-${indice + 1}",
            type = CardType.PESSOA,
            category = CardCategory.LIVRE,
            answer = "Resposta ${indice + 1}",
            clues = List(Card.QUANTIDADE_DE_DICAS) { "Dica ${it + 1}" },
        )
    }

    private fun criar(codigo: String) = CriarPartida.executar(
        codigo = codigo,
        jogadores = jogadores,
        regras = RegrasPartida(),
        cardsDisponiveis = cards,
    )

    @Test
    fun `mesmo codigo gera a mesma partida`() {
        val primeira = criar("LOBO")
        val segunda = criar("lobo") // seed normaliza trim + uppercase

        assertEquals(primeira.seed, segunda.seed)
        assertEquals(primeira.baralho, segunda.baralho)
    }

    @Test
    fun `codigos diferentes geram baralhos diferentes`() {
        assertNotEquals(criar("LOBO").baralho, criar("GATO").baralho)
    }

    @Test
    fun `baralho e permutacao dos cards disponiveis`() {
        val baralho = criar("LOBO").baralho

        assertEquals(cards.map { it.id }.sorted(), baralho.map { it.id }.sorted())
    }

    @Test
    fun `sem grupos informados cada jogador nasce em grupo proprio de 1`() {
        val partida = criar("LOBO")

        assertEquals(listOf("j1", "j2"), partida.grupos.map { it.id })
        assertEquals(listOf("Ana", "Bia"), partida.grupos.map { it.nome })
    }
}
