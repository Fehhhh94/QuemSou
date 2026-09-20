package com.quemsou.app.presentation.game

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
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

/**
 * Espera da partida. O checkpoint é gravado **antes** de exibir cada dica, e
 * essa gravação passa por aqui a cada toque do grid: mostrar o indicador na
 * hora transformava uma transição de milissegundos num pisca-pisca. O
 * indicador só entra depois de [ESPERA_ATE_O_INDICADOR] — a persistência não
 * muda, só o que se vê enquanto ela acontece.
 */
@Composable
internal fun CarregandoContent() {
    var visivel by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(ESPERA_ATE_O_INDICADOR)
        visivel = true
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (visivel) CircularProgressIndicator()
    }
}

private const val ESPERA_ATE_O_INDICADOR = 250L

/** Acima disso, a dica passa a ser lida como parágrafo, não como manchete. */
private const val LIMITE_DA_DICA_CURTA = 90

/** Teto de largura do grid; acima disso as cartas ficam altas demais. */
private val LARGURA_MAXIMA_DO_GRID = 420.dp

/**
 * Abaixo desta altura de janela a tela da dica se reorganiza para preservar a
 * área de leitura (paisagem de celular, retrato com fonte muito ampliada).
 */
private val ALTURA_PARA_LEITURA_FOLGADA = 560.dp

/**
 * Partida que não pôde ser preparada — quase sempre conteúdo insuficiente:
 * faltam respostas distintas com dez dicas disponíveis para as rodadas
 * pedidas. Estado de tela inteiro (com respiro, rolagem e saída), não um
 * texto solto: é aqui que o jogador descobre que precisa reduzir rodadas ou
 * escolher mais baralhos.
 */
@Composable
internal fun IndisponivelContent(
    onTentarNovamente: () -> Unit,
    onVoltarAoSetup: () -> Unit,
    onVoltarAoInicio: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.partida_indisponivel_titulo),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.dicas_partida_indisponivel),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onVoltarAoSetup,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.partida_indisponivel_voltar_ao_setup))
        }
        OutlinedButton(
            onClick = onTentarNovamente,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.partida_tentar_novamente))
        }
        TextButton(
            onClick = onVoltarAoInicio,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.dicas_voltar))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VezDeJogarContent(
    estado: PartidaUiState.VezDeJogar,
    onEstouComOCelular: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
        Text(
            text = stringResource(R.string.partida_vez_adivinham),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.nomesDosAdivinhadores.forEach { nome -> ChipDeJogador(nome = nome) }
        }
        Button(
            onClick = onEstouComOCelular,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.partida_vez_estou_com_celular))
        }
    }
}

/**
 * Grid da rodada. O conteúdo de contexto fica em cima e os dez números
 * descem para junto do rodapé: num celular grande, o grid encostado no topo
 * deixava a área de toque principal fora do alcance do polegar de quem está
 * com o aparelho na mão. `heightIn(min = maxHeight)` + `SpaceBetween` só
 * distribui a sobra — quando o conteúdo passa da tela (fonte ampliada,
 * paisagem), a rolagem volta a valer e nada é empurrado para fora.
 */
@Composable
internal fun GridContent(
    estado: PartidaUiState.Grid,
    onRevelarDica: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f)) {
        val alturaDaTela = this@BoxWithConstraints.maxHeight
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = alturaDaTela)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.partida_grid_rodada_leitor, estado.rodada, estado.nomeDoLeitor),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
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
          }
          // A instrução desce junto com o grid: é o rótulo dele, e ficar
          // colada ao bloco de cima deixaria uma frase solta no meio da tela.
          Column(
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.padding(top = 16.dp),
          ) {
              Text(
                  text = stringResource(R.string.party_grid_instruction),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              GradeDeNumeros(
                  posicoesReveladas = estado.posicoesReveladas,
                  onTocar = onRevelarDica,
                  modifier = Modifier.align(Alignment.CenterHorizontally),
              )
          }
        }
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

/**
 * A resposta só aparece enquanto pressionada — nunca fica fixa na tela.
 *
 * O alvo recebe um rótulo fixo: era uma área tocável sem nome nenhum, e o
 * rótulo não pode conter a resposta (é justamente o que se está escondendo).
 */
