package com.quemsou.app.presentation.setup

import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.Colecao
import com.quemsou.app.domain.model.EstadoDoBaralho
import com.quemsou.app.domain.repository.RepositorioDeCards
import com.quemsou.app.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class RepositorioFake(var baralhos: List<Baralho>) : RepositorioDeCards {
        override suspend fun buscarPorIds(ids: List<String>) = baralhos.filter { it.id in ids }

        override suspend fun buscarTodos() = baralhos
    }

    private fun cards(quantidade: Int, prefixo: String) = List(quantidade) { indice ->
        Card(
            id = "$prefixo-card-${indice + 1}",
            type = CardType.PESSOA,
            category = CardCategory.PERSONAGEM_FILME,
            answer = "Resposta ${indice + 1}",
            clues = List(Card.QUANTIDADE_DE_DICAS) { "Dica ${it + 1}" },
        )
    }

    private fun baralho(id: String, quantidadeDeCards: Int = 10) = Baralho(
        id = id,
        nome = "Baralho $id",
        categoria = CardCategory.PERSONAGEM_FILME,
        colecao = Colecao(id = "colecao-teste", nome = "Coleção de Teste", icone = "🧪"),
        versao = 1,
        estado = EstadoDoBaralho.FINALIZADO,
        cards = cards(quantidadeDeCards, prefixo = id),
    )

    private val repositorio = RepositorioFake(listOf(baralho("b1"), baralho("b2", quantidadeDeCards = 5)))

    private fun viewModelComNomes(quantidade: Int = 2): SetupViewModel {
        val viewModel = SetupViewModel(repositorio)
        repeat(quantidade - 2) { viewModel.adicionarJogador() }
        repeat(quantidade) { indice -> viewModel.renomearJogador(indice, "Jogador ${indice + 1}") }
        return viewModel
    }

    // region Baralhos da partida

    @Test
    fun `todos os baralhos do aparelho nascem selecionados com o contador da uniao`() {
        val estado = viewModelComNomes().uiState.value

        assertEquals(setOf("b1", "b2"), estado.baralhosSelecionados)
        assertEquals(15, estado.cardsNoMonte)
        assertTrue(estado.podeComecar)
    }

    @Test
    fun `desmarcar todos os baralhos bloqueia com motivo visivel imediato`() {
        val viewModel = viewModelComNomes()

        viewModel.alternarBaralho("b1")
        viewModel.alternarBaralho("b2")

        val estado = viewModel.uiState.value
        assertEquals(MotivoDoBloqueio.NENHUM_BARALHO, estado.motivoDoBloqueio)
        assertEquals(MotivoDoBloqueio.NENHUM_BARALHO, estado.motivoDoBloqueioVisivel)
        assertFalse(estado.podeComecar)
    }

    @Test
    fun `selecionar todos volta a marcar tudo`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarBaralho("b1")
        viewModel.alternarBaralho("b2")

        viewModel.selecionarTodosBaralhos()

        assertEquals(setOf("b1", "b2"), viewModel.uiState.value.baralhosSelecionados)
    }

    @Test
    fun `uniao com menos cards que rodadas bloqueia`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarBaralho("b1") // sobra só b2, com 5 cards

        viewModel.definirRodadas(6)

        assertEquals(MotivoDoBloqueio.CARDS_INSUFICIENTES, viewModel.uiState.value.motivoDoBloqueioVisivel)
        assertFalse(viewModel.uiState.value.podeComecar)

        viewModel.definirRodadas(5)
        assertTrue(viewModel.uiState.value.podeComecar)
    }

    @Test
    fun `recarregar preserva a selecao do usuario e novos baralhos entram desmarcados`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarBaralho("b2") // usuário deixa só b1

        repositorio.baralhos = listOf(baralho("b1"), baralho("b2"), baralho("b3"))
        viewModel.recarregarBaralhos()

        val estado = viewModel.uiState.value
        assertEquals(listOf("b1", "b2", "b3"), estado.baralhosDisponiveis.map { it.id })
        assertEquals(setOf("b1"), estado.baralhosSelecionados)
    }

    @Test
    fun `confirmar leva os baralhos selecionados em ordem estavel`() {
        val viewModel = viewModelComNomes()

        viewModel.confirmar()

        assertEquals(listOf("b1", "b2"), viewModel.configuracaoPronta.value!!.baralhos)
    }

    // endregion

    @Test
    fun `estado inicial tem 2 jogadores sem nome e nao pode comecar`() {
        val estado = SetupViewModel(repositorio).uiState.value

        assertEquals(2, estado.jogadores.size)
        assertFalse(estado.jogarEmTimes)
        assertFalse(estado.podeComecar)
        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, estado.motivoDoBloqueio)
    }

    @Test
    fun `estado inicial nao mostra o motivo do bloqueio antes de qualquer interacao`() {
        // Bug 1: os 2 campos de nome em branco do estado inicial não podem
        // disparar a mensagem de erro antes do usuário tocar em algo.
        val estado = SetupViewModel(repositorio).uiState.value

        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, estado.motivoDoBloqueio)
        assertNull(estado.motivoDoBloqueioVisivel)
    }

    @Test
    fun `tocar um campo de nome vazio revela o motivo do bloqueio`() {
        val viewModel = SetupViewModel(repositorio)

        viewModel.marcarJogadorTocado(0)

        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, viewModel.uiState.value.motivoDoBloqueioVisivel)
    }

    @Test
    fun `tentar comecar com campos invalidos revela o motivo do bloqueio`() {
        val viewModel = SetupViewModel(repositorio)

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
    fun `adicionar alem de 4 e remover abaixo de 2 sao ignorados`() {
        val viewModel = SetupViewModel(repositorio)

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
    fun `confirmar com jogar em times leva o agrupamento escolhido`() {
        val viewModel = viewModelComNomes(3)
        viewModel.alternarJogarEmTimes()
        viewModel.ciclarGrupo(0) // Grupo 1
        viewModel.ciclarGrupo(2) // Grupo 1 — mesmo grupo do jogador 1

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
    fun `modo shot desligado por padrao e quantidade fora da faixa e ignorada`() {
        val viewModel = viewModelComNomes()
        assertFalse(viewModel.uiState.value.modoShot)
        assertEquals(2, viewModel.uiState.value.quantidadeDeShots)

        viewModel.definirQuantidadeDeShots(0)
        viewModel.definirQuantidadeDeShots(4)
        assertEquals(2, viewModel.uiState.value.quantidadeDeShots)

        viewModel.definirQuantidadeDeShots(3)
        assertEquals(3, viewModel.uiState.value.quantidadeDeShots)
    }

    @Test
    fun `confirmar leva o modo shot e a quantidade escolhida`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarModoShot()
        viewModel.definirQuantidadeDeShots(1)

        viewModel.confirmar()

        val configuracao = viewModel.configuracaoPronta.value!!
        assertTrue(configuracao.modoShot)
        assertEquals(1, configuracao.quantidadeDeShots)
    }

    @Test
    fun `confirmar monta a configuracao e ignora quando bloqueado`() {
        val bloqueado = SetupViewModel(repositorio)
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
