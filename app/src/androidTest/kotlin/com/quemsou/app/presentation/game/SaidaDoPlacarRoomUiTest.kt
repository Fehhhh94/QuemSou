package com.quemsou.app.presentation.game

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quemsou.app.data.RepositorioDeCardsLocal
import com.quemsou.app.data.espelho.EstadoDaPartidaNoEspelho
import com.quemsou.app.data.espelho.JogadorDoEspelho
import com.quemsou.app.data.espelho.ResultadoDoInicio
import com.quemsou.app.data.espelho.ServidorDoEspelho
import com.quemsou.app.data.feedback.ModoDevFeedbackStore
import com.quemsou.app.data.feedback.RegistroDeFeedbackLocal
import com.quemsou.app.data.feedback.VotoDeCard
import com.quemsou.app.data.local.AppDatabase
import com.quemsou.app.data.local.paraEntidade
import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.Colecao
import com.quemsou.app.domain.model.DicaDoBanco
import com.quemsou.app.domain.model.EstadoDoBaralho
import com.quemsou.app.domain.repository.RepositorioDeCards
import com.quemsou.app.navigation.ConfiguracaoDaPartida
import com.quemsou.app.navigation.JogadorConfigurado
import com.quemsou.app.presentation.ui.theme.QuemSouTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercita o botão da tela real e a transação Room, com conteúdo fictício e banco isolado. */
@RunWith(AndroidJUnit4::class)
class SaidaDoPlacarRoomUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun voltarAoInicioFechaSessaoAntesDeNavegarEPreservaHistorico() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        val store = ViewModelStore()
        val liberarEncerramento = CompletableDeferred<Unit>()
        try {
            val cards = List(2) { indice ->
                val dicas = List(20) { DicaDoBanco("d$it", "Fato fictício $it da resposta $indice") }
                Card("c$indice", CardType.PESSOA, CardCategory.PERSONAGEM_FILME,
                    "Resposta fictícia $indice", dicas.take(10).map { it.texto }, "r$indice", dicas)
            }
            val baralho = Baralho("teste", "Baralho fictício", CardCategory.PERSONAGEM_FILME,
                Colecao("teste", "Teste", "T"), 1, EstadoDoBaralho.EM_DESENVOLVIMENTO, cards)
            db.baralhoDao().inserirTodos(listOf(baralho.paraEntidade()))
            db.cardDao().inserirTodos(cards.map { it.paraEntidade(baralho.id) })
            val local = RepositorioDeCardsLocal(db.baralhoDao(), db.cardDao(), db)
            var persistiuEncerramento = false
            val repo = object : RepositorioDeCards by local {
                override suspend fun encerrarSessao(sessao: String) {
                    liberarEncerramento.await()
                    local.encerrarSessao(sessao)
                    persistiuEncerramento = true
                }
            }
            val registro = RegistroDeFeedbackLocal(db.feedbackDeCardDao())
            val configuracao = ConfiguracaoDaPartida("LOBO", listOf(baralho.id), 2, true,
                listOf(JogadorConfigurado("Ana"), JogadorConfigurado("Bia")))
            val handle = SavedStateHandle(mapOf("configuracao" to configuracao.paraJson()))
            lateinit var vm: PartidaViewModel
            var saidas = 0
            var navegouAntesDePersistir = false
            compose.runOnIdle {
                vm = PartidaViewModel(handle, repo, modoDevDesligado, registro, espelhoDesligado)
                store.put("partida", vm)
            }
            compose.setContent {
                QuemSouTheme {
                    PartidaScreen(
                        onAbandonarPartida = { error("O placar não pede abandono") },
                        onVoltarAoInicio = { navegouAntesDePersistir = !persistiuEncerramento; saidas++ },
                        viewModel = vm,
                    )
                }
            }

            repeat(2) { indice ->
                compose.waitUntil(10_000) { vm.uiState.value is PartidaUiState.VezDeJogar }
                compose.onNodeWithText("Estou com o celular").performScrollTo().performClick()
                compose.waitUntil(10_000) { vm.uiState.value is PartidaUiState.Grid }
                compose.onNodeWithText("1").performClick()
                compose.waitUntil(10_000) { vm.uiState.value is PartidaUiState.DicaRevelada }
                if (indice == 0) {
                    compose.waitUntil(10_000) { vm.feedbackDaDica.value?.carregando == false }
                    compose.runOnIdle { vm.avaliarDica(vm.feedbackDaDica.value!!.chave, VotoDeCard.BOM) }
                    compose.waitUntil(10_000) { vm.feedbackDaDica.value?.salvo == true }
                }
                compose.onNodeWithText("Alguém acertou").performClick()
                compose.waitUntil(10_000) { vm.uiState.value is PartidaUiState.Anuncio }
                compose.onNodeWithText(if (indice == 0) "Próxima jogada" else "Ver placar")
                    .performScrollTo().performClick()
            }
            compose.waitUntil(10_000) { vm.uiState.value is PartidaUiState.PlacarFinal }
            assertEquals(20, (vm.uiState.value as PartidaUiState.PlacarFinal).ranking.sumOf { it.pontos })
            val dao = db.historicoDeDicasDao()
            val sessaoId = requireNotNull(handle.get<String>("sessao_dicas"))
            val sessaoAntes = requireNotNull(dao.sessao(sessaoId))
            val usadas = dao.usadas().toSet()
            val respostas = dao.respostas().toSet()
            val turnos = (1..2).map { dao.turno(sessaoId, it) }
            val feedbacks = registro.buscarTodosComResposta()
            val conteudo = local.buscarPorIds(listOf(baralho.id))
            assertFalse(sessaoAntes.encerrada)
            assertEquals("PLACAR_FINAL", local.progressoDaSessao(sessaoId)?.fase)
            assertTrue(usadas.isNotEmpty())
            assertEquals(1, feedbacks.size)
            assertTrue(dao.reservadas().isEmpty())

            compose.onNodeWithText("Voltar ao início").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals("Não pode navegar antes da transação de encerramento", 0, saidas)
                assertTrue(vm.uiState.value is PartidaUiState.Carregando)
            }
            assertFalse(requireNotNull(dao.sessao(sessaoId)).encerrada)
            liberarEncerramento.complete(Unit)
            compose.waitUntil(10_000) { saidas == 1 }

            assertFalse(navegouAntesDePersistir)
            assertEquals(sessaoAntes.copy(encerrada = true), dao.sessao(sessaoId))
            assertTrue(dao.sessoesAtivas().isEmpty())
            assertTrue(dao.reservadas().isEmpty())
            assertEquals(usadas, dao.usadas().toSet())
            assertEquals(respostas, dao.respostas().toSet())
            assertEquals(turnos, (1..2).map { dao.turno(sessaoId, it) })
            assertEquals(feedbacks, registro.buscarTodosComResposta())
            assertEquals(conteudo, local.buscarPorIds(listOf(baralho.id)))
        } finally {
            compose.runOnIdle { store.clear() }
            db.close()
        }
    }

    private val modoDevDesligado = object : ModoDevFeedbackStore {
        override val modoDevFeedback = MutableStateFlow(false)
        override suspend fun alternar() = false
    }

    private val espelhoDesligado = object : ServidorDoEspelho {
        override val endereco = MutableStateFlow<String?>(null)
        override val conectados = MutableStateFlow(emptyMap<String, Boolean>())
        override val esteAparelho = MutableStateFlow<String?>(null)
        override suspend fun iniciar(jogadores: List<JogadorDoEspelho>): ResultadoDoInicio =
            error("A partida de teste é offline")
        override fun atualizarJogadores(jogadores: List<JogadorDoEspelho>) = Unit
        override fun marcarEsteAparelho(jogadorId: String?) = Unit
        override fun liberarLugar(jogadorId: String) = Unit
        override fun publicarEstado(estado: EstadoDaPartidaNoEspelho) = Unit
        override fun parar() = Unit
    }
}
