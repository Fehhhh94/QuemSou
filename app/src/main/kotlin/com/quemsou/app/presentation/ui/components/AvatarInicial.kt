package com.quemsou.app.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Círculo com a inicial de [nome], usado para identificar um jogador de forma
 * legível a distância (leitor da rodada, escolhedor da vez etc.).
 */
@Composable
fun AvatarInicial(
    nome: String,
    modifier: Modifier = Modifier,
    tamanho: Dp = 64.dp,
) {
    Box(
        modifier = modifier
            .size(tamanho)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = nome.trim().take(1).uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
