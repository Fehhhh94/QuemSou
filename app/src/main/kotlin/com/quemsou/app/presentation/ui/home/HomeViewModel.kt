package com.quemsou.app.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quemsou.app.data.feedback.ModoDevFeedbackStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel da Home — existe só pelo **modo dev de feedback** (5B parte 2):
 * alternância pelo easter egg de 7 toques no título e, com o modo ligado, o
 * export/limpeza dos registros. Nada aqui toca o fluxo normal do jogador.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val modoDevStore: ModoDevFeedbackStore,
) : ViewModel() {

    /** `true` com o modo dev de feedback ligado. */
    val modoDev: StateFlow<Boolean> = modoDevStore.modoDevFeedback
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _avisoDeModoDev = MutableStateFlow<Boolean?>(null)

    /**
     * Aviso one-shot da alternância para o Snackbar da Home: `true` = acabou
     * de ligar, `false` = acabou de desligar, `null` = nada a avisar. A UI
     * consome com [avisoDeModoDevExibido].
     */
    val avisoDeModoDev: StateFlow<Boolean?> = _avisoDeModoDev.asStateFlow()

    /** 7 toques no título completados: alterna o modo dev e agenda o aviso. */
    fun alternarModoDev() {
        viewModelScope.launch {
            _avisoDeModoDev.value = modoDevStore.alternar()
        }
    }

    /** O Snackbar mostrou o aviso — limpa para não reexibir em recomposição. */
    fun avisoDeModoDevExibido() {
        _avisoDeModoDev.value = null
    }
}
