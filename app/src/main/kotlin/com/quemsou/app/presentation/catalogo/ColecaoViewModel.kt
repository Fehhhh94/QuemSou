package com.quemsou.app.presentation.catalogo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.quemsou.app.data.catalogo.EntradaDoCatalogo
import com.quemsou.app.data.catalogo.EstadoLocalDoBaralho
import com.quemsou.app.data.catalogo.RepositorioDoCatalogo
import com.quemsou.app.data.catalogo.ResultadoDoCatalogo
import com.quemsou.app.data.catalogo.ResultadoDoDownload
import com.quemsou.app.navigation.ColecaoRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Nível 2 do catálogo: os baralhos de uma coleção, com download por baralho
 * (progresso e cancelamento). Os downloads vivem no escopo deste ViewModel:
 * sobrevivem a rotação/dobra do aparelho; sair da tela cancela os que
 * estiverem em andamento (arquivos pequenos — segundos).
 */
@HiltViewModel
class ColecaoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repositorio: RepositorioDoCatalogo,
) : ViewModel() {

    private val colecaoId: String = savedStateHandle.toRoute<ColecaoRoute>().colecaoId

    private val _uiState = MutableStateFlow<ColecaoUiState>(ColecaoUiState.Carregando)
    val uiState: StateFlow<ColecaoUiState> = _uiState.asStateFlow()

    private val downloads = mutableMapOf<String, Job>()

    init {
        recarregar()
    }

    /** (Re)carrega a coleção a partir do catálogo (rede ou cache). */
    fun recarregar() {
        viewModelScope.launch {
            when (val resultado = repositorio.carregarCatalogo()) {
                is ResultadoDoCatalogo.Disponivel -> {
                    val daColecao = resultado.catalogo.itens
                        .filter { it.entrada.colecao.id == colecaoId }
                    if (daColecao.isEmpty()) {
                        _uiState.value = ColecaoUiState.Indisponivel
                        return@launch
                    }
                    val colecao = daColecao.first().entrada.colecao
                    _uiState.value = ColecaoUiState.Pronto(
                        nome = colecao.nome,
                        icone = colecao.icone,
                        offline = resultado.catalogo.offline,
                        baralhos = daColecao.map { item ->
                            BaralhoDaColecaoUi(
                                entrada = item.entrada,
                                estado = when (item.estadoLocal) {
                                    EstadoLocalDoBaralho.NAO_BAIXADO -> EstadoDoBaralhoUi.NaoBaixado
                                    EstadoLocalDoBaralho.BAIXADO -> EstadoDoBaralhoUi.Baixado
                                    EstadoLocalDoBaralho.ATUALIZACAO_DISPONIVEL ->
                                        EstadoDoBaralhoUi.AtualizacaoDisponivel
                                },
                            )
                        },
                        erro = null,
                    )
                }

                is ResultadoDoCatalogo.Indisponivel ->
                    _uiState.value = ColecaoUiState.Indisponivel
            }
        }
    }

    /** Baixa (ou atualiza) o baralho da [entrada], com progresso na UI. */
    fun baixar(entrada: EntradaDoCatalogo) {
        if (downloads.containsKey(entrada.id)) return
        downloads[entrada.id] = viewModelScope.launch {
            mudarEstadoDe(entrada.id, EstadoDoBaralhoUi.Baixando(progresso = 0f))
            val resultado = repositorio.baixarBaralho(entrada) { progresso ->
                mudarEstadoDe(entrada.id, EstadoDoBaralhoUi.Baixando(progresso))
            }
            downloads.remove(entrada.id)
            when (resultado) {
                is ResultadoDoDownload.Sucesso ->
                    mudarEstadoDe(entrada.id, EstadoDoBaralhoUi.Baixado)

                is ResultadoDoDownload.Falha -> {
                    reverterEstadoDe(entrada.id)
                    _uiState.update { estado ->
                        if (estado is ColecaoUiState.Pronto) estado.copy(erro = resultado.mensagem) else estado
                    }
                }
            }
        }
    }

    /** Cancela o download em andamento do baralho [entradaId]. */
    fun cancelarDownload(entradaId: String) {
        downloads.remove(entradaId)?.cancel()
        reverterEstadoDe(entradaId)
    }

    /** A UI viu o erro; limpa para não reexibir. */
    fun limparErro() {
        _uiState.update { estado ->
            if (estado is ColecaoUiState.Pronto) estado.copy(erro = null) else estado
        }
    }

    private fun mudarEstadoDe(entradaId: String, novo: EstadoDoBaralhoUi) {
        _uiState.update { estado ->
            if (estado !is ColecaoUiState.Pronto) return@update estado
            estado.copy(
                baralhos = estado.baralhos.map { baralho ->
                    if (baralho.entrada.id == entradaId) baralho.copy(estado = novo) else baralho
                },
            )
        }
    }

    /** Volta o botão ao estado derivado das versões (pós-cancelamento/falha). */
    private fun reverterEstadoDe(entradaId: String) {
        val estado = _uiState.value as? ColecaoUiState.Pronto ?: return
        val atual = estado.baralhos.firstOrNull { it.entrada.id == entradaId } ?: return
        if (atual.estado !is EstadoDoBaralhoUi.Baixando) return
        // O jeito mais simples e sempre correto de rederivar é reconsultar o
        // Room via recarregar(); como o catálogo pode estar offline, evita-se
        // rede repetida usando o cache — o repositório já faz esse fallback.
        recarregar()
    }
}

/** Estado da tela de baralhos da coleção (nível 2). */
sealed interface ColecaoUiState {

    data object Carregando : ColecaoUiState

    data class Pronto(
        val nome: String,
        val icone: String,
        val offline: Boolean,
        val baralhos: List<BaralhoDaColecaoUi>,
        val erro: String?,
    ) : ColecaoUiState

    /** Coleção desconhecida ou catálogo indisponível. */
    data object Indisponivel : ColecaoUiState
}

/** Um baralho da coleção com o estado do botão de 4 estados. */
data class BaralhoDaColecaoUi(
    val entrada: EntradaDoCatalogo,
    val estado: EstadoDoBaralhoUi,
)

/** Estado do botão do card de baralho (mockup v2). */
sealed interface EstadoDoBaralhoUi {
    data object NaoBaixado : EstadoDoBaralhoUi
    data object Baixado : EstadoDoBaralhoUi
    data object AtualizacaoDisponivel : EstadoDoBaralhoUi
    data class Baixando(val progresso: Float) : EstadoDoBaralhoUi
}
