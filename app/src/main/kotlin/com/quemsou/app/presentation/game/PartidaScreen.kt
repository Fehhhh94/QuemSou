package com.quemsou.app.presentation.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.quemsou.app.R
import com.quemsou.app.presentation.ui.components.ConfirmDialog

/**
 * Tela única da partida: as fases do jogo são estados de [PartidaUiState],
 * trocadas com [AnimatedContent] — não há navegação entre elas.
 */
@Composable
fun PartidaScreen(
    onAbandonarPartida: () -> Unit,
    onVoltarAoInicio: () -> Unit,
    viewModel: PartidaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val feedbackDev by viewModel.feedbackDev.collectAsState()
    val abandonoSolicitado by viewModel.abandonoSolicitado.collectAsState()
    var confirmarQueimar by remember { mutableStateOf(false) }

    BackHandler { viewModel.abandonarPartida() }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            AnimatedContent(targetState = uiState, label = "partida-fase") { estado ->
                when (estado) {
                    is PartidaUiState.Carregando -> CarregandoContent()

                    is PartidaUiState.VezDeJogar -> VezDeJogarContent(
                        estado = estado,
                        onEstouComOCelular = viewModel::iniciarTurno,
                    )

                    is PartidaUiState.Grid -> GridContent(
                        estado = estado,
                        onRevelarDica = viewModel::revelarDica,
                    )

                    is PartidaUiState.Shot -> ShotContent(
                        estado = estado,
                        onBebi = viewModel::confirmarShot,
                    )

                    is PartidaUiState.DicaRevelada -> DicaReveladaContent(
                        estado = estado,
                        onAlguemAcertou = viewModel::abrirQuemAcertou,
                        onOutraDica = viewModel::outraDica,
                        onPedirQueimar = { confirmarQueimar = true },
                    )

                    is PartidaUiState.QuemAcertou -> QuemAcertouBackdrop(estado = estado, viewModel = viewModel)

                    is PartidaUiState.Anuncio -> AnuncioContent(
                        estado = estado,
                        feedbackDev = feedbackDev,
                        onVotar = viewModel::votarNoCard,
                        onComentar = viewModel::comentarFeedback,
                        onProximoTurno = viewModel::proximoTurno,
                    )

                    is PartidaUiState.PlacarFinal -> PlacarFinalContent(
                        estado = estado,
                        onJogarDeNovo = viewModel::reiniciarPartida,
                        onVoltarAoInicio = onVoltarAoInicio,
                    )
                }
            }
        }
    }

    if (confirmarQueimar) {
        ConfirmDialog(
            titulo = stringResource(R.string.partida_queimar_titulo),
            texto = stringResource(R.string.partida_queimar_corpo),
            textoConfirmar = stringResource(R.string.partida_queimar_confirmar),
            textoCancelar = stringResource(R.string.partida_queimar_cancelar),
            onConfirmar = {
                confirmarQueimar = false
                viewModel.queimarCard()
            },
            onCancelar = { confirmarQueimar = false },
        )
    }

    if (abandonoSolicitado) {
        ConfirmDialog(
            titulo = stringResource(R.string.partida_abandonar_titulo),
            texto = stringResource(R.string.partida_abandonar_corpo),
            textoConfirmar = stringResource(R.string.partida_abandonar_confirmar),
            textoCancelar = stringResource(R.string.partida_abandonar_cancelar),
            onConfirmar = onAbandonarPartida,
            onCancelar = viewModel::continuarPartida,
        )
    }
}

/**
 * Pano de fundo da fase QuemAcertou: a folha (ver [QuemAcertouSheet]) cobre a
 * tela; "voltar" apenas a fecha — o jogo continua na mesma fase até alguém
 * ser escolhido, e um botão local a reabre.
 */
@Composable
private fun QuemAcertouBackdrop(estado: PartidaUiState.QuemAcertou, viewModel: PartidaViewModel) {
    var mostrarFolha by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Crossfade(targetState = mostrarFolha, label = "quem-acertou-folha") { visivel ->
            if (!visivel) {
                OutlinedButton(onClick = { mostrarFolha = true }) {
                    Text(stringResource(R.string.partida_quem_acertou_titulo))
                }
            }
        }
    }

    if (mostrarFolha) {
        QuemAcertouSheet(
            estado = estado,
            onRegistrarAcerto = viewModel::registrarAcerto,
            onVoltar = { mostrarFolha = false },
        )
    }
}
