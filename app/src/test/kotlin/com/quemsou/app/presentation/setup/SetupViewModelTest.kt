package com.quemsou.app.presentation.setup

import com.quemsou.app.domain.model.ModoDeJogo
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
    fun `motivo de bloqueio do modo times aparece sem precisar tocar ou tentar comecar`() {
        // Só NOMES_VAZIOS é escondido antes da interação: trocar para o modo
        // Times já é, em si, uma interação real do usuário.
        val viewModel = viewModelComNomes()
        viewModel.selecionarModo(ModoDeJogo.TIMES)

        assertEquals(MotivoDoBloqueio.TIMES_INCOMPLETOS, viewModel.uiState.value.motivoDoBloqueioVisivel)
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
    fun `modo times bloqueia sem time em todos e com um so time`() {
        val viewModel = viewModelComNomes()
        viewModel.selecionarModo(ModoDeJogo.TIMES)

        assertEquals(MotivoDoBloqueio.TIMES_INCOMPLETOS, viewModel.uiState.value.motivoDoBloqueio)

        viewModel.atribuirTime(0, "time-a")
        viewModel.atribuirTime(1, "time-a")
        assertEquals(MotivoDoBloqueio.TIMES_INSUFICIENTES, viewModel.uiState.value.motivoDoBloqueio)

        viewModel.atribuirTime(1, "time-b")
        assertTrue(viewModel.uiState.value.podeComecar)
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