@Composable
private fun AreaDaResposta(resposta: String) {
    var pressionada by remember { mutableStateOf(false) }
    val rotulo = stringResource(R.string.partida_grid_area_da_resposta)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .semantics { contentDescription = rotulo }
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
        Box(Modifier.padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
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

/**
 * As dez posições. A largura tem teto: sem ele, numa tela larga e baixa
 * (paisagem, Fold aberto, tablet) o `aspectRatio` esticava cada carta até
 * o grid não caber mais em pé. O teto não muda nada no retrato de um celular
 * comum, que já é mais estreito que ele.
 */
@Composable
private fun GradeDeNumeros(
    posicoesReveladas: List<Int>,
    onTocar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.widthIn(max = LARGURA_MAXIMA_DO_GRID), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..10).chunked(5).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                linha.forEach { posicao ->
                    val revelada = posicao in posicoesReveladas
                    // O número sozinho não diz o estado da posição: a
                    // descrição carrega "disponível"/"já revelada". A posição
                    // continua sem qualquer pista de dificuldade — nem visual,
                    // nem semântica.
                    val descricao = stringResource(
                        if (revelada) R.string.partida_grid_posicao_revelada else R.string.partida_grid_posicao_disponivel,
                        posicao,
                    )
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
                            .aspectRatio(.72f)
                            .heightIn(min = 64.dp)
                            .semantics {
                                contentDescription = descricao
                                if (revelada) disabled()
                            },
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (revelada) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            } else {
                                Text(
                                    text = "$posicao",
                                    style = MaterialTheme.typography.headlineMedium,
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
                            .heightIn(min = 52.dp),
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
    feedback: @Composable () -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Três ações empilhadas com as margens do retrato consumiam quase toda a
        // altura em paisagem: sobrava uma faixa de uma linha para a dica, que
        // é o único conteúdo que alguém precisa ler em voz alta. Abaixo deste
        // limiar a tela se reorganiza — ações lado a lado, margens menores e
        // corpo proporcional à janela — em vez de espremer a leitura.
        val janelaBaixa = this@BoxWithConstraints.maxHeight < ALTURA_PARA_LEITURA_FOLGADA
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = if (janelaBaixa) 12.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (janelaBaixa) 8.dp else 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.partida_dica_titulo, estado.posicao, estado.valor),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                ChipTipoDeCard(tipo = estado.tipo)
            }
            CorpoDaDica(
                texto = estado.texto,
                janelaBaixa = janelaBaixa,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                feedback = feedback,
            )
            AcoesDaDica(
                janelaBaixa = janelaBaixa,
                onAlguemAcertou = onAlguemAcertou,
                onOutraDica = onOutraDica,
                onPedirQueimar = onPedirQueimar,
            )
        }
    }
}

/**
 * Superfície de leitura da dica. O corpo responde à **altura da janela** e ao
 * comprimento do texto: numa janela baixa, manchete de 28 sp com a fonte do
 * sistema a 150% não cabe nem em uma linha, e insistir nela troca
 * legibilidade por corte. A rolagem interna segue como rede de segurança da
 * dica muito longa, nunca como resposta padrão.
 */
@Composable
private fun CorpoDaDica(
    texto: String,
    janelaBaixa: Boolean,
    modifier: Modifier = Modifier,
    feedback: @Composable () -> Unit = {},
) {
    val curta = texto.length <= LIMITE_DA_DICA_CURTA
    val estilo = when {
        !janelaBaixa && curta -> MaterialTheme.typography.headlineMedium
        !janelaBaixa -> MaterialTheme.typography.headlineSmall
        curta -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.bodyLarge
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = if (janelaBaixa) 8.dp else 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = texto, style = estilo, textAlign = TextAlign.Center)
            feedback()
        }
    }
}

/**
 * Acerto, outra dica e queima. Em janela baixa as duas ações principais vão
 * lado a lado: empilhadas, elas sozinhas ocupavam mais da metade da tela em
 * paisagem. Alvo mínimo de 48 dp e ordem de importância não mudam.
 */
@Composable
private fun AcoesDaDica(
    janelaBaixa: Boolean,
    onAlguemAcertou: () -> Unit,
    onOutraDica: () -> Unit,
    onPedirQueimar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (janelaBaixa) 4.dp else 16.dp)) {
        if (janelaBaixa) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onOutraDica,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.partida_dica_outra_dica))
                }
                Button(
                    onClick = onAlguemAcertou,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.partida_dica_alguem_acertou))
                }
            }
        } else {
            Button(
                onClick = onAlguemAcertou,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.partida_dica_alguem_acertou))
            }
            OutlinedButton(
                onClick = onOutraDica,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.partida_dica_outra_dica))
            }
        }
        TextButton(
            onClick = onPedirQueimar,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
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
                        .heightIn(min = 52.dp),
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
@OptIn(ExperimentalLayoutApi::class)
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                .heightIn(min = 52.dp),
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
    // Com quatro grupos, fonte a 150% ou tela em paisagem, o ranking e os
    // dois botões não cabem: a tela inteira rola em vez de cortar a última
    // linha ou deixar "Voltar ao início" fora do alcance.
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val alturaDaTela = this@BoxWithConstraints.maxHeight
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .heightIn(min = alturaDaTela)
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
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
            modifier = Modifier.fillMaxWidth(),
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
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.placar_jogar_de_novo))
        }
        OutlinedButton(
            onClick = onVoltarAoInicio,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.placar_voltar_ao_inicio))
        }
    }
    }
}
