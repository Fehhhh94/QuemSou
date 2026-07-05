package com.quemsou.app.presentation.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quemsou.app.R
import com.quemsou.app.presentation.ui.theme.QuemSouTheme

/**
 * Tela inicial do app: começar uma partida nova, ou (fase 4) entrar em uma
 * existente por código via Nearby Connections — por ora desabilitado.
 */
@Composable
fun HomeScreen(
    onCreateMatch: () -> Unit,
    onJoinWithCode: () -> Unit,
) {
    var mostrarComoJogar by remember { mutableStateOf(false) }

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
            Text(
                text = stringResource(id = R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = onCreateMatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(text = stringResource(id = R.string.home_create_match))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onJoinWithCode,
                    enabled = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text(text = stringResource(id = R.string.home_join_with_code))
                }
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(id = R.string.home_join_badge)) },
                )
            }
            TextButton(onClick = { mostrarComoJogar = true }) {
                Text(text = stringResource(id = R.string.home_how_to_play))
            }
        }
    }

    if (mostrarComoJogar) {
        AlertDialog(
            onDismissRequest = { mostrarComoJogar = false },
            title = { Text(stringResource(id = R.string.home_how_to_play_dialog_title)) },
            text = { Text(stringResource(id = R.string.home_how_to_play_dialog_body)) },
            confirmButton = {
                TextButton(onClick = { mostrarComoJogar = false }) {
                    Text(stringResource(id = R.string.home_how_to_play_dialog_close))
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    QuemSouTheme {
        HomeScreen(onCreateMatch = {}, onJoinWithCode = {})
    }
}
