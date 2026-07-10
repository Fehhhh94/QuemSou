package com.quemsou.app.presentation.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quemsou.app.R
import com.quemsou.app.domain.model.Partida
import com.quemsou.app.domain.model.RegrasPartida
import com.quemsou.app.navigation.ConfiguracaoDaPartida
import com.quemsou.app.presentation.ui.components.BarraDeAcaoInferior
import com.quemsou.app.presentation.ui.components.SeloDeEstado
import com.quemsou.app.presentation.ui.theme.ShotAmbar

/** Tela de configuração da partida: baralhos, jogadores, grupos e regras. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onComecarPartida: (ConfiguracaoDaPartida) -> Unit,
    onAbrirCatalogo: () -> Unit,
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

    // Ao voltar do catálogo (ON_RESUME), a lista de baralhos pode ter mudado.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) viewModel.recarregarBaralhos()
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.setup_title)) }) },
        bottomBar = {
            BarraDeAcaoInferior {
                uiState.motivoDoBloqueioVisivel?.let { motivo ->
                    Text(
                        text = textoDoBloqueio(motivo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Button(
                    onClick = viewModel::confirmar,
                    enabled = uiState.podeComecar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(stringResource(R.string.setup_comecar_partida))
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                SecaoBaralhos(
                    uiState = uiState,
                    onAlternarBaralho = viewModel::alternarBaralho,
                    onSelecionarTodos = viewModel::selecionarTodosBaralhos,
                    onAbrirCatalogo = onAbrirCatalogo,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            item {
                SecaoJogarEmTimes(ativo = uiState.jogarEmTimes, onAlternar = viewModel::alternarJogarEmTimes)
            }
            item {
                Text(stringResource(R.string.setup_jogadores_titulo), style = MaterialTheme.typography.titleMedium)
            }
            itemsIndexed(uiState.jogadores) { indice, jogador ->
                LinhaDeJogador(
                    indice = indice,
                    jogador = jogador,
                    jogarEmTimes = uiState.jogarEmTimes,
                    podeRemover = uiState.jogadores.size > Partida.MINIMO_DE_JOGADORES,
                    onNomeAlterado = { viewModel.renomearJogador(indice, it) },
                    onNomeCampoPerdeuFoco = { viewModel.marcarJogadorTocado(indice) },
                    onCiclarGrupo = { viewModel.ciclarGrupo(indice) },
                    onRemover = { viewModel.removerJogador(indice) },
                )
            }
            if (uiState.jogadores.size < Partida.MAXIMO_DE_JOGADORES) {
                item {
                    OutlinedButton(onClick = viewModel::adicionarJogador, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(
                            text = stringResource(R.string.setup_adicionar_jogador),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            item {
                SecaoRodadas(rodadas = uiState.numeroDeRodadas, onDefinir = viewModel::definirRodadas)
            }
            item {
                SecaoLeitorPontua(ativo = uiState.leitorPontua, onAlternar = viewModel::alternarLeitorPontua)
            }
            item {
                SecaoModoShot(
                    ativo = uiState.modoShot,
                    quantidade = uiState.quantidadeDeShots,
                    onAlternar = viewModel::alternarModoShot,
                    onDefinirQuantidade = viewModel::definirQuantidadeDeShots,
                )
            }
        }
    }
}

/**
 * Seleção de baralhos da partida: lista dos baralhos BAIXADOS agrupada por
 * coleção, checkbox e mini-selo por baralho, atalhos "Selecionar todos" e
 * "Catálogo →", e o contador vivo da união.
 */
