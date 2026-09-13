package com.quemsou.app.presentation.fabrica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quemsou.app.R
import com.quemsou.app.data.fabrica.PedidoDeBaralho
import com.quemsou.app.data.fabrica.PedidoDisponivel
import com.quemsou.app.data.fabrica.RepositorioDaFabrica
import com.quemsou.app.data.fabrica.SituacaoDaFabrica
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FabricaUiState(
    val conectado: Boolean = false,
    val ocupado: Boolean = false,
    val pedidos: List<PedidoDisponivel> = emptyList(),
    val pendente: PedidoDeBaralho? = null,
    val aviso: Int? = null,
)

@HiltViewModel
class FabricaViewModel @Inject constructor(private val repositorio: RepositorioDaFabrica) : ViewModel() {
    private val estado = MutableStateFlow(FabricaUiState())
    val uiState = estado.asStateFlow()

    init {
        executar {
            aplicar(repositorio.local())
            if (estado.value.conectado) aplicar(repositorio.sincronizar())
        }
    }

    fun conectar(codigo: String) = executar {
        repositorio.conectar(codigo)
        aplicar(repositorio.local())
        aplicar(repositorio.sincronizar())
        estado.update { it.copy(aviso = R.string.fabrica_conectada) }
    }

    fun pedir(tema: String, quantidade: Int, orientacoes: String, incluirFeedback: Boolean, base: String? = null) = executar {
        repositorio.pedir(tema, quantidade, orientacoes, incluirFeedback, base)
        aplicar(repositorio.sincronizar())
        estado.update { it.copy(aviso = R.string.fabrica_pedido_enviado) }
    }

    fun reenviar() = executar {
        repositorio.reenviar()
        aplicar(repositorio.sincronizar())
    }

    fun descartarReenvio() = executar {
        repositorio.descartarReenvio()
        aplicar(repositorio.local())
    }

    fun atualizar() {
        if (estado.value.conectado) executar { aplicar(repositorio.sincronizar()) }
    }

    private fun aplicar(situacao: SituacaoDaFabrica) = estado.update {
        it.copy(conectado = situacao.conectado, pedidos = situacao.pedidos, pendente = situacao.pendente)
    }

    private fun executar(bloco: suspend () -> Unit) {
        if (estado.value.ocupado) return
        estado.update { it.copy(ocupado = true, aviso = null) }
        viewModelScope.launch {
            try { bloco() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                try { aplicar(repositorio.local()) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { /* Conserva o último estado apresentado se o cache também falhar. */ }
                estado.update { it.copy(aviso = R.string.fabrica_falha) }
            }
            finally { estado.update { it.copy(ocupado = false) } }
        }
    }
}
