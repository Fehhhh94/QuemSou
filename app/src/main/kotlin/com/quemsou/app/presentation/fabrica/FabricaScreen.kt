package com.quemsou.app.presentation.fabrica

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.quemsou.app.R
import kotlinx.coroutines.delay

/** Pedido, acompanhamento e reposição no mesmo lugar, sem revelar respostas da biblioteca. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FabricaScreen(onVoltar: () -> Unit, onJogar: () -> Unit, vm: FabricaViewModel = hiltViewModel()) {
    val estado by vm.uiState.collectAsState()
    var tema by rememberSaveable { mutableStateOf("") }
    var orientacoes by rememberSaveable { mutableStateOf("") }
    var quantidade by rememberSaveable { mutableIntStateOf(10) }
    var incluirFeedback by rememberSaveable { mutableStateOf(true) }
    var codigo by remember { mutableStateOf("") }
    var configurando by rememberSaveable { mutableStateOf(false) }
    var confirmarDescarte by rememberSaveable { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { vm.atualizar(); delay(15_000) }
        }
    }
    if (confirmarDescarte) {
        AlertDialog(onDismissRequest = { confirmarDescarte = false },
            title = { Text(stringResource(R.string.fabrica_descartar)) },
            text = { Text(stringResource(R.string.fabrica_descartar_explicacao)) },
            confirmButton = { TextButton(onClick = { confirmarDescarte = false; vm.descartarReenvio() }) {
                Text(stringResource(R.string.fabrica_descartar))
            } },
            dismissButton = { TextButton(onClick = { confirmarDescarte = false }) { Text(stringResource(R.string.fabrica_manter)) } })
    }
    Scaffold(modifier = Modifier.imePadding(), topBar = { TopAppBar(title = { Text(stringResource(R.string.fabrica_titulo)) },
        navigationIcon = { TextButton(onClick = onVoltar) { Text(stringResource(R.string.fabrica_voltar)) } },
        actions = { TextButton(onClick = { configurando = !configurando }) { Text(stringResource(R.string.fabrica_conexao)) } }) }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.fabrica_intro), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.fabrica_computador), style = MaterialTheme.typography.bodyMedium)
                estado.pendente?.let { pendente ->
                    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.fabrica_reenvio_pendente, pendente.tema))
                        Button(onClick = vm::reenviar, enabled = !estado.ocupado) { Text(stringResource(R.string.fabrica_reenviar)) }
                        TextButton(onClick = { confirmarDescarte = true }, enabled = !estado.ocupado) { Text(stringResource(R.string.fabrica_descartar)) }
                    } }
                }
                if (!estado.conectado || configurando) {
                    OutlinedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.fabrica_conectar_descricao))
                        OutlinedTextField(value = codigo, onValueChange = { codigo = it.take(2000) },
                            label = { Text(stringResource(R.string.fabrica_codigo)) },
                            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = { vm.conectar(codigo); codigo = "" }, enabled = codigo.isNotBlank() && !estado.ocupado) {
                            Text(stringResource(R.string.fabrica_conectar))
                        }
                    } }
                }
                OutlinedTextField(value = tema, onValueChange = { tema = it.take(120) },
                    label = { Text(stringResource(R.string.fabrica_tema)) },
                    placeholder = { Text(stringResource(R.string.fabrica_tema_exemplo)) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.fabrica_tamanho), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30).forEach { n -> FilterChip(selected = quantidade == n, onClick = { quantidade = n },
                        label = { Text(stringResource(R.string.fabrica_cartas, n)) }) }
                }
                OutlinedTextField(value = orientacoes, onValueChange = { orientacoes = it.take(1500) },
                    label = { Text(stringResource(R.string.fabrica_preferencias)) }, modifier = Modifier.fillMaxWidth(), minLines = 3,
                    supportingText = { Text(stringResource(R.string.fabrica_preferencias_exemplo)) })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = incluirFeedback, onCheckedChange = { incluirFeedback = it })
                    Text(stringResource(R.string.fabrica_usar_feedback), Modifier.weight(1f))
                }
                Button(onClick = { vm.pedir(tema, quantidade, orientacoes, incluirFeedback) },
                    enabled = estado.conectado && !estado.ocupado && estado.pendente == null && tema.trim().length >= 3,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.fabrica_criar)) }
                if (estado.ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
                estado.aviso?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
                Text(stringResource(R.string.fabrica_pedidos), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = vm::atualizar, enabled = estado.conectado && !estado.ocupado) { Text(stringResource(R.string.fabrica_atualizar)) }
                if (estado.pedidos.isEmpty()) Text(stringResource(R.string.fabrica_vazio))
                estado.pedidos.forEach { item ->
                    val pedido = item.pedido
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(pedido.tema, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(when {
                                item.falhaAoInstalar -> R.string.fabrica_falha_instalacao
                                pedido.estado == "PRONTO" && !item.instalado -> R.string.fabrica_aguardando_download
                                else -> when (pedido.estado) {
                                "NA_FILA" -> R.string.fabrica_na_fila
                                "GERANDO" -> R.string.fabrica_gerando
                                "REVISANDO" -> R.string.fabrica_revisando
                                "PRONTO" -> R.string.fabrica_pronto
                                else -> R.string.fabrica_pedido_falhou
                            }}))
                            pedido.erro?.let { erro -> Text(stringResource(when (erro) {
                                "REVISAO_REPROVADA" -> R.string.fabrica_revisao_reprovada
                                "LIMITE_DE_DICAS" -> R.string.fabrica_limite
                                "INTERROMPIDO" -> R.string.fabrica_interrompido
                                else -> R.string.fabrica_geracao_falhou
                            })) }
                            if (pedido.estado == "PRONTO") {
                                if (item.instalado) Button(onClick = onJogar) { Text(stringResource(R.string.fabrica_jogar)) }
                                if (!item.instalado) TextButton(onClick = vm::atualizar, enabled = !estado.ocupado) { Text(stringResource(R.string.fabrica_receber)) }
                                TextButton(enabled = !estado.ocupado && estado.pendente == null && pedido.podeAmpliar, onClick = {
                                    vm.pedir(pedido.tema, pedido.quantidade,
                                        orientacoes, incluirFeedback, pedido.baralhoId)
                                }) { Text(stringResource(R.string.fabrica_mais_dicas)) }
                                if (!pedido.podeAmpliar) Text(stringResource(R.string.fabrica_limite))
                            }
                        }
                    }
                }
                Text(stringResource(R.string.fabrica_historico), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
