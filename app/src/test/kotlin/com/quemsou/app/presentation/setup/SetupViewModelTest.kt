package com.quemsou.app.presentation.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupViewModelTest {

    private fun viewModelComNomes(quantidade: Int = 2): SetupViewModel {
        val viewModel = SetupViewModel()
        repeat(quantidade - 2) { viewModel.adicionarJogador() }
        repeat(quantidade) { indice -> viewModel.renomearJogador(indice, "Jogador ${indice + 1}") }
        return viewModel
    }

    @Test
    fun `estado inicial tem 2 jogadores sem nome e nao pode comecar`() {
        val estado = SetupViewModel().uiState.value

        assertEquals(2, estado.jogadores.size)
        assertFalse(estado.jogarEmTimes)
        assertFalse(estado.podeComecar)
        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, estado.motivoDoBloqueio)
    }

    @Test
    fun `estado inicial nao mostra o motivo do bloqueio antes de qualquer interacao`() {
        // Bug 1: os 2 campos de nome em branco do estado inicial não podem
        // disparar a mensagem de erro antes do usuário tocar em algo.
        val estado = SetupViewModel().uiState.value

        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, estado.motivoDoBloqueio)
        assertNull(estado.motivoDoBloqueioVisivel)
    }

    @Test
    fun `tocar um campo de nome vazio revela o motivo do bloqueio`() {
        val viewModel = SetupViewModel()

        viewModel.marcarJogadorTocado(0)

        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, viewModel.uiState.value.motivoDoBloqueioVisivel)
    }

    @Test
    fun `tocar um campo ja preenchido nao revela bloqueio de outro campo vazio`() {
        val viewModel = SetupViewModel()
        viewModel.renomearJogador(0, "Ana")

        viewModel.marcarJogadorTocado(0)

        assertNull(viewModel.uiState.value.motivoDoBloqueioVisivel)
    }

    @Test
    fun `tentar comecar com campos invalidos revela o motivo do bloqueio`() {
        val viewModel = SetupViewModel()

        viewModel.confirmar()

        assertTrue(viewModel.uiState.value.tentouComecar)
        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, viewModel.uiState.value.motivoDoBloqueioVisivel)
    }

    @Test
    fun `remover jogador desloca os indices tocados junto com a lista`() {
        val viewModel = viewModelComNomes(3)
        viewModel.marcarJogadorTocado(1) // toca o "Jogador 2"
        viewModel.removerJogador(0) // remove o "Jogador 1": o tocado agora é o indice 0

        viewModel.renomearJogador(0, "   ") // esvazia o jogador tocado (era o indice 1)

        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, viewModel.uiState.value.motivoDoBloqueioVisivel)
    }

    @Test
    fun `nomear todos os jogadores libera o comecar`() {
        val viewModel = viewModelComNomes()

        assertTrue(viewModel.uiState.value.podeComecar)
        assertNull(viewModel.uiState.value.motivoDoBloqueio)
    }

    @Test
    fun `nome vazio em qualquer jogador bloqueia`() {
        val viewModel = viewModelComNomes()

        viewModel.renomearJogador(1, "   ")

        assertFalse(viewModel.uiState.value.podeComecar)
        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, viewModel.uiState.value.motivoDoBloqueio)
    }

    @Test
    fun `adicionar alem de 4 e remover abaixo de 2 sao ignorados`() {
        val viewModel = SetupViewModel()

        repeat(5) { viewModel.adicionarJogador() }
        assertEquals(4, viewModel.uiState.value.jogadores.size)

        repeat(5) { viewModel.removerJogador(0) }
        assertEquals(2, viewModel.uiState.value.jogadores.size)
    }

    @Test
    fun `ciclar grupo percorre sem grupo, grupos 1 a 3 e volta`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarJogarEmTimes()

        assertNull(viewModel.uiState.value.jogadores[0].grupo)

        viewModel.ciclarGrupo(0)
        assertEquals(1, viewModel.uiState.value.jogadores[0].grupo)

        viewModel.ciclarGrupo(0)
        assertEquals(2, viewModel.uiState.value.jogadores[0].grupo)

        viewModel.ciclarGrupo(0)
        assertEquals(3, viewModel.uiState.value.jogadores[0].grupo)

        viewModel.ciclarGrupo(0)
        assertNull(viewModel.uiState.value.jogadores[0].grupo)
    }

    @Test
    fun `jogar em times ligado nao bloqueia nenhum agrupamento`() {
        // Especificação v4: nenhum agrupamento é inválido — grupos de 1 e 2+
        // podem conviver, então o toggle não cria motivo de bloqueio.
        val viewModel = viewModelComNomes(3)
        viewModel.alternarJogarEmTimes()

        assertTrue(viewModel.uiState.value.podeComecar)

        viewModel.ciclarGrupo(0) // só o primeiro jogador tem grupo
        assertTrue(viewModel.uiState.value.podeComecar)
    }

    @Test
    fun `confirmar sem jogar em times monta todos os jogadores sem grupo`() {
        val viewModel = viewModelComNomes(3)

        viewModel.confirmar()

        val configuracao = viewModel.configuracaoPronta.value!!
        assertTrue(configuracao.jogadores.all { it.grupoId == null })
    }

    @Test
    fun `confirmar com jogar em times leva o agrupamento escolhido`() {
        val viewModel = viewModelComNomes(3)
        viewModel.alternarJogarEmTimes()
        viewModel.ciclarGrupo(0) // Grupo 1
        viewModel.ciclarGrupo(2) // Grupo 1 — mesmo grupo do jogador 1
        // Jogador 2 fica "Sem grupo": grupo próprio de 1 na partida.

        viewModel.confirmar()

        val configuracao = viewModel.configuracaoPronta.value!!
        assertEquals(listOf("g1", null, "g1"), configuracao.jogadores.map { it.grupoId })
    }

    @Test
    fun `desligar jogar em times descarta o agrupamento no confirmar`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarJogarEmTimes()
        viewModel.ciclarGrupo(0)
        viewModel.alternarJogarEmTimes() // desliga de volta

        viewModel.confirmar()

        val configuracao = viewModel.configuracaoPronta.value!!
        assertTrue(configuracao.jogadores.all { it.grupoId == null })
    }

    @Test
    fun `confirmar monta a configuracao e ignora quando bloqueado`() {
        val bloqueado = SetupViewModel()
        bloqueado.confirmar()
        assertNull(bloqueado.configuracaoPronta.value)

        val viewModel = viewModelComNomes(3)
        viewModel.definirRodadas(4)
        viewModel.confirmar()

        val configuracao = viewModel.configuracaoPronta.value!!
        assertEquals(4, configuracao.codigo.length)
        assertEquals(4, configuracao.numeroDeRodadas)
        assertTrue(configuracao.leitorPontua)
        assertEquals(listOf("Jogador 1", "Jogador 2", "Jogador 3"), configuracao.jogadores.map { it.nome })

        viewModel.consumirConfiguracaoPronta()
        assertNull(viewModel.configuracaoPronta.value)
    }
}
