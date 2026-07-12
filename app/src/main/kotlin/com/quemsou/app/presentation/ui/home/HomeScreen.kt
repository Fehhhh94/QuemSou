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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quemsou.app.R
import com.quemsou.app.presentation.ui.theme.QuemSouTheme

/**
 * Tela inicial do app: começar uma partida nova, abrir o catálogo de
 * baralhos, ou (fase 4) entrar em uma partida por código via Nearby
 * Connections — por ora desabilitado.
 *
 * O título esconde o easter egg do **modo dev de feedback** (7 toques, padrão
 * Android de opções de desenvolvedor); a alternância é confirmada por
 * Snackbar. Não há nenhuma outra entrada de UI para o modo.
 */
@Composable
fun HomeScreen(
    onCreateMatch: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    onJoinWithCode: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val avisoDeModoDev by viewModel.avisoDeModoDev.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val textoLigado = stringResource(R.string.home_dev_snackbar_ligado)
    val textoDesligado = stringResource(R.string.home_dev_snackbar_desligado)
    LaunchedEffect(avisoDeModoDev) {
        val ligado = avisoDeModoDev ?: return@LaunchedEffect
        viewModel.avisoDeModoDevExibido()
        snackbarHostState.showSnackbar(if (ligado) textoLigado else textoDesligado)
    }

    HomeContent(
        snackbarHostState = snackbarHostState,
        onCreateMatch = onCreateMatch,
        onAbrirCatalogo = onAbrirCatalogo,
        onJoinWithCode = onJoinWithCode,
        onSeteToquesNoTitulo = viewModel::alternarModoDev,
    )
}

@Composable
private fun HomeContent(
    snackbarHostState: SnackbarHostState,
    onCreateMatch: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    onJoinWithCode: () -> Unit,
    onSeteToquesNoTitulo: () -> Unit,
) {
    var mostrarComoJogar by remember { mutableStateOf(false) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TituloComEasterEgg(onSeteToques = onSeteToquesNoTitulo)
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
            OutlinedButton(
                onClick = onAbrirCatalogo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(text = stringResource(id = R.string.home_baralhos))
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

/**
 * Título da Home com o contador escondido de 7 toques (easter egg do modo dev
 * de feedback). Toques com mais de 2 s de intervalo reiniciam a contagem;
 * `detectTapGestures` (sem `clickable`) para não ganhar ripple nem semântica
 * de botão — o título continua parecendo um título.
 */
@Composable
private fun TituloComEasterEgg(onSeteToques: () -> Unit) {
    var toques by remember { mutableIntStateOf(0) }
    var ultimoToqueEmMs by remember { mutableLongStateOf(0L) }

    Text(
        text = stringResource(id = R.string.home_title),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures {
                val agora = System.currentTimeMillis()
                toques = if (agora - ultimoToqueEmMs > JANELA_ENTRE_TOQUES_MS) 1 else toques + 1
                ultimoToqueEmMs = agora
                if (toques == TOQUES_PARA_ALTERNAR) {
                    toques = 0
                    onSeteToques()
                }
            }
        },
    )
}

private const val TOQUES_PARA_ALTERNAR = 7
private const val JANELA_ENTRE_TOQUES_MS = 2_000L

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    QuemSouTheme {
        HomeContent(
            snackbarHostState = SnackbarHostState(),
            onCreateMatch = {},
            onAbrirCatalogo = {},
            onJoinWithCode = {},
            onSeteToquesNoTitulo = {},
        )
    }
}
