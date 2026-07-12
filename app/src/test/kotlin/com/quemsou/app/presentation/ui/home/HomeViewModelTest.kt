package com.quemsou.app.presentation.ui.home

import com.quemsou.app.data.feedback.ModoDevFeedbackStore
import com.quemsou.app.data.feedback.NovoFeedback
import com.quemsou.app.data.feedback.RegistroDeFeedback
import com.quemsou.app.data.local.FeedbackComResposta
import com.quemsou.app.data.local.FeedbackDeCardEntity
import com.quemsou.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Guarda o modo em memória — sem DataStore. */
    private class ModoDevFake(ativo: Boolean = false) : ModoDevFeedbackStore {
        private val estado = MutableStateFlow(ativo)

        override val modoDevFeedback: Flow<Boolean> = estado

        override suspend fun alternar(): Boolean {
            estado.value = !estado.value
            return estado.value
        }
    }

    /** Devolve os registros configurados em memória — sem Room. */
    private class RegistroFake(
        private val existentes: MutableList<FeedbackComResposta> = mutableListOf(),
    ) : RegistroDeFeedback {
        private val quantidade = MutableStateFlow(existentes.size)

        override suspend fun registrar(novo: NovoFeedback) = error("a Home nunca grava")

        override fun quantidade(): Flow<Int> = quantidade

        override suspend fun buscarTodosComResposta() = existentes.toList()

        override suspend fun apagarTudo() {
            existentes.clear()
            quantidade.value = 0
        }
    }

    private fun umRegistro() = FeedbackComResposta(
        feedback = FeedbackDeCardEntity(
            id = 1,
            baralhoId = "cinema-classico-1",
            cardId = "cc1-01",
            voto = "BOM",
            comentario = null,
            rodada = 1,
            resultadoDoTurno = "ACERTO",
            numeroDaDicaDoAcerto = 2,
            criadoEm = 1_752_192_000_000, // 2025-07-11T00:00:00Z
        ),
        resposta = "CHAPLIN",
    )

    private fun viewModel(
        modoDev: Boolean = false,
        registro: RegistroDeFeedback = RegistroFake(),
    ) = HomeViewModel(ModoDevFake(modoDev), registro)

    @Test
    fun `alternar liga o modo dev e agenda o aviso de ativado`() {
        val viewModel = viewModel(modoDev = false)
        assertFalse(viewModel.modoDev.value)
        assertNull(viewModel.avisoDeModoDev.value)

        viewModel.alternarModoDev()

        assertTrue(viewModel.modoDev.value)
        assertEquals(true, viewModel.avisoDeModoDev.value)
    }

    @Test
    fun `alternar de novo desliga o modo dev e agenda o aviso de desativado`() {
        val viewModel = viewModel(modoDev = true)
        assertTrue(viewModel.modoDev.value)

        viewModel.alternarModoDev()

        assertFalse(viewModel.modoDev.value)
        assertEquals(false, viewModel.avisoDeModoDev.value)
    }

    @Test
    fun `aviso exibido limpa o one-shot sem mexer no modo`() {
        val viewModel = viewModel(modoDev = false)
        viewModel.alternarModoDev()

        viewModel.avisoDeModoDevExibido()

        assertNull(viewModel.avisoDeModoDev.value)
        assertTrue(viewModel.modoDev.value)
    }

    @Test
    fun `exportar so e visivel com modo dev ligado e registros presentes`() {
        // Modo desligado esconde o item mesmo com registros no Room.
        assertFalse(viewModel(modoDev = false, registro = RegistroFake(mutableListOf(umRegistro()))).exportarVisivel.value)
        // Modo ligado sem registro nenhum também esconde (N == 0).
        assertFalse(viewModel(modoDev = true).exportarVisivel.value)

        val visivel = viewModel(modoDev = true, registro = RegistroFake(mutableListOf(umRegistro())))
        assertTrue(visivel.exportarVisivel.value)
        assertEquals(1, visivel.quantidadeDeFeedback.value)
    }

    @Test
    fun `montar json de export embala os registros no formato quemsou-feedback`() = runTest {
        val viewModel = viewModel(modoDev = true, registro = RegistroFake(mutableListOf(umRegistro())))

        val json = viewModel.montarJsonDeExport()

        assertTrue(json.contains("\"formato\": \"quemsou-feedback\""))
        assertTrue(json.contains("\"versao\": 1"))
        assertTrue(json.contains("\"cardId\": \"cc1-01\""))
        assertTrue(json.contains("\"resposta\": \"CHAPLIN\""))
    }

    @Test
    fun `limpar feedback apaga tudo e esconde o export`() {
        val registro = RegistroFake(mutableListOf(umRegistro()))
        val viewModel = viewModel(modoDev = true, registro = registro)
        assertTrue(viewModel.exportarVisivel.value)

        viewModel.limparFeedback()

        assertEquals(0, viewModel.quantidadeDeFeedback.value)
        assertFalse(viewModel.exportarVisivel.value)
    }
}
