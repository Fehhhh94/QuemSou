package com.quemsou.app.presentation.ui.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.quemsou.app.R
import com.quemsou.app.presentation.ui.theme.QuemSouTheme

/**
 * Tela de configuração da partida (jogadores, categorias, regras).
 * Placeholder da Fase 0 — nenhuma regra de jogo foi implementada ainda.
 */
@Composable
fun SetupScreen() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupScreenPreview() {
    QuemSouTheme {
        SetupScreen()
    }
}
