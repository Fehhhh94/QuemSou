package com.quemsou.app.presentation.ui.home

import android.content.Intent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.quemsou.app.presentation.ui.theme.FestaAzul
import com.quemsou.app.presentation.ui.theme.FestaLima
import com.quemsou.app.presentation.ui.theme.FestaTinta
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quemsou.app.BuildConfig
import com.quemsou.app.R
import com.quemsou.app.presentation.ui.components.ConfirmDialog
import com.quemsou.app.presentation.ui.theme.QuemSouTheme
import kotlinx.coroutines.launch

/** Central dos jogos locais. O QuemSou é o primeiro jogo disponível. */
@Composable
fun HomeScreen(
    onCreateMatch: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    onCriarBaralho: () -> Unit = {},
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
        onCriarBaralho = onCriarBaralho,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeContent(
    snackbarHostState: SnackbarHostState,
    versaoDoBuild: String,
    modoDev: Boolean,
    exportarVisivel: Boolean,
    quantidadeDeFeedback: Int,
    onCreateMatch: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    onAlternarModoDev: () -> Unit,
    onExportarFeedback: () -> Unit,
    onLimparFeedback: () -> Unit,
    onCriarBaralho: () -> Unit = {},
) {
    // `rememberSaveable`: girar o aparelho ou mudar o tamanho da fonte não
    // pode fechar o "Como jogar" nem recolher a criação de baralhos que a
    // pessoa acabou de abrir.
    var mostrarComoJogar by rememberSaveable { mutableStateOf(false) }
    var confirmarLimpeza by rememberSaveable { mutableStateOf(false) }
    var mostrarAjustes by rememberSaveable { mutableStateOf(false) }

    // Numa janela baixa (paisagem no celular, fonte muito ampliada) a arte
    // decorativa empurrava "Jogar QuemSou" para fora da primeira tela: quem
    // abre o app vê enfeite e precisa descobrir a rolagem para achar a ação.
    // A arte não carrega informação (tem `clearAndSetSemantics`), então é ela
    // que sai — a chamada para jogar fica.
    val alturaDaJanela = LocalConfiguration.current.screenHeightDp
    val janelaBaixa = alturaDaJanela < ALTURA_MINIMA_PARA_A_ARTE

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxSize().padding(horizontal = 20.dp)) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.titleLarge)
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                            Text(stringResource(R.string.party_tag), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.party_greeting), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                        Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    CartaoDoJogo(
                        nome = stringResource(R.string.party_game_name),
                        descricao = stringResource(R.string.party_game_description),
                        detalhes = stringResource(R.string.party_game_details),
                        rotuloDeJogar = stringResource(R.string.home_create_match),
                        onJogar = onCreateMatch,
                        ilustracao = { if (!janelaBaixa) CartasDaFesta(Modifier.fillMaxWidth()) },
                        acoesDeApoio = {
                            OutlinedButton(
                                onClick = onAbrirCatalogo,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = .6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.home_baralhos))
                            }
                            TextButton(
                                onClick = { mostrarComoJogar = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.home_how_to_play))
                            }
                        },
                    )
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.party_make_it_yours), style = MaterialTheme.typography.titleMedium)
                            RodapeDevDeFeedback(
                                modoDev = modoDev,
                                exportarVisivel = exportarVisivel,
                                quantidadeDeFeedback = quantidadeDeFeedback,
                                onAlternarModoDev = onAlternarModoDev,
                                onExportar = onExportarFeedback,
                                onLimpar = { confirmarLimpeza = true },
                            )
                            TextButton(onClick = { mostrarAjustes = !mostrarAjustes }) {
                                Text(stringResource(if (mostrarAjustes) R.string.party_less_options else R.string.party_more_options))
                            }
                            if (mostrarAjustes) {
                                Text(stringResource(R.string.party_factory_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedButton(onClick = onCriarBaralho, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                    Text(stringResource(R.string.fabrica_titulo))
                                }
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.home_versao, versaoDoBuild),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
                )
            }
        }
    }

    if (mostrarComoJogar) {
        AlertDialog(
            onDismissRequest = { mostrarComoJogar = false },
            title = { Text(stringResource(id = R.string.home_how_to_play_dialog_title)) },
            text = { Text(stringResource(id = R.string.home_how_to_play_dialog_body), modifier = Modifier.verticalScroll(rememberScrollState())) },
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

/** Preferência de avaliação e ações dos registros locais do QuemSou. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RodapeDevDeFeedback(
    modoDev: Boolean,
    exportarVisivel: Boolean,
    quantidadeDeFeedback: Int,
    onAlternarModoDev: () -> Unit,
    onExportar: () -> Unit,
    onLimpar: () -> Unit,
) {
    val acento = MaterialTheme.colorScheme.primary
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
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = modoDev,
                onCheckedChange = { onAlternarModoDev() },
                colors = SwitchDefaults.colors(checkedTrackColor = acento),
            )
        }
        if (exportarVisivel) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

/** Abaixo desta altura de janela, a arte decorativa da Home sai de cena. */
private const val ALTURA_MINIMA_PARA_A_ARTE = 560

/**
 * Entrada de um jogo na central: ilustração, identidade, chamada para jogar
 * e ações de apoio. É o ponto de extensão do "Bora Jogar" — um segundo jogo
 * é outra chamada deste composable, não outra cópia do layout. Nenhum
 * registro/catálogo de jogos foi inventado: existe um jogo, e a central só
 * mostra o que funciona.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoDoJogo(
    nome: String,
    descricao: String,
    detalhes: String,
    rotuloDeJogar: String,
    onJogar: () -> Unit,
    ilustracao: @Composable () -> Unit,
    acoesDeApoio: @Composable FlowRowScope.() -> Unit,
) {
    Surface(color = FestaAzul, contentColor = Color.White, shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ilustracao()
            Text(nome, style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
            Text(descricao, style = MaterialTheme.typography.bodyLarge)
            Text(detalhes, style = MaterialTheme.typography.labelLarge)
            Button(
                onClick = onJogar,
                colors = ButtonDefaults.buttonColors(containerColor = FestaLima, contentColor = FestaTinta),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(rotuloDeJogar) }
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = acoesDeApoio,
            )
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
            onAlternarModoDev = {},
            onExportarFeedback = {},
            onLimparFeedback = {},
        )
    }
}
