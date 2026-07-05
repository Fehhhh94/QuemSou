package com.quemsou.app.presentation.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Placeholder navegável da 3.2 — descartável na 3.3. Mostra a fase atual e um
 * botão mínimo por fase, só para o fluxo ser exercitado manualmente. O botão
 * voltar é interceptado e vira o evento de abandonar partida (a UI de
 * confirmação vem na 3.3).
 */
@Composable
fun PartidaScreen(viewModel: PartidaViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val abandonoSolicitado by viewModel.abandonoSolicitado.collectAsState()

    BackHandler { viewModel.abandonarPartida() }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Partida (placeholder 3.2) — fase: ${uiState::class.simpleName}")
            if (abandonoSolicitado) {
                Text("Abandonar partida? (confirmação vem na 3.3)")
                Button(onClick = viewModel::continuarPartida) { Text("Continuar jogando") }
            }
            when (val estado = uiState) {
                is PartidaUiState.Carregando -> Text("Carregando…")

                is PartidaUiState.VezDeJogar -> {
                    Text("Rodada ${estado.rodada}/${estado.totalDeRodadas} — leitor: ${estado.nomeDoLeitor}")
                    Button(onClick = viewModel::iniciarTurno) { Text("Iniciar turno") }
                }

                is PartidaUiState.Grid -> {
                    Text("Escolhe: ${estado.nomeDoEscolhedor} · em jogo: ${estado.pontosEmJogo} pts")
                    val posicaoLivre = (1..10).first { it !in estado.posicoesReveladas }
                    Button(onClick = { viewModel.revelarDica(posicaoLivre) }) {
                        Text("Revelar posição $posicaoLivre")
                    }
                }

                is PartidaUiState.DicaRevelada -> {
                    Text("Dica ${estado.posicao} (${estado.valor} pts): ${estado.texto}")
                    Button(onClick = viewModel::abrirQuemAcertou) { Text("Alguém acertou") }
                    Button(onClick = viewModel::outraDica) { Text("Outra dica") }
                    Button(onClick = viewModel::queimarCard) { Text("Desistir (queimar)") }
                }

                is PartidaUiState.QuemAcertou -> estado.adivinhadores.forEach { adivinhador ->
                    Button(onClick = { viewModel.registrarAcerto(adivinhador.id) }) {
                        Text(adivinhador.nome)
                    }
                }

                is PartidaUiState.Anuncio -> {
                    Text("Resposta: ${estado.resposta} (${estado.dicasUsadas} dicas)")
                    Button(onClick = viewModel::proximoTurno) { Text("Próximo turno") }
                }

                is PartidaUiState.PlacarFinal -> {
                    Text("Vencedor(es): ${estado.vencedores.joinToString()}${if (estado.empate) " (empate)" else ""}")
                    estado.ranking.forEach { linha -> Text("${linha.nome}: ${linha.pontos} pts") }
                }
            }
        }
    }
}
