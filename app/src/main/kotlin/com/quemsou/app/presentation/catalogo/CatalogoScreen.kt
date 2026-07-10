package com.quemsou.app.presentation.catalogo

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quemsou.app.R
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.presentation.ui.components.BannerOffline
import com.quemsou.app.presentation.ui.theme.NovidadeAmbar

/**
 * Nível 1 do catálogo: coleções com filtro por categoria, status agregado e
 * o card "Pedir um baralho" ao final. Offline, mostra o último estado
 * conhecido (banner + coleções sem download esmaecidas).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    onAbrirColecao: (String) -> Unit,
    onVoltar: () -> Unit,
    viewModel: CatalogoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarPedido by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalogo_titulo)) },
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
            is CatalogoUiState.Carregando -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is CatalogoUiState.Indisponivel -> CatalogoIndisponivel(
                onTentarDeNovo = viewModel::recarregar,
                modifier = Modifier.padding(innerPadding),
            )

            is CatalogoUiState.Pronto -> LazyColumn(
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
                    FiltroDeCategorias(filtro = estado.filtro, onFiltrar = viewModel::filtrar)
                }
                items(estado.colecoesFiltradas, key = { it.id }) { colecao ->
                    CardDeColecao(
                        colecao = colecao,
                        offline = estado.offline,
                        onAbrir = { onAbrirColecao(colecao.id) },
                    )
                }
                item {
                    CardPedirBaralho(onTocar = { mostrarPedido = true })
                }
            }
        }
    }

    if (mostrarPedido) {
        PedirBaralhoSheet(
            catalogoConhecido = (uiState as? CatalogoUiState.Pronto)
                ?.colecoes
                ?.joinToString(" · ") { "${it.nome}: ${it.quantidadeDeBaralhos} baralhos" }
                .orEmpty(),
            onFechar = { mostrarPedido = false },
        )
    }
}

@Composable
private fun FiltroDeCategorias(filtro: CardCategory?, onFiltrar: (CardCategory?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = filtro == null,
            onClick = { onFiltrar(null) },
            label = { Text(stringResource(R.string.catalogo_filtro_todas)) },
        )
        listOf(
            CardCategory.PERSONAGEM_FILME to R.string.catalogo_filtro_personagem_filme,
            CardCategory.MUNDO_DA_MUSICA to R.string.catalogo_filtro_mundo_musica,
        ).forEach { (categoria, textoId) ->
            FilterChip(
                selected = filtro == categoria,
                onClick = { onFiltrar(categoria) },
                label = { Text(stringResource(textoId)) },
            )
        }
    }
}

@Composable
private fun CardDeColecao(
    colecao: ColecaoDoCatalogoUi,
    offline: Boolean,
    onAbrir: () -> Unit,
) {
    val esmaecida = offline && colecao.baixados == 0
    Surface(
        onClick = onAbrir,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (esmaecida) 0.5f else 1f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = colecao.icone, fontSize = 32.sp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = colecao.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (colecao.temNovidade) {
                        // Pontinho âmbar: há baralho novo ou atualização na coleção.
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(NovidadeAmbar, CircleShape),
                        )
                    }
                }
                Text(
                    text = stringResource(
                        R.string.catalogo_colecao_meta,
                        colecao.quantidadeDeBaralhos,
                        colecao.quantidadeDeCards,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (esmaecida) {
                        stringResource(R.string.catalogo_requer_conexao)
                    } else {
                        stringResource(R.string.catalogo_colecao_no_aparelho, colecao.baixados, colecao.quantidadeDeBaralhos)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun CardPedirBaralho(onTocar: () -> Unit) {
    Surface(
        onClick = onTocar,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.pedir_baralho_card),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun CatalogoIndisponivel(onTentarDeNovo: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.catalogo_indisponivel),
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedButton(onClick = onTentarDeNovo) {
            Text(stringResource(R.string.catalogo_tentar_de_novo))
        }
    }
}

/**
 * "Pedir um baralho": formulário simples que monta um texto estruturado e
 * dispara o Sharesheet do Android (ACTION_SEND, text/plain) — o pedido sai
 * pelo app que o usuário escolher; nada é transmitido pelo QuemSou.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PedirBaralhoSheet(
    catalogoConhecido: String,
    onFechar: () -> Unit,
) {
    val contexto = LocalContext.current
    var tema by remember { mutableStateOf("") }
    var colecao by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    val nomeDoApp = stringResource(R.string.app_name)
    val textoDoPedido = stringResource(
        R.string.pedir_baralho_texto,
        nomeDoApp,
        tema.trim(),
        colecao.trim().ifBlank { "—" },
        observacoes.trim().ifBlank { "—" },
        catalogoConhecido.ifBlank { "—" },
    )

    ModalBottomSheet(onDismissRequest = onFechar, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.pedir_baralho_titulo), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = tema,
                onValueChange = { tema = it },
                label = { Text(stringResource(R.string.pedir_baralho_tema)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = colecao,
                onValueChange = { colecao = it },
                label = { Text(stringResource(R.string.pedir_baralho_colecao)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = observacoes,
                onValueChange = { observacoes = it },
                label = { Text(stringResource(R.string.pedir_baralho_observacoes)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, textoDoPedido)
                    }
                    contexto.startActivity(Intent.createChooser(intent, null))
                    onFechar()
                },
                enabled = tema.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 0.dp),
            ) {
                Text(stringResource(R.string.pedir_baralho_enviar))
            }
        }
    }
}
