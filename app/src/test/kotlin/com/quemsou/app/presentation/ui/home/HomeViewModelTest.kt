package com.quemsou.app.presentation.ui.home

import com.quemsou.app.data.feedback.ModoDevFeedbackStore
import com.quemsou.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    @Test
    fun `alternar liga o modo dev e agenda o aviso de ativado`() {
        val viewModel = HomeViewModel(ModoDevFake(ativo = false))
        assertFalse(viewModel.modoDev.value)
        assertNull(viewModel.avisoDeModoDev.value)

        viewModel.alternarModoDev()

        assertTrue(viewModel.modoDev.value)
        assertEquals(true, viewModel.avisoDeModoDev.value)
    }

    @Test
    fun `alternar de novo desliga o modo dev e agenda o aviso de desativado`() {
        val viewModel = HomeViewModel(ModoDevFake(ativo = true))
        assertTrue(viewModel.modoDev.value)

        viewModel.alternarModoDev()

        assertFalse(viewModel.modoDev.value)
        assertEquals(false, viewModel.avisoDeModoDev.value)
    }

    @Test
    fun `aviso exibido limpa o one-shot sem mexer no modo`() {
        val viewModel = HomeViewModel(ModoDevFake(ativo = false))
        viewModel.alternarModoDev()

        viewModel.avisoDeModoDevExibido()

        assertNull(viewModel.avisoDeModoDev.value)
        assertTrue(viewModel.modoDev.value)
    }
}
