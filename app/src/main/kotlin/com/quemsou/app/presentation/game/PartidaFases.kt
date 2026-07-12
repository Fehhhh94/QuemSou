package com.quemsou.app.presentation.game

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quemsou.app.R
import com.quemsou.app.data.feedback.VotoDeCard
import com.quemsou.app.presentation.ui.components.AvatarInicial
import com.quemsou.app.presentation.ui.components.BarraDeAcaoInferior
import com.quemsou.app.presentation.ui.components.ChipDeJogador
import com.quemsou.app.presentation.ui.components.ChipTipoDeCard
import com.quemsou.app.presentation.ui.components.RodapeDePontos
import com.quemsou.app.presentation.ui.theme.ShotAmbar
import com.quemsou.app.presentation.ui.theme.ShotAmbarEscuro
import com.quemsou.app.presentation.ui.theme.ShotOnAmbar

@Composable
internal fun CarregandoContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun VezDeJogarContent(
    estado: PartidaUiState.VezDeJogar,
    onEstouComOCelular: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.partida_vez_rodada, estado.rodada, estado.totalDeRodadas),
            style = MaterialTheme.typography.titleMedium,
        )
        AvatarInicial(nome = estado.nomeDoLeitor, tamanho = 96.dp)
        Text(
            text = stringResource(R.string.partida_vez_leitor_le, estado.nomeDoLeitor),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.partida_vez_passe_o_celular, estado.nomeDoLeitor),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.nomesDosAdivinhadores.forEach { nome -> ChipDeJogador(nome = nome) }
        }
        Button(
            onClick = onEstouComOCelular,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.partida_vez_estou_com_celular))
        }
    }
}

@Composable
internal fun GridContent(
    estado: PartidaUiState.Grid,
    onRevelarDica: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.partida_grid_rodada_leitor, estado.rodada, estado.nomeDoLeitor),
                    style = MaterialTheme.typography.titleMedium,
                )
                ChipTipoDeCard(tipo = estado.tipo)
            }
            AreaDaResposta(resposta = estado.respostaParaOLeitor)
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.partida_grid_vez_de_escolher, estado.nomeDoEscolhedor),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
            GradeDeNumeros(posicoesReveladas = estado.posicoesReveladas, onTocar = onRevelarDica)
        }
        RodapeDePontos(
            texto = stringResource(
                R.string.partida_grid_rodape,
                estado.posicoesReveladas.size,
                estado.pontosEmJogo,
            ),
        )
    }
}

/** A resposta só aparece enquanto pressionada — nunca fica fixa na tela. */
@Composable
private fun AreaDaResposta(resposta: String) {
    var pressionada by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressionada = true
                        tryAwaitRelease()
                        pressionada = false
                    },
                )
            },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (pressionada) resposta else stringResource(R.string.partida_grid_segure_para_ver),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (pressionada) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun GradeDeNumeros(posicoesReveladas: List<Int>, onTocar: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..10).chunked(5).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                linha.forEach { posicao ->
                    val revelada = posicao in posicoesReveladas
                    Surface(
                        onClick = { if (!revelada) onTocar(posicao) },
                        enabled = !revelada,
                        shape = RoundedCornerShape(12.dp),
                        color = if (revelada) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (revelada) {
                                Icon(Icons.Default.Check, contentDescription = "$posicao")
                            } else {
                                Text(
                                    text = "$posicao",
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Overlay do Modo Shot: folha inferior sobre scrim escuro, por cima do grid da
 * fase anterior. Não é dispensável por toque no scrim — só o "Bebi!" avança;
 * o botão voltar segue o padrão da partida (confirmação de abandono pelo
 * `BackHandler` da tela). A paleta âmbar é exclusiva do modo — nada de âmbar
 * no grid ao fundo.
 */
@Composable
internal fun ShotContent(
    estado: PartidaUiState.Shot,
    onBebi: () -> Unit,
) {
    val ambar = if (isSystemInDarkTheme()) ShotAmbar else ShotAmbarEscuro
    Box(Modifier.fillMaxSize()) {
        GridContent(estado = estado.grid, onRevelarDica = {})
        // Scrim escuro que consome qualquer toque sem ação: o grid fica
        // inalcançável e o overlay não é dispensável.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = stringResource(R.string.partida_shot_emoji), fontSize = 56.sp)
                    Text(
                        text = stringResource(R.string.partida_shot_titulo),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ambar,
                    )
                    Text(
                        text = stringResource(R.string.partida_shot_corpo, estado.nomeDoBebedor, estado.posicao),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.partida_shot_subtitulo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                BarraDeAcaoInferior(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onBebi,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShotAmbar,
                            contentColor = ShotOnAmbar,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text(stringResource(R.string.partida_shot_bebi))
                    }
                }
            }
        }
    }
}

@Composable
internal fun DicaReveladaContent(
    estado: PartidaUiState.DicaRevelada,
    onAlguemAcertou: () -> Unit,
    onOutraDica: () -> Unit,
    onPedirQueimar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.partida_dica_titulo, estado.posicao, estado.valor),
                style = MaterialTheme.typography.titleMedium,
            )
            ChipTipoDeCard(tipo = estado.tipo)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = estado.texto,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onAlguemAcertou,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.partida_dica_alguem_acertou))
        }
        OutlinedButton(
            onClick = onOutraDica,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.partida_dica_outra_dica))
        }
        TextButton(onClick = onPedirQueimar, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.partida_dica_queimar_link))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuemAcertouSheet(
    estado: PartidaUiState.QuemAcertou,
    onRegistrarAcerto: (String) -> Unit,
    onVoltar: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onVoltar, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.partida_quem_acertou_titulo), style = MaterialTheme.typography.titleLarge)
            estado.adivinhadores.forEach { adivinhador ->
                Button(
                    onClick = { onRegistrarAcerto(adivinhador.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(adivinhador.nome)
                }
            }
            if (estado.pontosDoLeitor > 0) {
                Text(
                    text = stringResource(
                        R.string.partida_quem_acertou_leitor_nota,
                        estado.nomeDoLeitor,
                        estado.pontosDoLeitor,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onVoltar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.partida_quem_acertou_voltar))
            }
        }
    }
}

