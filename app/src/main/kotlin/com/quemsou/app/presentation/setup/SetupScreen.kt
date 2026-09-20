package com.quemsou.app.presentation.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
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
import com.quemsou.app.presentation.ui.components.CodigoQr
import com.quemsou.app.presentation.ui.components.ConfirmDialog
import com.quemsou.app.presentation.ui.theme.SeloVerde
import com.quemsou.app.presentation.ui.theme.SeloVerdeClaro
import com.quemsou.app.presentation.ui.theme.ShotAmbar

/** Tela de configuração da partida: baralhos, jogadores, grupos e regras. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onComecarPartida: (ConfiguracaoDaPartida) -> Unit,
    onAbrirCatalogo: () -> Unit,
    onVoltar: () -> Unit,
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

    var mostrarOpcoes by rememberSaveable { mutableStateOf(false) }
    // Edge-to-edge (`enableEdgeToEdge` na MainActivity) desliga o resize da
    // janela: sem `imePadding` o teclado do nome cobre a barra de ação e o
    // próprio campo em foco. O Scaffold inteiro sobe com o teclado.
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_title)) },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.party_back)) } },
            )
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BarraDeAcaoInferior(modifier = Modifier.widthIn(max = 640.dp)) {
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
                            .heightIn(min = 52.dp),
                    ) {
                        Text(stringResource(R.string.setup_comecar_partida))
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.party_setup_intro), style = MaterialTheme.typography.headlineLarge)
                        Text(stringResource(R.string.party_setup_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    SecaoJogarEmTimes(ativo = uiState.jogarEmTimes, onAlternar = viewModel::alternarJogarEmTimes)
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
                    SecaoRodadas(
                        rodadas = uiState.numeroDeRodadas,
                        quantidadeDeJogadores = uiState.jogadores.size,
                        onDefinir = viewModel::definirRodadas,
                    )
                }
                item {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                        SecaoBaralhos(
                            uiState = uiState,
                            onAlternarBaralho = viewModel::alternarBaralho,
                            onSelecionarTodos = viewModel::selecionarTodosBaralhos,
                            onAbrirCatalogo = onAbrirCatalogo,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                item {
                    BotaoDeOpcoes(
                        aberto = mostrarOpcoes,
                        opcoesAtivas = uiState.opcoesAtivas,
                        onAlternar = { mostrarOpcoes = !mostrarOpcoes },
                    )
                }
                if (mostrarOpcoes) {
                    item { SecaoLeitorPontua(ativo = uiState.leitorPontua, onAlternar = viewModel::alternarLeitorPontua) }
                    item {
                        SecaoModoShot(
                            ativo = uiState.modoShot,
                            quantidade = uiState.quantidadeDeShots,
                            onAlternar = viewModel::alternarModoShot,
                            onDefinirQuantidade = viewModel::definirQuantidadeDeShots,
                        )
                    }
                    item {
                        SecaoEspelho(uiState = uiState, onAlternar = viewModel::alternarEspelho, onTocarNaLinha = viewModel::tocarNaLinhaDoEspelho)
                    }
                }
            }
        }
    }

    uiState.espelhoLugarALiberar?.let { jogadorId ->
        val indice = uiState.jogadores.indexOfFirst { it.id == jogadorId }
        val nome = uiState.jogadores.getOrNull(indice)?.nome?.trim().orEmpty()
            .ifBlank { stringResource(R.string.setup_espelho_sem_nome, indice + 1) }
        ConfirmDialog(
            titulo = stringResource(R.string.setup_espelho_liberar_titulo, nome),
            texto = stringResource(R.string.setup_espelho_liberar_corpo),
            textoConfirmar = stringResource(R.string.setup_espelho_liberar_confirmar),
            textoCancelar = stringResource(R.string.setup_espelho_liberar_cancelar),
            onConfirmar = viewModel::confirmarLiberarLugar,
            onCancelar = viewModel::cancelarLiberarLugar,
        )
    }
}

/**
 * Seleção de baralhos da partida: lista dos baralhos BAIXADOS agrupada por
 * coleção, checkbox e mini-selo por baralho, atalhos "Selecionar todos" e
 * "Catálogo →", e o contador vivo da união.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun SecaoBaralhos(
    uiState: SetupUiState,
    onAlternarBaralho: (String) -> Unit,
    onSelecionarTodos: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(stringResource(R.string.setup_baralhos_titulo), style = MaterialTheme.typography.titleLarge)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!uiState.semRespostasNoAparelho) {
                    TextButton(onClick = onSelecionarTodos) {
                        Text(stringResource(R.string.setup_baralhos_selecionar_todos))
                    }
                }
                TextButton(onClick = onAbrirCatalogo) {
                    Text(stringResource(R.string.setup_baralhos_abrir_catalogo))
                }
            }
        }
        if (uiState.semRespostasNoAparelho) {
            // Não há o que selecionar: a seção diz o que aconteceu e para
            // onde ir, em vez de oferecer um "Selecionar todos" sobre nada.
            // Os baralhos esgotados continuam listados abaixo, com "0
            // respostas disponíveis" — some a oferta, não o acervo.
            Text(
                text = stringResource(R.string.setup_sem_respostas_titulo),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.setup_sem_respostas_corpo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        uiState.baralhosDisponiveis
            .groupBy { it.colecaoId }
            .forEach { (_, baralhosDaColecao) ->
                Text(
                    text = "${baralhosDaColecao.first().colecaoIcone} ${baralhosDaColecao.first().colecaoNome}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() },
                )
                if (baralhosDaColecao.first().especial) {
                    Text(
                        text = stringResource(R.string.setup_especiais_descricao),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                baralhosDaColecao.forEach { baralho ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).toggleable(
                            value = baralho.id in uiState.baralhosSelecionados,
                            role = Role.Checkbox,
                            onValueChange = { onAlternarBaralho(baralho.id) },
                        ).heightIn(min = 48.dp).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(checked = baralho.id in uiState.baralhosSelecionados, onCheckedChange = null)
                        Column(Modifier.weight(1f)) {
                            Text(baralho.nome, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(R.string.setup_baralho_cards, baralho.quantidadeDeCards),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        if (!uiState.semRespostasNoAparelho) {
            Text(
                text = if (!uiState.baralhosCarregados) {
                    stringResource(R.string.setup_carregando_respostas)
                } else {
                    // Plural de verdade nas duas metades: "1 baralho · 1
                    // resposta disponível" em vez de "1 baralhos · 1 respostas".
                    stringResource(
                        R.string.setup_baralhos_contador_composto,
                        pluralStringResource(
                            R.plurals.setup_baralhos_selecionados,
                            uiState.baralhosSelecionados.size,
                            uiState.baralhosSelecionados.size,
                        ),
                        pluralStringResource(
                            R.plurals.setup_respostas_disponiveis,
                            uiState.cardsNoMonte,
                            uiState.cardsNoMonte,
                        ),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.setup_variedade),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Espelho de leitura: o anfitrião liga o servidor local, mostra
 * o QR e o endereço, e acompanha quem já entrou pelo navegador.
 *
 * Acesso em Mais opções, depois dos jogadores e baralhos. Começar a partida
 * não depende do espelho: ninguém conectado é um estado válido.
 *
 * Cada linha da lista é tocável e o toque significa uma coisa diferente
 * conforme o estado ([SetupUiState.estadoNoEspelho]); a dica de uso acima da
 * lista existe porque um alvo de toque sem convite não é descoberto.
 */
