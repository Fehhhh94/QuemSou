package com.quemsou.app.presentation.game

import androidx.lifecycle.SavedStateHandle
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.ModoDeJogo
import com.quemsou.app.domain.repository.RepositorioDeCards
import com.quemsou.app.navigation.ConfiguracaoDaPartida
import com.quemsou.app.navigation.JogadorConfigurado
import com.quemsou.app.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PartidaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class RepositorioFake(private val cards: List<Card>) : RepositorioDeCards {
        override suspend fun buscarPorCategoria(categoria: CardCategory) = cards
    }

    private fun cards(quantidade: Int = 10) = List(quantidade) { indice ->
        Card(
            id = "card-${indice + 1}",
            type = CardType.PESSOA,
            category = CardCategory.LIVRE,
            answer = "Resposta ${indice + 1}",
            clues = List(Card.QUANTIDADE_DE_DICAS) { "Dica ${it + 1} do card ${indice + 1}" },
        )
    }

    private fun configuracao(
        nomes: List<String> = listOf("Ana", "Bia", "Caio"),
        rodadas: Int = 2,
        leitorPontua: Boolean = true,
    ) = ConfiguracaoDaPartida(
        codigo = "LOBO",
        categoria = CardCategory.LIVRE,
        modoDeJogo = ModoDeJogo.INDIVIDUAL,
        numeroDeRodadas = rodadas,
        leitorPontua = leitorPontua,
        jogadores = nomes.map { JogadorConfigurado(nome = it) },
    )

    private fun handleDe(configuracao: ConfiguracaoDaPartida) =
        SavedStateHandle(mapOf("configuracao" to configuracao.paraJson()))

    private fun viewModel(
        configuracao: ConfiguracaoDaPartida = configuracao(),
        handle: SavedStateHandle = handleDe(configuracao),
    ) = PartidaViewModel(handle, RepositorioFake(cards()))

    @Test
    fun `partida completa por eventos ate o placar final`() {
        val viewModel = viewModel()

        // Rodada 1: leitor Ana, adivinhadores Bia e Caio.
        var vez = viewModel.uiState.value as PartidaUiState.VezDeJogar
        assertEquals(1, vez.rodada)
        assertEquals(2, vez.totalDeRodadas)
        assertEquals("Ana", vez.nomeDoLeitor)
        assertEquals(listOf("Bia", "Caio"), vez.nomesDosAdivinhadores)

        viewModel.iniciarTurno()
        var grid = viewModel.uiState.value as PartidaUiState.Grid
        assertEquals("Bia", grid.nomeDoEscolhedor)
        assertEquals(10, grid.pontosEmJogo)
        assertTrue(grid.respostaParaOLeitor.isNotBlank())

        viewModel.revelarDica(3)
        var dica = viewModel.uiState.value as PartidaUiState.DicaRevelada
        assertEquals(3, dica.posicao)
        assertEquals(10, dica.valor)

        // Ninguém arriscou: o escolhedor gira.
        viewModel.outraDica()
        grid = viewModel.uiState.value as PartidaUiState.Grid
        assertEquals(listOf(3), grid.posicoesReveladas)
        assertEquals("Caio", grid.nomeDoEscolhedor)
        assertEquals(9, grid.pontosEmJogo)

        viewModel.revelarDica(7)
        dica = viewModel.uiState.value as PartidaUiState.DicaRevelada
        assertEquals(9, dica.valor)

        viewModel.abrirQuemAcertou()
        val quemAcertou = viewModel.uiState.value as PartidaUiState.QuemAcertou
        assertEquals(listOf("Bia", "Caio"), quemAcertou.adivinhadores.map { it.nome })

        viewModel.registrarAcerto("j2")
        val anuncio = viewModel.uiState.value as PartidaUiState.Anuncio.Acerto
        assertEquals("Bia", anuncio.nomeDoAcertador)
        assertEquals(2, anuncio.dicasUsadas)
        assertEquals(9, anuncio.pontosDoAcertador)
        assertEquals(9, anuncio.pontosDoLeitor)

        // Rodada 2: leitor girou para Bia.
        viewModel.proximoTurno()
        vez = viewModel.uiState.value as PartidaUiState.VezDeJogar
        assertEquals(2, vez.rodada)
        assertEquals("Bia", vez.nomeDoLeitor)

        viewModel.iniciarTurno()
        viewModel.revelarDica(1)
        viewModel.abrirQuemAcertou()
        viewModel.registrarAcerto("j1")
        val segundoAnuncio = viewModel.uiState.value as PartidaUiState.Anuncio.Acerto
        assertEquals("Ana", segundoAnuncio.nomeDoAcertador)
        assertEquals(10, segundoAnuncio.pontosDoAcertador)

        viewModel.proximoTurno()
        val placarFinal = viewModel.uiState.value as PartidaUiState.PlacarFinal
        assertEquals(
            listOf(LinhaDoPlacar("Ana", 19), LinhaDoPlacar("Bia", 19), LinhaDoPlacar("Caio", 0)),
            placarFinal.ranking,
        )
        assertEquals(listOf("Ana", "Bia"), placarFinal.vencedores)
        assertTrue(placarFinal.empate)
    }

    @Test
    fun `dez dicas sem acerto queimam o card com a resposta no anuncio`() {
        val viewModel = viewModel()
        viewModel.iniciarTurno()
        val resposta = (viewModel.uiState.value as PartidaUiState.Grid).respostaParaOLeitor

        for (posicao in 1..10) {
            viewModel.revelarDica(posicao)
            if (posicao < 10) viewModel.outraDica()
        }
        viewModel.outraDica() // não há 11ª dica: queima

        val anuncio = viewModel.uiState.value as PartidaUiState.Anuncio.Queimado
        assertEquals(resposta, anuncio.resposta)
        assertEquals(10, anuncio.dicasUsadas)
    }

    @Test
    fun `quem acertou e pulado quando ha um unico adivinhador`() {
        val viewModel = viewModel(configuracao(nomes = listOf("Ana", "Bia")))
        viewModel.iniciarTurno()
        viewModel.revelarDica(5)

        viewModel.abrirQuemAcertou()

        val anuncio = viewModel.uiState.value as PartidaUiState.Anuncio.Acerto
        assertEquals("Bia", anuncio.nomeDoAcertador)
        assertEquals(10, anuncio.pontosDoAcertador)
    }

    @Test
    fun `leitor nao pontua no anuncio quando a regra esta desligada`() {
        val viewModel = viewModel(configuracao(leitorPontua = false))
        viewModel.iniciarTurno()
        viewModel.revelarDica(1)
        viewModel.abrirQuemAcertou()
        viewModel.registrarAcerto("j2")

        val anuncio = viewModel.uiState.value as PartidaUiState.Anuncio.Acerto
        assertEquals(10, anuncio.pontosDoAcertador)
        assertEquals(0, anuncio.pontosDoLeitor)
    }

    @Test
    fun `morte de processo restaura a mesma fase, posicoes e placar`() {
        val handle = handleDe(configuracao())
        val antes = PartidaViewModel(handle, RepositorioFake(cards()))

        // Rodada 1 completa (Bia acerta com 2 dicas: +9/+9) e metade da rodada 2.
        antes.iniciarTurno()
        antes.revelarDica(3)
        antes.outraDica()
        antes.revelarDica(7)
        antes.abrirQuemAcertou()
        antes.registrarAcerto("j2")
        antes.proximoTurno()
        antes.iniciarTurno()
        antes.revelarDica(4)
        antes.outraDica()
        val estadoAntes = antes.uiState.value as PartidaUiState.Grid

        // "Morte de processo": novo ViewModel com o mesmo SavedStateHandle.
        val depois = PartidaViewModel(handle, RepositorioFake(cards()))

        assertEquals(estadoAntes, depois.uiState.value)

        // A partida restaurada segue até o fim com o placar da rodada 1 preservado.
        depois.revelarDica(1)
        depois.abrirQuemAcertou()
        depois.registrarAcerto("j1")
        depois.proximoTurno()
        val placarFinal = depois.uiState.value as PartidaUiState.PlacarFinal
        assertEquals(
            listOf(LinhaDoPlacar("Ana", 18), LinhaDoPlacar("Bia", 18), LinhaDoPlacar("Caio", 0)),
            placarFinal.ranking,
        )
    }

    @Test
    fun `evento invalido para a fase atual e ignorado`() {
        val viewModel = viewModel()

        // Em VezDeJogar, eventos de outras fases não fazem nada.
        val vez = viewModel.uiState.value
        viewModel.revelarDica(1)
        viewModel.outraDica()
        viewModel.proximoTurno()
        assertEquals(vez, viewModel.uiState.value)

        // No Grid, acertar ou pedir outra dica não fazem nada.
        viewModel.iniciarTurno()
        val grid = viewModel.uiState.value
        viewModel.registrarAcerto("j2")
        viewModel.outraDica()
        viewModel.iniciarTurno()
        assertEquals(grid, viewModel.uiState.value)

        // Com dica revelada, revelar de novo não faz nada.
        viewModel.revelarDica(2)
        val dica = viewModel.uiState.value
        viewModel.revelarDica(5)
        assertEquals(dica, viewModel.uiState.value)

        // Posição repetida e fora do grid são ignoradas.
        viewModel.outraDica()
        val gridComPosicao = viewModel.uiState.value as PartidaUiState.Grid
        viewModel.revelarDica(2)
        viewModel.revelarDica(0)
        viewModel.revelarDica(11)
        assertEquals(gridComPosicao, viewModel.uiState.value)
    }

    @Test
    fun `voltar vira pedido de abandono e pode ser cancelado`() {
        val viewModel = viewModel()
        assertFalse(viewModel.abandonoSolicitado.value)

        viewModel.abandonarPartida()
        assertTrue(viewModel.abandonoSolicitado.value)

        viewModel.continuarPartida()
        assertFalse(viewModel.abandonoSolicitado.value)
    }
}
