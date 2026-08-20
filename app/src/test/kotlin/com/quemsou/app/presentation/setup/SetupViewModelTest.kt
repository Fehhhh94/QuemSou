package com.quemsou.app.presentation.setup

import com.quemsou.app.data.espelho.JogadorDoEspelho
import com.quemsou.app.data.espelho.RegistroDeSessoes
import com.quemsou.app.data.espelho.ResultadoDoInicio
import com.quemsou.app.data.espelho.ServidorDoEspelho
import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.Colecao
import com.quemsou.app.domain.model.EstadoDoBaralho
import com.quemsou.app.domain.repository.RepositorioDeCards
import com.quemsou.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class RepositorioFake(var baralhos: List<Baralho>) : RepositorioDeCards {
        override suspend fun buscarPorIds(ids: List<String>) = baralhos.filter { it.id in ids }

        override suspend fun buscarTodos() = baralhos
    }

    /**
     * Espelho de mentira: **tudo menos o socket**. Delega ao
     * `RegistroDeSessoes` de verdade em vez de reimplementar as regras de
     * pareamento — assim o teste do ViewModel não vira uma segunda versão
     * (e uma segunda fonte de bugs) das mesmas regras. [desfecho] escolhe o
     * que o `iniciar` responde; [entrarPelaWeb] simula um navegador.
     */
    private class ServidorFake(
        var desfecho: ResultadoDoInicio = ResultadoDoInicio.NoAr("http://192.168.0.7:8080"),
    ) : ServidorDoEspelho {
        private val sessoes = RegistroDeSessoes()
        private var noAr = false

        val enderecoAtual = MutableStateFlow<String?>(null)
        var parou = false
        var inicios = 0
        var portaoDoInicio: CompletableDeferred<Unit>? = null

        /** Elenco como o servidor o conhece agora. */
        val elenco: List<JogadorDoEspelho> get() = sessoes.jogadores.value

        override val endereco = enderecoAtual
        override val conectados = sessoes.conectados
        override val esteAparelho = sessoes.esteAparelho

        /** Um navegador tocando no nome, do outro lado do QR. */
        fun entrarPelaWeb(jogadorId: String) {
            sessoes.reivindicar(jogadorId, token = null)
        }

        /** O nome ainda aparece na lista do cliente web? */
        fun oferecidoNaWeb(jogadorId: String) = sessoes.jogadoresOferecidos().any { it.id == jogadorId }

        override suspend fun iniciar(jogadores: List<JogadorDoEspelho>): ResultadoDoInicio {
            inicios++
            portaoDoInicio?.await()
            val resultado = desfecho
            if (resultado is ResultadoDoInicio.NoAr) {
                noAr = true
                enderecoAtual.value = resultado.endereco
                sessoes.definirJogadores(jogadores)
            }
            return resultado
        }

        override fun atualizarJogadores(jogadores: List<JogadorDoEspelho>) {
            if (noAr) sessoes.definirJogadores(jogadores)
        }

        override fun marcarEsteAparelho(jogadorId: String?) {
            if (noAr) sessoes.marcarEsteAparelho(jogadorId)
        }

        override fun liberarLugar(jogadorId: String) {
            if (noAr) sessoes.liberarLugar(jogadorId)
        }

        override fun parar() {
            parou = true
            noAr = false
            enderecoAtual.value = null
            sessoes.limpar()
        }
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
    private val servidor = ServidorFake()

    private fun criarViewModel() = SetupViewModel(repositorio, servidor)

    private fun viewModelComNomes(quantidade: Int = 2): SetupViewModel {
        val viewModel = criarViewModel()
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
        val estado = criarViewModel().uiState.value

        assertEquals(2, estado.jogadores.size)
        assertFalse(estado.jogarEmTimes)
        assertFalse(estado.podeComecar)
        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, estado.motivoDoBloqueio)
    }

    @Test
    fun `estado inicial nao mostra o motivo do bloqueio antes de qualquer interacao`() {
        // Bug 1: os 2 campos de nome em branco do estado inicial não podem
        // disparar a mensagem de erro antes do usuário tocar em algo.
        val estado = criarViewModel().uiState.value

        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, estado.motivoDoBloqueio)
        assertNull(estado.motivoDoBloqueioVisivel)
    }

    @Test
    fun `tocar um campo de nome vazio revela o motivo do bloqueio`() {
        val viewModel = criarViewModel()

        viewModel.marcarJogadorTocado(0)

        assertEquals(MotivoDoBloqueio.NOMES_VAZIOS, viewModel.uiState.value.motivoDoBloqueioVisivel)
    }

    @Test
    fun `tentar comecar com campos invalidos revela o motivo do bloqueio`() {
        val viewModel = criarViewModel()

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
        val viewModel = criarViewModel()

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
        val bloqueado = criarViewModel()
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

    // region Espelho de leitura (4A parte 1)

    @Test
    fun `espelho nasce desligado e nada sobe sem o toque`() {
        val estado = viewModelComNomes().uiState.value

        assertFalse(estado.espelhoLigado)
        assertNull(estado.espelhoEndereco)
        assertNull(estado.espelhoFalha)
        assertEquals(emptySet<String>(), estado.espelhoConectados)
    }

    @Test
    fun `ligar o espelho publica o endereco e leva o elenco com ids estaveis`() {
        val viewModel = viewModelComNomes()

        viewModel.alternarEspelho()

        val estado = viewModel.uiState.value
        assertTrue(estado.espelhoLigado)
        assertEquals("http://192.168.0.7:8080", estado.espelhoEndereco)
        assertEquals(listOf("j1", "j2"), servidor.elenco.map { it.id })
        assertEquals(listOf("Jogador 1", "Jogador 2"), servidor.elenco.map { it.nome })
    }

    @Test
    fun `toques repetidos enquanto inicia sobem um unico servidor`() = runTest {
        servidor.portaoDoInicio = CompletableDeferred()
        val viewModel = viewModelComNomes()

        viewModel.alternarEspelho()
        assertTrue(viewModel.uiState.value.espelhoIniciando)
        viewModel.alternarEspelho()

        assertEquals(1, servidor.inicios)
        servidor.portaoDoInicio!!.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.espelhoIniciando)
        assertTrue(viewModel.uiState.value.espelhoLigado)
    }

    @Test
    fun `jogador que entra pelo navegador aparece conectado na lista do Setup`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarEspelho()

        servidor.entrarPelaWeb("j2")

        assertEquals(setOf("j2"), viewModel.uiState.value.espelhoConectados)
        assertEquals(EstadoNoEspelho.ENTROU, viewModel.uiState.value.estadoNoEspelho("j2"))
        assertEquals(EstadoNoEspelho.AGUARDANDO, viewModel.uiState.value.estadoNoEspelho("j1"))
    }

    @Test
    fun `mudanca na lista de jogadores chega ao espelho sem religar`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarEspelho()

        viewModel.adicionarJogador()
        viewModel.renomearJogador(2, "Carla")

        assertEquals(listOf("j1", "j2", "j3"), servidor.elenco.map { it.id })
        assertEquals("Carla", servidor.elenco.last().nome)
    }

    @Test
    fun `remover jogador nao faz o id de quem ficou escorregar para outra linha`() {
        val viewModel = viewModelComNomes(3)
        viewModel.alternarEspelho()

        viewModel.removerJogador(0)

        assertEquals(listOf("j2", "j3"), servidor.elenco.map { it.id })
        assertEquals(listOf("Jogador 2", "Jogador 3"), servidor.elenco.map { it.nome })
    }

    @Test
    fun `sem rede o switch volta a desligado com o motivo explicado`() {
        servidor.desfecho = ResultadoDoInicio.SemRede
        val viewModel = viewModelComNomes()

        viewModel.alternarEspelho()

        val estado = viewModel.uiState.value
        assertFalse(estado.espelhoLigado)
        assertNull(estado.espelhoEndereco)
        assertEquals(FalhaDoEspelho.SEM_REDE, estado.espelhoFalha)
        // O espelho é acessório: falhar nele não pode travar a partida.
        assertTrue(estado.podeComecar)
    }

    @Test
    fun `desligar o espelho derruba o servidor e limpa o endereco`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarEspelho()

        viewModel.alternarEspelho()

        val estado = viewModel.uiState.value
        assertFalse(estado.espelhoLigado)
        assertNull(estado.espelhoEndereco)
        assertTrue(servidor.parou)
    }

    @Test
    fun `comecar a partida nao exige ninguem conectado`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarEspelho()

        viewModel.confirmar()

        assertEquals(emptySet<String>(), viewModel.uiState.value.espelhoConectados)
        assertTrue(viewModel.uiState.value.podeComecar)
        assertEquals(listOf("Jogador 1", "Jogador 2"), viewModel.configuracaoPronta.value!!.jogadores.map { it.nome })
    }

    @Test
    fun `tocar em quem aguarda marca este aparelho e some da lista da web`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarEspelho()

        viewModel.tocarNaLinhaDoEspelho("j1")

        assertEquals("j1", viewModel.uiState.value.espelhoEsteAparelho)
        assertEquals(EstadoNoEspelho.ESTE_APARELHO, viewModel.uiState.value.estadoNoEspelho("j1"))
        assertFalse(servidor.oferecidoNaWeb("j1"))
    }

    @Test
    fun `tocar de novo em quem esta marcado desmarca`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarEspelho()
        viewModel.tocarNaLinhaDoEspelho("j1")

        viewModel.tocarNaLinhaDoEspelho("j1")

        assertNull(viewModel.uiState.value.espelhoEsteAparelho)
        assertTrue(servidor.oferecidoNaWeb("j1"))
    }

    @Test
    fun `marcar outro move o marcador e nunca deixa dois`() {
        val viewModel = viewModelComNomes(3)
        viewModel.alternarEspelho()
        viewModel.tocarNaLinhaDoEspelho("j1")

        viewModel.tocarNaLinhaDoEspelho("j3")

        assertEquals("j3", viewModel.uiState.value.espelhoEsteAparelho)
        assertEquals(EstadoNoEspelho.AGUARDANDO, viewModel.uiState.value.estadoNoEspelho("j1"))
    }

    @Test
    fun `desligar o espelho limpa o marcador junto`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarEspelho()
        viewModel.tocarNaLinhaDoEspelho("j1")

        viewModel.alternarEspelho()

        assertNull(viewModel.uiState.value.espelhoEsteAparelho)
    }

    @Test
    fun `tocar em quem entrou pela rede abre a confirmacao em vez de marcar`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarEspelho()
        servidor.entrarPelaWeb("j2")

        viewModel.tocarNaLinhaDoEspelho("j2")

        assertEquals("j2", viewModel.uiState.value.espelhoLugarALiberar)
        assertNull(viewModel.uiState.value.espelhoEsteAparelho)
        assertEquals(setOf("j2"), viewModel.uiState.value.espelhoConectados)
    }

    @Test
    fun `confirmar liberar devolve o lugar e o nome reaparece na web`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarEspelho()
        servidor.entrarPelaWeb("j2")
        viewModel.tocarNaLinhaDoEspelho("j2")

        viewModel.confirmarLiberarLugar()

        assertNull(viewModel.uiState.value.espelhoLugarALiberar)
        assertEquals(EstadoNoEspelho.AGUARDANDO, viewModel.uiState.value.estadoNoEspelho("j2"))
        assertTrue(servidor.oferecidoNaWeb("j2"))
    }

    @Test
    fun `cancelar a confirmacao nao mexe em nada`() {
        val viewModel = viewModelComNomes()
        viewModel.alternarEspelho()
        servidor.entrarPelaWeb("j2")
        viewModel.tocarNaLinhaDoEspelho("j2")

        viewModel.cancelarLiberarLugar()

        assertNull(viewModel.uiState.value.espelhoLugarALiberar)
        assertEquals(EstadoNoEspelho.ENTROU, viewModel.uiState.value.estadoNoEspelho("j2"))
    }

    // endregion
}
