package com.quemsou.app.presentation.ui.game

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
 * Tela principal do jogo, onde cada jogador verá seu card com a resposta secreta
 * e as 10 dicas em curva de dificuldade. Placeholder da Fase 0 — nenhuma regra
 * de jogo foi implementada ainda.
 */
@Composable
fun GameScreen() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = R.string.game_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameScreenPreview() {
    QuemSouTheme {
        GameScreen()
    }
}