/**
 * Fim de turno com a resposta revelada. [feedbackDev] não nulo (modo dev de
 * feedback ligado) insere o widget dev entre o bloco da resposta e o botão
 * de avançar — com o modo desligado, nenhum composable do widget entra na
 * composição. O `imePadding` + scroll deixam o bloco da resposta encolher com
 * o teclado do comentário aberto, mantendo a resposta visível.
 */
@Composable
internal fun AnuncioContent(
    estado: PartidaUiState.Anuncio,
    feedbackDev: FeedbackDevUiState?,
    onVotar: (VotoDeCard) -> Unit,
    onComentar: (String) -> Unit,
    onProximoTurno: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (estado) {
            is PartidaUiState.Anuncio.Acerto -> {
                Text(
                    text = stringResource(R.string.partida_anuncio_acerto_titulo, estado.nomeDoAcertador),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.partida_anuncio_acerto_subtitulo, estado.dicasUsadas),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            is PartidaUiState.Anuncio.Queimado -> {
                Text(
                    text = stringResource(R.string.partida_anuncio_queimado_titulo),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.partida_anuncio_queimado_subtitulo),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.partida_anuncio_resposta_era), style = MaterialTheme.typography.labelLarge)
                Text(estado.resposta, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (estado is PartidaUiState.Anuncio.Acerto) {
                AssistChipDePontos(stringResource(R.string.partida_anuncio_pontos_acertador, estado.pontosDoAcertador))
            }
            if (estado.pontosDoLeitor > 0) {
                AssistChipDePontos(
                    stringResource(R.string.partida_anuncio_pontos_leitor, estado.nomeDoLeitor, estado.pontosDoLeitor),
                )
            }
        }
        if (feedbackDev != null) {
            FeedbackDevWidget(
                estado = feedbackDev,
                onVotar = onVotar,
                onComentar = onComentar,
            )
        }
        Button(
            onClick = onProximoTurno,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                stringResource(
                    if (estado.ultimaRodada) R.string.partida_anuncio_ver_placar else R.string.partida_anuncio_proximo_turno,
                ),
            )
        }
    }
}

@Composable
private fun AssistChipDePontos(texto: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
internal fun PlacarFinalContent(
    estado: PartidaUiState.PlacarFinal,
    onJogarDeNovo: () -> Unit,
    onVoltarAoInicio: () -> Unit,
) {
    val maiorPontuacao = estado.ranking.maxOf { it.pontos }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.placar_titulo), style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (estado.empate) {
                stringResource(R.string.placar_empate, estado.vencedores.joinToString(" e "))
            } else {
                stringResource(R.string.placar_vencedor, estado.vencedores.first())
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            estado.ranking.forEach { linha ->
                val emPrimeiro = linha.pontos == maiorPontuacao
                Surface(
                    color = if (emPrimeiro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(linha.nome, style = MaterialTheme.typography.titleMedium)
                        Text("${linha.pontos} pts", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Button(
            onClick = onJogarDeNovo,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.placar_jogar_de_novo))
        }
        OutlinedButton(
            onClick = onVoltarAoInicio,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.placar_voltar_ao_inicio))
        }
    }
}
