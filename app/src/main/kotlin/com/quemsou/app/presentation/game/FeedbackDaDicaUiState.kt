package com.quemsou.app.presentation.game

import com.quemsou.app.data.feedback.VotoDeCard

data class FeedbackDaDicaUiState(
    val chave: String,
    val voto: VotoDeCard? = null,
    val comentario: String = "",
    val carregando: Boolean = true,
    val salvando: Boolean = false,
    val salvo: Boolean = false,
    val erro: Boolean = false,
)
