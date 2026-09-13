package com.quemsou.app.data.fabrica

import com.quemsou.app.data.catalogo.*
import com.quemsou.app.data.feedback.*
import com.quemsou.app.data.local.FeedbackComResposta
import com.quemsou.app.domain.model.Baralho
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RepositorioDaFabricaTest {
    private class Conexao : ConexaoDaFabrica {
        var tentativa: PedidoDeBaralho? = null
        var lista = emptyList<PedidoNaFila>()
        val resultados = mutableMapOf<String, BaralhoJson>()
        val envios = mutableListOf<PedidoDeBaralho>()
        var falharEnvio = false
        var falharLista = false
        var cancelarDownload = false
        var downloads = 0
        override suspend fun conectado() = true
        override suspend fun conectar(codigo: String) = Unit
        override suspend fun listar(): List<PedidoNaFila> {
            if (falharLista) throw IOException()
            return lista
        }
        override suspend fun cache() = lista
        override suspend fun baixar(id: String): BaralhoJson {
            downloads++
            if (cancelarDownload) throw CancellationException()
            return resultados.getValue(id)
        }
        override suspend fun pendente() = tentativa
        override suspend fun guardar(pedido: PedidoDeBaralho) { check(tentativa == null); tentativa = pedido }
        override suspend fun enviarPendente() {
            val p = tentativa ?: return
            envios += p
            if (falharEnvio) throw IOException()
            confirmar(p.id)
        }
        override suspend fun confirmar(id: String) { if (tentativa?.id == id) tentativa = null }
    }
    private class Destino : DestinoDeBaralhos {
        val versoes = mutableMapOf<String, Int>()
        override suspend fun instalado(id: String, versao: Int) = (versoes[id] ?: 0) >= versao
        override suspend fun instalar(baralho: Baralho) { versoes[baralho.id] = baralho.versao }
    }
    private class Feedback : RegistroDeFeedback {
        var leituras = 0
        override suspend fun registrar(novo: NovoFeedback) = Unit
        override fun quantidade() = flowOf(0)
        override suspend fun buscarTodosComResposta(): List<FeedbackComResposta> { leituras++; return emptyList() }
        override suspend fun apagarTudo() = Unit
    }
    private val conexao = Conexao()
    private val destino = Destino()
    private val feedback = Feedback()
    private fun repo() = RepositorioDaFabrica(conexao, destino, feedback)
    private fun modelo(id: String) = BaralhoJson(id, "Cinema", "PERSONAGEM_FILME", ColecaoJson("c", "Cinema", "C"),
        1, "EM_DESENVOLVIMENTO", listOf(CardDoBaralhoJson("$id-c", "PESSOA", "Harry Potter",
            List(10) { "Fato $it" }, "harry-potter", List(60) { DicaJson("d$it", "Fato $it") })))

    @Test fun `timeout e recriacao preservam id e snapshot de feedback`() = runTest {
        conexao.falharEnvio = true
        try { repo().pedir("Cinema", 10, "Com amigos", true, null); fail() } catch (_: IOException) { }
        val original = conexao.envios.single()
        assertEquals(original, repo().local().pendente)
        conexao.falharEnvio = false
        repo().reenviar()
        assertEquals(listOf(original, original), conexao.envios)
        assertEquals(1, feedback.leituras)
        assertNull(conexao.tentativa)
    }

    @Test fun `consulta confirma pedido aceito mesmo quando resposta do post se perdeu`() = runTest {
        conexao.falharEnvio = true
        try { repo().pedir("Cinema", 10, "", false, null) } catch (_: IOException) { }
        val p = requireNotNull(conexao.tentativa)
        conexao.lista = listOf(PedidoNaFila(p.id, p.tema, "GERANDO"))
        assertNull(repo().sincronizar().pendente)
        assertEquals(1, conexao.envios.size)
    }

    @Test fun `desabilitar feedback nao le nem envia avaliacoes`() = runTest {
        repo().pedir("Cinema", 10, "", false, null)
        assertEquals(0, feedback.leituras)
        assertEquals("", conexao.envios.single().feedback)
    }

    @Test fun `resultado invalido nao impede instalacao de outro valido`() = runTest {
        conexao.lista = listOf(PedidoNaFila("p1", "Um", "PRONTO", "b1", 1), PedidoNaFila("p2", "Dois", "PRONTO", "b2", 1))
        conexao.resultados["p1"] = modelo("id-diferente")
        conexao.resultados["p2"] = modelo("b2")
        val itens = repo().sincronizar().pedidos
        assertTrue(itens[0].falhaAoInstalar)
        assertFalse(itens[0].instalado)
        assertTrue(itens[1].instalado)
        assertEquals(mapOf("b2" to 1), destino.versoes)
    }

    @Test fun `versao instalada ou superior evita download e downgrade`() = runTest {
        destino.versoes["b"] = 2
        conexao.lista = listOf(PedidoNaFila("p", "Cinema", "PRONTO", "b", 1))
        assertTrue(repo().sincronizar().pedidos.single().instalado)
        assertEquals(0, conexao.downloads)
        assertEquals(2, destino.versoes["b"])
    }

    @Test fun `cache offline mantem pedido e distingue pronto de instalado`() = runTest {
        conexao.lista = listOf(PedidoNaFila("p", "Cinema", "PRONTO", "b", 1))
        conexao.falharLista = true
        assertFalse(repo().local().pedidos.single().instalado)
        destino.versoes["b"] = 1
        assertTrue(repo().local().pedidos.single().instalado)
        assertEquals(0, conexao.downloads)
    }

    @Test fun `cancelamento do download nao e tratado como falha de conteudo`() = runTest {
        conexao.lista = listOf(PedidoNaFila("p", "Cinema", "PRONTO", "b", 1))
        conexao.cancelarDownload = true
        try { repo().sincronizar(); fail() } catch (_: CancellationException) { }
        assertTrue(destino.versoes.isEmpty())
    }

    @Test fun `nova solicitacao nao substitui tentativa incerta`() = runTest {
        conexao.falharEnvio = true
        try { repo().pedir("Cinema", 10, "", false, null) } catch (_: IOException) { }
        val original = conexao.tentativa
        try { repo().pedir("Musica", 20, "", false, null); fail() } catch (_: IllegalStateException) { }
        assertEquals(original, conexao.tentativa)
    }

    @Test fun `quantidade invalida nao cria tentativa`() = runTest {
        try { repo().pedir("Cinema", 5, "", false, null); fail() } catch (_: IllegalArgumentException) { }
        assertNull(conexao.tentativa)
    }
}