@Composable
private fun SecaoBaralhos(
    uiState: SetupUiState,
    onAlternarBaralho: (String) -> Unit,
    onSelecionarTodos: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.setup_baralhos_titulo), style = MaterialTheme.typography.titleMedium)
            Row {
                TextButton(onClick = onSelecionarTodos) {
                    Text(stringResource(R.string.setup_baralhos_selecionar_todos))
                }
                TextButton(onClick = onAbrirCatalogo) {
                    Text(stringResource(R.string.setup_baralhos_abrir_catalogo))
                }
            }
        }
        uiState.baralhosDisponiveis
            .groupBy { it.colecaoId }
            .forEach { (_, baralhosDaColecao) ->
                Text(
                    text = "${baralhosDaColecao.first().colecaoIcone} ${baralhosDaColecao.first().colecaoNome}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                baralhosDaColecao.forEach { baralho ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = baralho.id in uiState.baralhosSelecionados,
                            onCheckedChange = { onAlternarBaralho(baralho.id) },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(baralho.nome, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.setup_baralho_cards, baralho.quantidadeDeCards),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SeloDeEstado(estado = baralho.estado, compacto = true)
                    }
                }
            }
        Text(
            text = stringResource(
                R.string.setup_baralhos_contador,
                uiState.baralhosSelecionados.size,
                uiState.cardsNoMonte,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LinhaDeJogador(
    indice: Int,
    jogador: JogadorEmEdicao,
    jogarEmTimes: Boolean,
    podeRemover: Boolean,
    onNomeAlterado: (String) -> Unit,
    onNomeCampoPerdeuFoco: () -> Unit,
    onCiclarGrupo: () -> Unit,
    onRemover: () -> Unit,
) {
    // `onFocusChanged` dispara uma vez na composição inicial com foco=false;
    // sem essa guarda, todo campo nasceria "tocado" e o Bug 1 voltaria.
    var campoRecebeuFoco by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = jogador.nome,
                onValueChange = onNomeAlterado,
                label = { Text(stringResource(R.string.setup_jogador_nome_placeholder, indice + 1)) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .onFocusChanged { estadoDoFoco ->
                        if (estadoDoFoco.isFocused) {
                            campoRecebeuFoco = true
                        } else if (campoRecebeuFoco) {
                            onNomeCampoPerdeuFoco()
                        }
                    },
            )
            if (podeRemover) {
                IconButton(onClick = onRemover, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.setup_jogador_remover_cd))
                }
            }
        }
        if (jogarEmTimes) {
            FilterChip(
                selected = jogador.grupo != null,
                onClick = onCiclarGrupo,
                label = {
                    Text(
                        jogador.grupo?.let { stringResource(R.string.setup_grupo_n, it) }
                            ?: stringResource(R.string.setup_sem_grupo),
                    )
                },
            )
        }
    }
}

@Composable
private fun SecaoJogarEmTimes(ativo: Boolean, onAlternar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.setup_jogar_em_times_titulo), style = MaterialTheme.typography.titleMedium)
        Switch(checked = ativo, onCheckedChange = { onAlternar() })
    }
}

@Composable
private fun SecaoRodadas(rodadas: Int, onDefinir: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.setup_rodadas_titulo), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledIconButton(
                onClick = { onDefinir(rodadas - 1) },
                enabled = rodadas > 1,
                modifier = Modifier.size(48.dp),
            ) {
                Text("−")
            }
            Text(
                text = "$rodadas",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center,
            )
            FilledIconButton(onClick = { onDefinir(rodadas + 1) }, modifier = Modifier.size(48.dp)) {
                Text("+")
            }
        }
    }
}

@Composable
private fun SecaoLeitorPontua(ativo: Boolean, onAlternar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.setup_leitor_pontua_titulo), style = MaterialTheme.typography.titleMedium)
        Switch(checked = ativo, onCheckedChange = { onAlternar() })
    }
}

/**
 * Card do Modo Shot — a única regra 18+ da configuração, marcada pela borda
 * âmbar sutil (paleta exclusiva do modo; nada de âmbar no resto do Setup).
 * O stepper de quantidade (1–3) só aparece com o toggle ligado.
 */
@Composable
private fun SecaoModoShot(
    ativo: Boolean,
    quantidade: Int,
    onAlternar: () -> Unit,
    onDefinirQuantidade: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ShotAmbar.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.setup_modo_shot_titulo), style = MaterialTheme.typography.titleMedium)
                Switch(checked = ativo, onCheckedChange = { onAlternar() })
            }
            Text(
                text = stringResource(R.string.setup_modo_shot_descricao),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (ativo) {
                Text(
                    text = stringResource(R.string.setup_modo_shot_quantidade_titulo),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledIconButton(
                        onClick = { onDefinirQuantidade(quantidade - 1) },
                        enabled = quantidade > RegrasPartida.MINIMO_DE_SHOTS,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Text("−")
                    }
                    Text(
                        text = "$quantidade",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center,
                    )
                    FilledIconButton(
                        onClick = { onDefinirQuantidade(quantidade + 1) },
                        enabled = quantidade < RegrasPartida.MAXIMO_DE_SHOTS,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Text("+")
                    }
                }
            }
            Text(
                text = stringResource(R.string.setup_modo_shot_nota),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun textoDoBloqueio(motivo: MotivoDoBloqueio): String = stringResource(
    when (motivo) {
        MotivoDoBloqueio.POUCOS_JOGADORES -> R.string.setup_bloqueio_poucos_jogadores
        MotivoDoBloqueio.NOMES_VAZIOS -> R.string.setup_bloqueio_nomes_vazios
        MotivoDoBloqueio.NENHUM_BARALHO -> R.string.setup_bloqueio_nenhum_baralho
        MotivoDoBloqueio.CARDS_INSUFICIENTES -> R.string.setup_bloqueio_cards_insuficientes
    },
)
