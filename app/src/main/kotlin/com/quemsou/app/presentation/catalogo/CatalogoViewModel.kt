package com.quemsou.app.presentation.catalogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quemsou.app.data.catalogo.EstadoLocalDoBaralho
import com.quemsou.app.data.catalogo.ItemDoCatalogo
import com.quemsou.app.data.catalogo.RepositorioDoCatalogo
import com.quemsou.app.data.catalogo.ResultadoDoCatalogo
import com.quemsou.app.domain.model.CardCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Nível 1 do catálogo: as coleções, agregadas a partir do índice (rede ou
 * cache) cruzado com o Room. O filtro por categoria é só de exibição.
 */
@HiltViewModel
class CatalogoViewModel @Inject constructor(
    private val repositorio: RepositorioDoCatalogo,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogoUiState>(CatalogoUiState.Carregando)
    val uiState: StateFlow<CatalogoUiState> = _uiState.asStateFlow()

    init {
        recarregar()
    }

    /** (Re)carrega o catálogo — rede primeiro, cache como fallback offline. */
    fun recarregar() {
        viewModelScope.launch {
            when (val resultado = repositorio.carregarCatalogo()) {
                is ResultadoDoCatalogo.Disponivel -> _uiState.value = CatalogoUiState.Pronto(
                    colecoes = agruparPorColecao(resultado.catalogo.itens),
                    offline = resultado.catalogo.offline,
                    filtro = (_uiState.value as? CatalogoUiState.Pronto)?.filtro,
                )

                is ResultadoDoCatalogo.Indisponivel ->
                    _uiState.value = CatalogoUiState.Indisponivel
            }
        }
    }

    /** Aplica o filtro por [categoria]; `null` = todas. */
    fun filtrar(categoria: CardCategory?) {
        _uiState.update { estado ->
            if (estado is CatalogoUiState.Pronto) estado.copy(filtro = categoria) else estado
        }
    }

    private fun agruparPorColecao(itens: List<ItemDoCatalogo>): List<ColecaoDoCatalogoUi> =
        itens
            .groupBy { it.entrada.colecao.id }
            .map { (colecaoId, doGrupo) ->
                val colecao = doGrupo.first().entrada.colecao
                ColecaoDoCatalogoUi(
                    id = colecaoId,
                    nome = colecao.nome,
                    icone = colecao.icone,
                    quantidadeDeBaralhos = doGrupo.size,
                    quantidadeDeCards = doGrupo.sumOf { it.entrada.quantidadeDeCards },
                    baixados = doGrupo.count { it.estadoLocal != EstadoLocalDoBaralho.NAO_BAIXADO },
                    temNovidade = doGrupo.any { it.estadoLocal != EstadoLocalDoBaralho.BAIXADO },
                    categorias = doGrupo.map { it.entrada.categoria }.toSet(),
                )
            }
            .sortedBy { it.nome }
}

/** Estado da tela de coleções (nível 1). */
sealed interface CatalogoUiState {

    /** Buscando o índice (rede ou cache). */
    data object Carregando : CatalogoUiState

    /** Catálogo em mãos; [offline] liga o banner e esmaece não-baixados. */
    data class Pronto(
        val colecoes: List<ColecaoDoCatalogoUi>,
        val offline: Boolean,
        val filtro: CardCategory? = null,
    ) : CatalogoUiState {
        /** Coleções após o filtro por categoria (só exibição). */
        val colecoesFiltradas: List<ColecaoDoCatalogoUi>
            get() = filtro?.let { alvo -> colecoes.filter { alvo in it.categorias } } ?: colecoes
    }

    /** Sem rede e sem cache: só o aviso e o tentar de novo. */
    data object Indisponivel : CatalogoUiState
}

/** Uma coleção agregada para o card do nível 1. */
data class ColecaoDoCatalogoUi(
    val id: String,
    val nome: String,
    val icone: String,
    val quantidadeDeBaralhos: Int,
    val quantidadeDeCards: Int,
    val baixados: Int,
    val temNovidade: Boolean,
    val categorias: Set<CardCategory>,
)
