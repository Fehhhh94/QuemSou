package com.quemsou.app.presentation.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quemsou.app.R
import com.quemsou.app.presentation.ui.theme.QuemSouTheme

/**
 * Tela inicial do app. Oferece as duas entradas do fluxo: criar uma partida nova
 * ou entrar em uma partida existente através de um código. Nesta fase os botões
 * apenas navegam — nenhuma regra de jogo foi implementada ainda.
 */
@Composable
fun HomeScreen(
    onCreateMatch: () -> Unit,
    onJoinWithCode: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Button(onClick = onCreateMatch) {
                Text(text = stringResource(id = R.string.home_create_match))
            }
            Button(onClick = onJoinWithCode) {
                Text(text = stringResource(id = R.string.home_join_with_code))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    QuemSouTheme {
        HomeScreen(onCreateMatch = {}, onJoinWithCode = {})
    }
}
