package com.quemsou.app.presentation.catalogo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quemsou.app.R
import com.quemsou.app.presentation.ui.components.BannerOffline
import com.quemsou.app.presentation.ui.components.SeloDeEstado
import com.quemsou.app.presentation.ui.theme.NovidadeAmbar
import com.quemsou.app.presentation.ui.theme.NovidadeAmbarEscuro

/**
 * Nível 2 do catálogo: os baralhos de uma coleção, cada card com selo de
 * estado, meta e o botão de 4 estados (Baixar / Atualizar / Baixado /
 * Cancelar + progresso). Os cards de um baralho **nunca são listados** —
 * respostas são surpresa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColecaoScreen(
    onVoltar: () -> Unit,
    viewModel: ColecaoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val pronto = uiState as? ColecaoUiState.Pronto
                    Text(pronto?.let { "${it.icone} ${it.nome}" } ?: "")
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.catalogo_voltar_cd),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val estado = uiState) {
            is ColecaoUiState.Carregando -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is ColecaoUiState.Indisponivel -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.catalogo_indisponivel),
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(onClick = viewModel::recarregar) {
                    Text(stringResource(R.string.catalogo_tentar_de_novo))
                }
            }

            is ColecaoUiState.Pronto -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (estado.offline) {
                    item { BannerOffline(Modifier.padding(top = 8.dp)) }
                }
                item {
                    Text(
                        text = stringResource(
                            R.string.catalogo_colecao_meta,
                            estado.baralhos.size,
                            estado.baralhos.sumOf { it.entrada.quantidadeDeCards },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(estado.baralhos, key = { it.entrada.id }) { baralho ->
                    CardDeBaralho(
                        baralho = baralho,
                        offline = estado.offline,
                        onBaixar = { viewModel.baixar(baralho.entrada) },
                        onCancelar = { viewModel.cancelarDownload(baralho.entrada.id) },
                    )
                }
            }
        }
    }

    (uiState as? ColecaoUiState.Pronto)?.erro?.let { mensagem ->
        AlertDialog(
            onDismissRequest = viewModel::limparErro,
            title = { Text(stringResource(R.string.catalogo_erro_titulo)) },
            text = { Text(mensagem) },
            confirmButton = {
                TextButton(onClick = viewModel::limparErro) {
                    Text(stringResource(R.string.catalogo_erro_ok))
                }
            },
        )
    }
}

@Composable
private fun CardDeBaralho(
    baralho: BaralhoDaColecaoUi,
    offline: Boolean,
    onBaixar: () -> Unit,
    onCancelar: () -> Unit,
) {
    val entrada = baralho.entrada
    val esmaecido = offline && baralho.estado is EstadoDoBaralhoUi.NaoBaixado
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (esmaecido) 0.5f else 1f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entrada.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                SeloDeEstado(estado = entrada.estado)
            }
            if (entrada.descricao.isNotBlank()) {
                Text(text = entrada.descricao, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = if (entrada.tamanhoEmBytes > 0) {
                    stringResource(
                        R.string.catalogo_baralho_meta_tamanho,
                        entrada.quantidadeDeCards,
                        entrada.versao,
                        formatarTamanho(entrada.tamanhoEmBytes),
                    )
                } else {
                    stringResource(R.string.catalogo_baralho_meta, entrada.quantidadeDeCards, entrada.versao)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (esmaecido) {
                Text(
                    text = stringResource(R.string.catalogo_requer_conexao),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                BotaoDeQuatroEstados(estado = baralho.estado, onBaixar = onBaixar, onCancelar = onCancelar)
            }
        }
    }
}

/** Botão do mockup v2: Baixar cheio · Atualizar contorno âmbar · Baixado contorno neutro · Cancelar + progresso. */
@Composable
private fun BotaoDeQuatroEstados(
    estado: EstadoDoBaralhoUi,
    onBaixar: () -> Unit,
    onCancelar: () -> Unit,
) {
    when (estado) {
        is EstadoDoBaralhoUi.NaoBaixado -> Button(onClick = onBaixar, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.catalogo_baixar))
        }

        is EstadoDoBaralhoUi.AtualizacaoDisponivel -> {
            val ambar = if (isSystemInDarkTheme()) NovidadeAmbar else NovidadeAmbarEscuro
            OutlinedButton(
                onClick = onBaixar,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ambar),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.catalogo_atualizar))
            }
        }

        is EstadoDoBaralhoUi.Baixado -> OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.catalogo_baixado))
        }

        is EstadoDoBaralhoUi.Baixando -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(
                progress = { estado.progresso },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.catalogo_cancelar))
            }
        }
    }
}

/** Formata bytes para exibição curta (ex.: "12 KB"). */
private fun formatarTamanho(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
