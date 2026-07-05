package com.quemsou.app.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quemsou.app.navigation.ConfiguracaoDaPartida

/**
 * Placeholder navegável da 3.2 — descartável na 3.3. Só o suficiente para
 * exercitar o [SetupViewModel] e navegar até a rota Partida.
 */
@Composable
fun SetupScreen(
    onComecarPartida: (ConfiguracaoDaPartida) -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuracaoPronta by viewModel.configuracaoPronta.collectAsState()

    LaunchedEffect(configuracaoPronta) {
        configuracaoPronta?.let { configuracao ->
            onComecarPartida(configuracao)
            viewModel.consumirConfiguracaoPronta()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Setup (placeholder 3.2)")
            Text("Jogadores: ${uiState.jogadores.size} · Rodadas: ${uiState.numeroDeRodadas}")
            Text("Bloqueio: ${uiState.motivoDoBloqueio ?: "nenhum"}")
            Button(
                onClick = {
                    uiState.jogadores.forEachIndexed { indice, _ ->
                        viewModel.renomearJogador(indice, "Jogador ${indice + 1}")
                    }
                },
            ) {
                Text("Preencher nomes de teste")
            }
            Button(onClick = viewModel::confirmar, enabled = uiState.podeComecar) {
                Text("Começar partida")
            }
        }
    }
}
