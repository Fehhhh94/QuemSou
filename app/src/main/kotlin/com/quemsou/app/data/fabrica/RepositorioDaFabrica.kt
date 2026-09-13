package com.quemsou.app.data.fabrica

import com.quemsou.app.data.catalogo.DestinoDeBaralhos
import com.quemsou.app.data.catalogo.ParserDoCatalogo
import com.quemsou.app.data.catalogo.ResultadoDoParse
import com.quemsou.app.data.feedback.ExportadorDeFeedback
import com.quemsou.app.data.feedback.RegistroDeFeedback
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PedidoDisponivel(val pedido: PedidoNaFila, val instalado: Boolean, val falhaAoInstalar: Boolean = false)
data class SituacaoDaFabrica(val conectado: Boolean, val pedidos: List<PedidoDisponivel>, val pendente: PedidoDeBaralho?)

/** Coordena cache, tentativa durável e instalação. A falha de um resultado não bloqueia os demais. */
@Singleton
class RepositorioDaFabrica @Inject constructor(
    private val conexao: ConexaoDaFabrica,
    private val destino: DestinoDeBaralhos,
    private val feedback: RegistroDeFeedback,
) {
    private val mutex = Mutex()

    suspend fun local(): SituacaoDaFabrica = SituacaoDaFabrica(
        conexao.conectado(), conexao.cache().map { apresentar(it) }, conexao.pendente(),
    )

    suspend fun conectar(codigo: String) = mutex.withLock { conexao.conectar(codigo) }

    suspend fun sincronizar(): SituacaoDaFabrica = mutex.withLock {
        val pedidos = conexao.listar()
        conexao.pendente()?.let { tentativa ->
            if (pedidos.any { it.id == tentativa.id }) conexao.confirmar(tentativa.id)
        }
        val apresentados = pedidos.map { pedido ->
            try {
                var item = apresentar(pedido)
                if (pedido.estado == "PRONTO" && !item.instalado) {
                    val modelo = conexao.baixar(pedido.id)
                    require(modelo.id == pedido.baralhoId && modelo.versao == pedido.versao && pedido.versao > 0)
                    val resultado = ParserDoCatalogo().validarBaralho(modelo)
                    require(resultado is ResultadoDoParse.Sucesso)
                    destino.instalar(resultado.valor)
                    item = apresentar(pedido)
                }
                item
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { PedidoDisponivel(pedido, instalado = false, falhaAoInstalar = true) }
        }
        SituacaoDaFabrica(true, apresentados, conexao.pendente())
    }

    suspend fun pedir(tema: String, quantidade: Int, orientacoes: String, incluirFeedback: Boolean, base: String?) = mutex.withLock {
        require(tema.trim().length in 3..120 && orientacoes.length <= 1500 && quantidade in listOf(10, 20, 30))
        check(conexao.pendente() == null)
        val avaliacoes = if (incluirFeedback) {
            ExportadorDeFeedback.montarJson(feedback.buscarTodosComResposta(), Instant.now().toString())
        } else ""
        require(avaliacoes.length <= 500_000)
        conexao.guardar(PedidoDeBaralho(UUID.randomUUID().toString(), tema.trim(), quantidade, orientacoes.trim(), avaliacoes, base))
        conexao.enviarPendente()
    }

    /** Reenvia exatamente o payload salvo; editar campos ou avaliar outra carta não muda a tentativa. */
    suspend fun reenviar() = mutex.withLock { conexao.enviarPendente() }

    suspend fun descartarReenvio() = mutex.withLock {
        conexao.pendente()?.let { conexao.confirmar(it.id) }
    }

    private suspend fun apresentar(pedido: PedidoNaFila) = PedidoDisponivel(
        pedido, pedido.estado == "PRONTO" && pedido.baralhoId != null && pedido.versao > 0 &&
            destino.instalado(pedido.baralhoId, pedido.versao),
    )
}
