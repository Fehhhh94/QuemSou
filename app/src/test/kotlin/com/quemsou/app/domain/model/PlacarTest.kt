package com.quemsou.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlacarTest {

    private val jogadores = listOf(
        Jogador(id = "j1", nome = "Ana", timeId = "time-a"),
        Jogador(id = "j2", nome = "Bia", timeId = "time-a"),
        Jogador(id = "j3", nome = "Caio", timeId = "time-b"),
    )

    @Test
    fun `acumula pontos por jogador`() {
        val placar = Placar.inicial(jogadores)
            .adicionar("j1", 10)
            .adicionar("j1", 6)
            .adicionar("j2", 8)

        assertEquals(16, placar.pontosDe("j1"))
        assertEquals(8, placar.pontosDe("j2"))
        assertEquals(0, placar.pontosDe("j3"))
    }

    @Test
    fun `ranking ordena do maior para o menor`() {
        val placar = Placar.inicial(jogadores)
            .adicionar("j2", 10)
            .adicionar("j3", 5)

        assertEquals(listOf("j2" to 10, "j3" to 5, "j1" to 0), placar.ranking())
    }

    @Test
    fun `empate declara todos os vencedores`() {
        val placar = Placar.inicial(jogadores)
            .adicionar("j1", 7)
            .adicionar("j3", 7)

        assertEquals(listOf("j1", "j3"), placar.vencedores())
    }

    @Test
    fun `pontos por time somam os jogadores do time`() {
        val placar = Placar.inicial(jogadores)
            .adicionar("j1", 4)
            .adicionar("j2", 3)
            .adicionar("j3", 5)

        assertEquals(mapOf("time-a" to 7, "time-b" to 5), placar.pontosPorTime(jogadores))
        assertEquals(listOf("time-a"), placar.vencedoresPorTime(jogadores))
    }

    @Test
    fun `adicionar pontos a jogador desconhecido e rejeitado`() {
        assertThrows(IllegalArgumentException::class.java) {
            Placar.inicial(jogadores).adicionar("intruso", 5)
        }
    }
}
