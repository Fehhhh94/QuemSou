package com.quemsou.app.presentation.game

import androidx.lifecycle.SavedStateHandle
import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.EstadoDoBaralho
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

    private class RepositorioFake(private val baralhos: List<Baralho>) : RepositorioDeCards {
        override suspend fun buscarPorIds(ids: List<String>) = baralhos.filter { it.id in ids }
    }

    private fun cards(quantidade: Int = 10) = List(quantidade) { indice ->
        Card(
            id = "card-${indice + 1}",
            type = CardType.PESSOA,
            category = CardCategory.PERSONAGEM_FILME,
            answer = "Resposta ${indice + 1}",
            clues = List(Card.QUANTIDADE_DE_DICAS) { "Dica ${it + 1} do card ${indice + 1}" },
        )
    }

    private fun baralhos() = listOf(
        Baralho(
            id = "b1",
            nome = "Baralho de Teste",
            categoria = CardCategory.PERSONAGEM_FILME,
            versao = 1,
            estado = EstadoDoBaralho.FINALIZADO,
            cards = cards(),
        ),
    )

    private fun configuracao(
        nomes: List<String> = listOf("Ana", "Bia", "Caio"),
        rodadas: Int = 2,
        leitorPontua: Boolean = true,
        grupos: List<String?> = nomes.map { null },
        modoShot: Boolean = false,
        quantidadeDeShots: Int = 2,
    ) = ConfiguracaoDaPartida(
        codigo = "LOBO",
        baralhos = listOf("b1"),
        numeroDeRodadas = rodadas,
        leitorPontua = leitorPontua,
        modoShot = modoShot,
        quantidadeDeShots = quantidadeDeShots,
        jogadores = nomes.mapIndexed { indice, nome ->
            JogadorConfigurado(nome = nome, grupoId = grupos[indice])
        },
    )

    private fun handleDe(configuracao: ConfiguracaoDaPartida) =
        SavedStateHandle(mapOf("configuracao" to configuracao.paraJson()))

    private fun viewModel(
        configuracao: ConfiguracaoDaPartida = configuracao(),
        handle: SavedStateHandle = handleDe(configuracao),
    ) = PartidaViewModel(handle, RepositorioFake(baralhos()))

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
        assertEquals(1, grid.rodada)
        assertEquals("Ana", grid.nomeDoLeitor)
        assertEquals("Bia", grid.nomeDoEscolhedor)
        assertEquals(10, grid.pontosEmJogo)
        assertEquals(CardType.PESSOA, grid.tipo)
        assertTrue(grid.respostaParaOLeitor.isNotBlank())

        viewModel.revelarDica(3)
        var dica = viewModel.uiState.value as PartidaUiState.DicaRevelada
        assertEquals(3, dica.posicao)
        assertEquals(10, dica.valor)
        assertEquals(CardType.PESSOA, dica.tipo)

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
        // Caio é o escolhedor da vez agora, então entra primeiro na lista.
        assertEquals(listOf("Caio", "Bia"), quemAcertou.adivinhadores.map { it.nome })
        assertEquals(9, quemAcertou.pontosEmJogo)
        assertEquals(1, quemAcertou.pontosDoLeitor)

        viewModel.registrarAcerto("j2")
        val anuncio = viewModel.uiState.value as PartidaUiState.Anuncio.Acerto
        assertEquals("Bia", anuncio.nomeDoAcertador)
        assertEquals(2, anuncio.dicasUsadas)
        assertEquals(9, anuncio.pontosDoAcertador)
        assertEquals(1, anuncio.pontosDoLeitor)

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
            listOf(LinhaDoPlacar("Ana", 11), LinhaDoPlacar("Bia", 9), LinhaDoPlacar("Caio", 0)),
            placarFinal.ranking,
        )
        assertEquals(listOf("Ana"), placarFinal.vencedores)
        assertFalse(placarFinal.empate)
    }

    @Test
    fun `dez dicas sem acerto queimam o card com a resposta e 10 pontos para o leitor no anuncio`() {
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
        assertEquals("Ana", anuncio.nomeDoLeitor)
        assertEquals(10, anuncio.pontosDoLeitor)
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
        val antes = PartidaViewModel(handle, RepositorioFake(baralhos()))

        // Rodada 1 completa (Bia acerta com 2 dicas: acertador +9, leitor +1) e metade da rodada 2.
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
        val depois = PartidaViewModel(handle, RepositorioFake(baralhos()))

        assertEquals(estadoAntes, depois.uiState.value)

        // A partida restaurada segue até o fim com o placar da rodada 1 preservado.
        depois.revelarDica(1)
        depois.abrirQuemAcertou()
        depois.registrarAcerto("j1")
        depois.proximoTurno()
        val placarFinal = depois.uiState.value as PartidaUiState.PlacarFinal
        assertEquals(
            listOf(LinhaDoPlacar("Ana", 10), LinhaDoPlacar("Bia", 10), LinhaDoPlacar("Caio", 0)),
            placarFinal.ranking,
        )
    }

    @Test
    fun `placar final agrega os pontos por grupo com grupos mistos`() {
        // "Ana & Bia" num grupo; Caio solo — grupos mistos na mesma partida.
        val handle = handleDe(configuracao(grupos = listOf("g1", "g1", null)))
        val viewModel = PartidaViewModel(handle, RepositorioFake(baralhos()))

        // Rodada 1 (leitora Ana): Bia acerta na 1ª dica → grupo "Ana & Bia" +10.
        viewModel.iniciarTurno()
        viewModel.revelarDica(1)
        viewModel.abrirQuemAcertou()
        viewModel.registrarAcerto("j2")
        viewModel.proximoTurno()

        // Morte de processo entre as rodadas: os pontos por grupo sobrevivem.
        val restaurado = PartidaViewModel(handle, RepositorioFake(baralhos()))

        // Rodada 2 (leitora Bia): Caio acerta na 2ª dica → Caio +9 e a
        // leitora Bia +1 — o ponto da leitora vai para o grupo "Ana & Bia".
        restaurado.iniciarTurno()
        restaurado.revelarDica(3)
        restaurado.outraDica()
        restaurado.revelarDica(5)
        restaurado.abrirQuemAcertou()
        restaurado.registrarAcerto("j3")
        restaurado.proximoTurno()

        val placarFinal = restaurado.uiState.value as PartidaUiState.PlacarFinal
        assertEquals(
            listOf(LinhaDoPlacar("Ana & Bia", 11), LinhaDoPlacar("Caio", 9)),
            placarFinal.ranking,
        )
        assertEquals(listOf("Ana & Bia"), placarFinal.vencedores)
        assertFalse(placarFinal.empate)
    }

    /**
     * Toca o grid às cegas até cair numa posição com shot (determinístico pela
     * seed do código "LOBO"), retornando o overlay aberto. Falha se nenhuma
     * das 10 posições tiver shot.
     */
    private fun PartidaViewModel.tocarAteAbrirUmShot(): PartidaUiState.Shot {
        for (posicao in 1..10) {
            if (uiState.value !is PartidaUiState.Grid) break
            revelarDica(posicao)
            when (val estado = uiState.value) {
                is PartidaUiState.Shot -> return estado
                is PartidaUiState.DicaRevelada -> outraDica()
                else -> error("Fase inesperada ao tocar o grid: $estado")
            }
        }
        error("Nenhuma posição com shot encontrada no grid.")
    }

    @Test
    fun `posicao com shot abre o overlay e o bebi revela a dica normalmente`() {
        val viewModel = viewModel(configuracao(modoShot = true, quantidadeDeShots = 3))
        viewModel.iniciarTurno()

        val overlay = viewModel.tocarAteAbrirUmShot()

        // Quem bebe é sempre quem escolheu o número — o escolhedor da vez.
        assertEquals(overlay.grid.nomeDoEscolhedor, overlay.nomeDoBebedor)

        // Durante o overlay, eventos de outras fases são ignorados.
        viewModel.revelarDica((overlay.posicao % 10) + 1)
        viewModel.outraDica()
        assertEquals(overlay, viewModel.uiState.value)

        // "Bebi!": a dica da posição pendente é revelada, valendo os mesmos
        // pontos que o grid anunciava antes do toque — pontuação intocada.
        viewModel.confirmarShot()
        val dica = viewModel.uiState.value as PartidaUiState.DicaRevelada
        assertEquals(overlay.posicao, dica.posicao)
        assertEquals(overlay.grid.pontosEmJogo, dica.valor)
    }

    @Test
    fun `confirmar shot fora da fase shot e ignorado`() {
        val viewModel = viewModel(configuracao(modoShot = true))
        viewModel.iniciarTurno()
        val grid = viewModel.uiState.value

        viewModel.confirmarShot()

        assertEquals(grid, viewModel.uiState.value)
    }

    @Test
    fun `modo shot desligado nunca abre o overlay`() {
        val viewModel = viewModel()
        viewModel.iniciarTurno()

        for (posicao in 1..10) {
            viewModel.revelarDica(posicao)
            assertTrue(viewModel.uiState.value is PartidaUiState.DicaRevelada)
            if (posicao < 10) viewModel.outraDica()
        }
    }

    @Test
    fun `morte de processo com o overlay do shot aberto restaura o overlay`() {
        val handle = handleDe(configuracao(modoShot = true, quantidadeDeShots = 3))
        val antes = PartidaViewModel(handle, RepositorioFake(baralhos()))
        antes.iniciarTurno()
        val overlayAntes = antes.tocarAteAbrirUmShot()

        // "Morte de processo": novo ViewModel com o mesmo SavedStateHandle
        // volta EXATAMENTE ao overlay — posição pendente e bebedor incluídos.
        val depois = PartidaViewModel(handle, RepositorioFake(baralhos()))
        assertEquals(overlayAntes, depois.uiState.value)

        // E o fluxo segue dali: o "Bebi!" revela a mesma posição pendente.
        depois.confirmarShot()
        val dica = depois.uiState.value as PartidaUiState.DicaRevelada
        assertEquals(overlayAntes.posicao, dica.posicao)
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

    @Test
    fun `reiniciar partida no placar final volta para a rodada 1 com placar zerado`() {
        val viewModel = viewModel(configuracao(nomes = listOf("Ana", "Bia"), rodadas = 1))
        viewModel.iniciarTurno()
        viewModel.revelarDica(1)
        viewModel.abrirQuemAcertou() // 1 adivinhador: pula direto para o anúncio
        val placarFinalAntes = run {
            viewModel.proximoTurno()
            viewModel.uiState.value as PartidaUiState.PlacarFinal
        }
        assertEquals(10, placarFinalAntes.ranking.first { it.nome == "Bia" }.pontos)

        viewModel.reiniciarPartida()

        val vez = viewModel.uiState.value as PartidaUiState.VezDeJogar
        assertEquals(1, vez.rodada)
        assertEquals("Ana", vez.nomeDoLeitor)

        viewModel.iniciarTurno()
        viewModel.revelarDica(1)
        viewModel.abrirQuemAcertou()
        viewModel.proximoTurno()
        val placarFinalDepois = viewModel.uiState.value as PartidaUiState.PlacarFinal
        assertEquals(10, placarFinalDepois.ranking.first { it.nome == "Bia" }.pontos)
    }

    @Test
    fun `reiniciar partida fora do placar final e ignorado`() {
        val viewModel = viewModel()
        val vez = viewModel.uiState.value

        viewModel.reiniciarPartida()

        assertEquals(vez, viewModel.uiState.value)
    }
}
