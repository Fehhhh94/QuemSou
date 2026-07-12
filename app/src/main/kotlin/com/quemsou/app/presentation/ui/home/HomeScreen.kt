package com.quemsou.app.presentation.ui.home

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quemsou.app.BuildConfig
import com.quemsou.app.R
import com.quemsou.app.presentation.ui.components.ConfirmDialog
import com.quemsou.app.presentation.ui.theme.DevVioleta
import com.quemsou.app.presentation.ui.theme.DevVioletaEscuro
import com.quemsou.app.presentation.ui.theme.QuemSouTheme
import kotlinx.coroutines.launch

/**
 * Tela inicial do app: começar uma partida nova, abrir o catálogo de
 * baralhos, ou (fase 4) entrar em uma partida por código via Nearby
 * Connections — por ora desabilitado.
 *
 * O rodapé traz o Switch **"Modo dev"** do feedback de cards — controle
 * VISÍVEL deliberado (o feedback vai virar feature pública; a transição
 * está registrada em `docs/IMPROVEMENTS.md`), discreto para não competir
 * com as ações principais. A alternância é confirmada por Snackbar; com o
 * modo ligado e registros no Room, o rodapé agrupa também o "Exportar
 * feedback (N)" e o "Limpar feedback".
 */
@Composable
fun HomeScreen(
    onCreateMatch: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    onJoinWithCode: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val modoDev by viewModel.modoDev.collectAsState()
    val avisoDeModoDev by viewModel.avisoDeModoDev.collectAsState()
    val exportarVisivel by viewModel.exportarVisivel.collectAsState()
    val quantidadeDeFeedback by viewModel.quantidadeDeFeedback.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val contexto = LocalContext.current

    val textoLigado = stringResource(R.string.home_dev_snackbar_ligado)
    val textoDesligado = stringResource(R.string.home_dev_snackbar_desligado)
    LaunchedEffect(avisoDeModoDev) {
        val ligado = avisoDeModoDev ?: return@LaunchedEffect
        viewModel.avisoDeModoDevExibido()
        snackbarHostState.showSnackbar(if (ligado) textoLigado else textoDesligado)
    }

    HomeContent(
        snackbarHostState = snackbarHostState,
        versaoDoBuild = BuildConfig.VERSION_NAME,
        modoDev = modoDev,
        exportarVisivel = exportarVisivel,
        quantidadeDeFeedback = quantidadeDeFeedback,
        onCreateMatch = onCreateMatch,
        onAbrirCatalogo = onAbrirCatalogo,
        onJoinWithCode = onJoinWithCode,
        onAlternarModoDev = viewModel::alternarModoDev,
        onExportarFeedback = {
            // Mesmo padrão do "Pedir um baralho": ACTION_SEND text/plain — o
            // JSON sai pelo app que o dev escolher; nada é transmitido daqui.
            escopo.launch {
                val json = viewModel.montarJsonDeExport()
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                contexto.startActivity(Intent.createChooser(intent, null))
            }
        },
        onLimparFeedback = viewModel::limparFeedback,
    )
}

@Composable
private fun HomeContent(
    snackbarHostState: SnackbarHostState,
    versaoDoBuild: String,
    modoDev: Boolean,
    exportarVisivel: Boolean,
    quantidadeDeFeedback: Int,
    onCreateMatch: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    onJoinWithCode: () -> Unit,
    onAlternarModoDev: () -> Unit,
    onExportarFeedback: () -> Unit,
    onLimparFeedback: () -> Unit,
) {
    var mostrarComoJogar by remember { mutableStateOf(false) }
    var confirmarLimpeza by remember { mutableStateOf(false) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
            RodapeDevDeFeedback(
                modoDev = modoDev,
                exportarVisivel = exportarVisivel,
                quantidadeDeFeedback = quantidadeDeFeedback,
                onAlternarModoDev = onAlternarModoDev,
                onExportar = onExportarFeedback,
                onLimpar = { confirmarLimpeza = true },
            )
            // Identificação de build sempre visível (lição da validação
            // física: um APK defasado passou despercebido porque a tela não
            // dizia qual build estava rodando). Debug ganha sufixo por build.
            Text(
                text = stringResource(R.string.home_versao, versaoDoBuild),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
            )
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

    if (confirmarLimpeza) {
        ConfirmDialog(
            titulo = stringResource(R.string.home_dev_limpar_titulo),
            texto = stringResource(R.string.home_dev_limpar_corpo),
            textoConfirmar = stringResource(R.string.home_dev_limpar_confirmar),
            textoCancelar = stringResource(R.string.home_dev_limpar_cancelar),
            onConfirmar = {
                confirmarLimpeza = false
                onLimparFeedback()
            },
            onCancelar = { confirmarLimpeza = false },
        )
    }
}

/**
 * Rodapé do modo dev de feedback: a linha "Modo dev" com o Switch (controle
 * visível deliberado — ver KDoc da tela) e, com o modo ligado e registros no
 * Room, o export/limpeza agrupados logo abaixo. Fica no rodapé de propósito:
 * presente sem competir com as ações principais da Home.
 */
@Composable
private fun RodapeDevDeFeedback(
    modoDev: Boolean,
    exportarVisivel: Boolean,
    quantidadeDeFeedback: Int,
    onAlternarModoDev: () -> Unit,
    onExportar: () -> Unit,
    onLimpar: () -> Unit,
) {
    val acento = if (isSystemInDarkTheme()) DevVioleta else DevVioletaEscuro
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_dev_switch),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = modoDev,
                onCheckedChange = { onAlternarModoDev() },
                colors = SwitchDefaults.colors(checkedTrackColor = acento),
            )
        }
        if (exportarVisivel) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onExportar) {
                    Text(
                        text = stringResource(R.string.home_dev_exportar, quantidadeDeFeedback),
                        style = MaterialTheme.typography.labelLarge,
                        color = acento,
                    )
                }
                TextButton(onClick = onLimpar) {
                    Text(
                        text = stringResource(R.string.home_dev_limpar),
                        style = MaterialTheme.typography.labelLarge,
                        color = acento,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    QuemSouTheme {
        HomeContent(
            snackbarHostState = SnackbarHostState(),
            versaoDoBuild = "0.5.0-dev",
            modoDev = false,
            exportarVisivel = false,
            quantidadeDeFeedback = 0,
            onCreateMatch = {},
            onAbrirCatalogo = {},
            onJoinWithCode = {},
            onAlternarModoDev = {},
            onExportarFeedback = {},
            onLimparFeedback = {},
        )
    }
}