@Composable
private fun SecaoEspelho(
    uiState: SetupUiState,
    onAlternar: () -> Unit,
    onTocarNaLinha: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.setup_espelho_titulo), style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = uiState.espelhoLigado,
                onCheckedChange = { onAlternar() },
                enabled = !uiState.espelhoIniciando,
            )
        }
        Text(
            text = stringResource(R.string.setup_espelho_descricao),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        uiState.espelhoFalha?.let { falha ->
            Text(
                text = stringResource(
                    when (falha) {
                        FalhaDoEspelho.SEM_REDE -> R.string.setup_espelho_sem_rede
                        FalhaDoEspelho.SEM_PORTA -> R.string.setup_espelho_sem_porta
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        val endereco = uiState.espelhoEndereco
        if (uiState.espelhoLigado && endereco != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CodigoQr(
                    conteudo = endereco,
                    contentDescription = stringResource(R.string.setup_espelho_qr_cd),
                )
                Text(
                    text = endereco,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Text(
                text = stringResource(R.string.setup_espelho_dica_toque),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // O ✓ verde confirma quem entrou pela rede. "Este aparelho" fica
            // em onSurface, com contraste cheio e sem cor própria.
            val verdeDoEntrou = if (isSystemInDarkTheme()) SeloVerdeClaro else SeloVerde
            uiState.jogadores.forEachIndexed { indice, jogador ->
                val estado = uiState.estadoNoEspelho(jogador.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTocarNaLinha(jogador.id) }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = jogador.nome.trim().ifBlank {
                            stringResource(R.string.setup_espelho_sem_nome, indice + 1)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(
                            when (estado) {
                                EstadoNoEspelho.ESTE_APARELHO -> R.string.setup_espelho_este_aparelho
                                EstadoNoEspelho.ENTROU -> R.string.setup_espelho_entrou
                                EstadoNoEspelho.AGUARDANDO -> R.string.setup_espelho_aguardando
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (estado) {
                            EstadoNoEspelho.ESTE_APARELHO -> MaterialTheme.colorScheme.onSurface
                            EstadoNoEspelho.ENTROU -> verdeDoEntrou
                            EstadoNoEspelho.AGUARDANDO -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Text(
                text = stringResource(R.string.setup_espelho_nota),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
                    .heightIn(min = 56.dp)
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
            val nome = jogador.nome.trim().ifBlank { stringResource(R.string.setup_jogador_nome_placeholder, indice + 1) }
            val descricao = stringResource(R.string.setup_grupo_toque, nome)
            FilterChip(
                selected = jogador.grupo != null,
                onClick = onCiclarGrupo,
                label = {
                    Text(
                        jogador.grupo?.let { stringResource(R.string.setup_grupo_n, it) }
                            ?: stringResource(R.string.setup_sem_grupo),
                    )
                },
                // O ciclo "sem grupo → 1 → 2 → 3" não se explica sozinho:
                // a semântica diz de quem é o grupo que o toque muda.
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = descricao },
            )
        }
    }
}

/**
 * Agrupar jogadores é a única decisão do Setup cujo efeito não é óbvio pelo
 * rótulo: a descrição existe para o convidado entender o que muda no placar
 * antes de ligar o switch.
 */
@Composable
private fun SecaoJogarEmTimes(ativo: Boolean, onAlternar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.setup_jogar_em_times_titulo), style = MaterialTheme.typography.titleMedium)
            Switch(checked = ativo, onCheckedChange = { onAlternar() })
        }
        Text(
            text = stringResource(R.string.setup_jogar_em_times_descricao),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Entrada de "Mais opções". Recolhido, resume as regras secundárias que já
 * saíram do padrão ([OpcaoAtiva]) — recolher esconde os controles, nunca o
 * fato de o Modo Shot, o espelho ou o leitor sem pontos estarem valendo.
 */
@Composable
private fun BotaoDeOpcoes(
    aberto: Boolean,
    opcoesAtivas: List<OpcaoAtiva>,
    onAlternar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(onClick = onAlternar, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(if (aberto) R.string.party_setup_close_options else R.string.party_setup_options))
        }
        if (!aberto && opcoesAtivas.isNotEmpty()) {
            // `stringResource` não pode ser chamado dentro do lambda do
            // joinToString: os rótulos são resolvidos antes e só depois unidos.
            val rotulos = opcoesAtivas.map { opcao ->
                stringResource(
                    when (opcao) {
                        OpcaoAtiva.LEITOR_NAO_PONTUA -> R.string.setup_opcao_leitor_nao_pontua
                        OpcaoAtiva.MODO_SHOT -> R.string.setup_opcao_modo_shot
                        OpcaoAtiva.ESPELHO -> R.string.setup_opcao_espelho
                    },
                )
            }
            val nomes = rotulos.joinToString(" · ")
            Text(
                text = stringResource(R.string.setup_opcoes_ativas, nomes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SecaoRodadas(rodadas: Int, quantidadeDeJogadores: Int, onDefinir: (Int) -> Unit) {
    val rodadasPorJogador = rodadas / quantidadeDeJogadores
    val diminuirDescricao = stringResource(R.string.setup_rodadas_diminuir_cd)
    val aumentarDescricao = stringResource(R.string.setup_rodadas_aumentar_cd)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.setup_rodadas_titulo), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledIconButton(
                onClick = { onDefinir(rodadas - quantidadeDeJogadores) },
                enabled = rodadas > quantidadeDeJogadores,
                modifier = Modifier.size(48.dp).semantics { contentDescription = diminuirDescricao },
            ) {
                Text("−")
            }
            Text(
                text = "$rodadas",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center,
            )
            FilledIconButton(
                onClick = { onDefinir(rodadas + quantidadeDeJogadores) },
                modifier = Modifier.size(48.dp).semantics { contentDescription = aumentarDescricao },
            ) {
                Text("+")
            }
        }
        Text(
            text = pluralStringResource(
                R.plurals.setup_rodadas_por_jogador,
                rodadasPorJogador,
                rodadasPorJogador,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        MotivoDoBloqueio.SEM_RESPOSTAS_NO_APARELHO -> R.string.setup_bloqueio_sem_respostas
        MotivoDoBloqueio.NENHUM_BARALHO -> R.string.setup_bloqueio_nenhum_baralho
        MotivoDoBloqueio.SELECAO_SEM_RESPOSTAS -> R.string.setup_bloqueio_selecao_sem_respostas
        MotivoDoBloqueio.CARDS_INSUFICIENTES -> R.string.setup_bloqueio_cards_insuficientes
    },
)
