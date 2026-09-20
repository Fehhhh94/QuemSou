package com.quemsou.app.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quemsou.app.data.catalogo.*
import com.quemsou.app.data.fabrica.*
import com.quemsou.app.data.feedback.RegistroDeFeedbackLocal
import com.quemsou.app.data.local.AppDatabase
import com.quemsou.app.domain.model.ProgressoDaPartida
import com.quemsou.app.domain.rules.SelecionadorDeDicas
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class FabricaPersistenciaTest {
    private val contexto get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun tentativaCompletaSobreviveAoFechamentoEReaberturaDoDataStore() = runBlocking {
        val arquivo = File(contexto.cacheDir, "fabrica-${UUID.randomUUID()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        fun servico() = ServicoDaFabrica(PreferenceDataStoreFactory.create(scope = scope) { arquivo })
        try {
            val pedido = PedidoDeBaralho(UUID.randomUUID().toString(), "Cinema", 10, "Entre amigos", "snapshot da avaliação")
            servico().guardar(pedido)
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val reaberto = servico()
            assertEquals(pedido, reaberto.pendente())
            reaberto.confirmar("outro-id")
            assertEquals(pedido, reaberto.pendente())
            reaberto.confirmar(pedido.id)
            assertNull(reaberto.pendente())
            try { reaberto.conectar("http://127.0.0.1|" + "a".repeat(43)); fail() }
            catch (_: IllegalArgumentException) { }
            assertFalse(reaberto.conectado())
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            arquivo.delete()
        }
    }

    @Test fun ampliacaoInstaladaPelaFabricaPreservaCartaAtivaEHistoricoNoRoom() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        try {
            var versao = 1
            var total = 60
            fun modelo() = BaralhoJson("privado", "Cinema", "PERSONAGEM_FILME", ColecaoJson("c", "Cinema", "C"),
                versao, "EM_DESENVOLVIMENTO", listOf(CardDoBaralhoJson("hp", "PESSOA", "Harry Potter",
                    List(10) { "Fato independente $it" }, "harry-potter", List(total) { DicaJson("d$it", "Fato independente $it") })))
            val conexao = object : ConexaoDaFabrica {
                override suspend fun conectado() = true
                override suspend fun conectar(codigo: String) = Unit
                override suspend fun listar() = listOf(PedidoNaFila("pedido", "Cinema", "PRONTO", "privado", versao))
                override suspend fun cache() = listar()
                override suspend fun baixar(id: String) = modelo()
                override suspend fun pendente(): PedidoDeBaralho? = null
                override suspend fun guardar(pedido: PedidoDeBaralho) = Unit
                override suspend fun enviarPendente() = Unit
                override suspend fun confirmar(id: String) = Unit
            }
            val fabrica = RepositorioDaFabrica(conexao, InstaladorDeBaralhos(db), RegistroDeFeedbackLocal(db.feedbackDeCardDao()))
            assertTrue(fabrica.sincronizar().pedidos.single().instalado)
            val jogo = RepositorioDeCardsLocal(db.baralhoDao(), db.cardDao(), db)
            val base = jogo.prepararSessao("s", listOf("privado")).single().cards.single()
            val carta = jogo.prepararTurno("s", 1, base, 42)
            jogo.salvarProgresso("s", ProgressoDaPartida(1, "DICA_REVELADA", listOf(0, 0), listOf(1, 2, 3)), carta.clues.take(3), false)
            val historico = db.historicoDeDicasDao().usadas().toSet()
            versao = 2
            total = 120
            assertTrue(fabrica.sincronizar().pedidos.single().instalado)
            assertEquals(historico, db.historicoDeDicasDao().usadas().toSet())
            assertEquals(carta, jogo.prepararTurno("s", 1, base, 999))
            jogo.encerrarSessao("s")
            val atual = jogo.buscarTodos().single().cards.single()
            assertEquals(117, SelecionadorDeDicas.disponiveis(atual, historico).size)
            val novaBase = jogo.prepararSessao("s2", listOf("privado")).single().cards.single()
            val novaCarta = jogo.prepararTurno("s2", 1, novaBase, 13)
            assertTrue(novaCarta.clues.intersect(carta.clues.take(3).toSet()).isEmpty())
        } finally { db.close() }
    }

    @Test fun estadoFinalizadoLegadoNaoBloqueiaAtualizacao() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        try {
            fun modelo(versao: Int): com.quemsou.app.domain.model.Baralho {
                val json = BaralhoJson(
                    "legado",
                    "Tema legado",
                    "PERSONAGEM_FILME",
                    ColecaoJson("legado", "Tema legado", "🎬"),
                    versao,
                    "FINALIZADO",
                    listOf(
                        CardDoBaralhoJson(
                            "legado-1",
                            "PESSOA",
                            "Resposta",
                            List(10) { "Dica válida ${it + 1}" },
                        ),
                    ),
                )
                return (ParserDoCatalogo().validarBaralho(json) as ResultadoDoParse.Sucesso).valor
            }

            val instalador = InstaladorDeBaralhos(db)
            instalador.instalar(modelo(1))
            instalador.instalar(modelo(2))

            assertEquals(2, db.baralhoDao().buscarPorIds(listOf("legado")).single().versao)
        } finally {
            db.close()
        }
    }
}
