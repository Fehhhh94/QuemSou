package com.quemsou.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GrupoTest {

    private val ana = Jogador(id = "j1", nome = "Ana")
    private val bruno = Jogador(id = "j2", nome = "Bruno")

    @Test
    fun `grupo de 1 jogador usa o nome do jogador como exibicao`() {
        val grupo = Grupo.criar(id = "g1", membros = listOf(ana))

        assertEquals("Ana", grupo.nome)
        assertEquals(listOf("j1"), grupo.jogadores)
        assertEquals(0, grupo.pontos)
    }

    @Test
    fun `grupo de 2 ou mais concatena os nomes dos membros`() {
        assertEquals("Ana & Bruno", Grupo.criar(id = "g1", membros = listOf(ana, bruno)).nome)
    }

    @Test
    fun `individuais criam um grupo proprio de 1 por jogador`() {
        val grupos = Grupo.individuais(listOf(ana, bruno))

        assertEquals(listOf("j1", "j2"), grupos.map { it.id })
        assertEquals(listOf("Ana", "Bruno"), grupos.map { it.nome })
        assertTrue(grupos.all { it.jogadores.size == 1 && it.pontos == 0 })
    }

    @Test
    fun `adicionar pontos acumula e rejeita negativos`() {
        val grupo = Grupo.criar(id = "g1", membros = listOf(ana))
            .adicionarPontos(10)
            .adicionarPontos(6)

        assertEquals(16, grupo.pontos)
        assertThrows(IllegalArgumentException::class.java) { grupo.adicionarPontos(-1) }
    }

    @Test
    fun `grupo invalido e rejeitado`() {
        assertThrows(IllegalArgumentException::class.java) {
            Grupo(id = "g1", nome = "Ana", jogadores = emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            Grupo(id = "g1", nome = "Ana", jogadores = listOf("j1", "j1"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Grupo(id = "g1", nome = "Ana", jogadores = listOf("j1"), pontos = -1)
        }
    }
}
