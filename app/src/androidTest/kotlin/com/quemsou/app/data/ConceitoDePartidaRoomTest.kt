package com.quemsou.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.quemsou.app.data.local.*
import com.quemsou.app.data.catalogo.ParserDoCatalogo
import com.quemsou.app.data.catalogo.ResultadoDoParse
import com.quemsou.app.data.importer.CardsJson
import com.quemsou.app.domain.model.*
import com.quemsou.app.domain.rules.*
import com.quemsou.app.data.espelho.JogadorDoEspelho
import com.quemsou.app.data.espelho.ResultadoDoInicio
import com.quemsou.app.data.espelho.ServidorDoEspelho
import com.quemsou.app.presentation.setup.MotivoDoBloqueio
import com.quemsou.app.presentation.setup.SetupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ConceitoDePartidaRoomTest {
    private val contexto get() = ApplicationProvider.getApplicationContext<Context>()
    private fun banco() = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
    private suspend fun comBanco(bloco: suspend (AppDatabase) -> Unit) {
        val db = banco()
        try { bloco(db) } finally { db.close() }
    }
    private fun repo(db: AppDatabase) = RepositorioDeCardsLocal(db.baralhoDao(), db.cardDao(), db)
    private fun card(id: String = "hp", resposta: String = "Harry Potter", inicio: Int = 0, total: Int = 60): Card {
        val dicas = List(total) { DicaDoBanco("d${it + inicio}", "Fato ${it + inicio} de $resposta") }
        return Card(id, CardType.PESSOA, CardCategory.PERSONAGEM_FILME, resposta, dicas.take(10).map { it.texto }, resposta, dicas)
    }
    private suspend fun inserir(db: AppDatabase, id: String = "b", cards: List<Card> = listOf(card())) {
        val b = Baralho(id, id, CardCategory.PERSONAGEM_FILME, Colecao("c", "Cinema", "C"), 1, EstadoDoBaralho.EM_DESENVOLVIMENTO, cards)
        db.baralhoDao().inserirTodos(listOf(b.paraEntidade()))
        db.cardDao().inserirTodos(cards.map { it.paraEntidade(id) })
    }
    private suspend fun abrir(r: RepositorioDeCardsLocal, sessao: String, ids: List<String> = listOf("b")): Card {
        val b = r.prepararSessao(sessao, ids)
        val carta = r.prepararTurno(sessao, 1, b.first().cards.first(), 9)
        r.salvarProgresso(sessao, ProgressoDaPartida(1, "GRID", listOf(0, 0), emptyList()), emptyList(), false)
        return carta
    }

    @Test fun catalogoEmbarcadoRealOferece60RespostasSemDependerDaFabrica() = runBlocking {
        comBanco { db ->
            val json = contexto.assets.open("cards.json").bufferedReader().use { it.readText() }
            val baralhos = CardsJson.deJson(json).baralhos.map {
                (ParserDoCatalogo().validarBaralho(it) as ResultadoDoParse.Sucesso).valor
            }
            baralhos.forEach { b ->
                db.baralhoDao().inserirTodos(listOf(b.paraEntidade()))
                db.cardDao().inserirTodos(b.cards.map { it.paraEntidade(b.id) })
            }
            val disponiveis = repo(db).buscarTodos()
            assertEquals(2, disponiveis.size)
            assertEquals(60, AcervoDeRespostas.contarIdentidades(disponiveis.flatMap { it.cards }.map(AcervoDeRespostas::chaves)))
            assertTrue(disponiveis.flatMap { it.cards }.all { SelecionadorDeDicas.disponiveis(it, emptySet()).size >= 10 })
        }
    }

    @Test fun tresVistasDeixam57LivresEAsSeteVoltamAposAnuncio() = runBlocking {
        comBanco { db ->
            inserir(db)
            val r = repo(db)
            val carta = abrir(r, "s")
            assertTrue(db.historicoDeDicasDao().usadas().isEmpty())
            assertTrue(db.historicoDeDicasDao().reservadas().isNotEmpty())
            r.salvarProgresso("s", ProgressoDaPartida(1, "DICA_REVELADA", listOf(0, 0), listOf(3, 8, 2)), carta.clues.take(3), false)
            assertEquals(57, SelecionadorDeDicas.disponiveis(card(), db.historicoDeDicasDao().usadas().toSet()).size)
            r.salvarProgresso("s", ProgressoDaPartida(1, "ANUNCIO", listOf(0, 0), listOf(3, 8, 2), "j2"), carta.clues.take(3), true)
            assertTrue(db.historicoDeDicasDao().reservadas().isEmpty())
            val livres = SelecionadorDeDicas.disponiveis(card(), db.historicoDeDicasDao().usadas().toSet()).map { it.texto }
            assertTrue(livres.containsAll(carta.clues.drop(3)))
            val seguinte = abrir(r, "s2")
            assertTrue(seguinte.clues.intersect(carta.clues.take(3).toSet()).isEmpty())
        }
    }

    /**
     * Usa consumo e consulta reais do Room até o estado do Setup. Uma resposta
     * com dez dicas fica inelegível após revelar uma; o baralho e as nove dicas
     * não vistas continuam no acervo. Nenhum servidor precisa ser iniciado.
     */
    @Test fun setupReconheceEsgotamentoRealETrocaDeSelecaoSemApagarHistorico() = runBlocking {
        comBanco { db ->
            inserir(db, cards = listOf(card(total = 10)))
            val r = repo(db)
            val store = ViewModelStore()
            val espelho = object : ServidorDoEspelho {
                override val endereco = MutableStateFlow<String?>(null)
                override val conectados = MutableStateFlow(emptyMap<String, Boolean>())
                override val esteAparelho = MutableStateFlow<String?>(null)
                override suspend fun iniciar(jogadores: List<JogadorDoEspelho>): ResultadoDoInicio =
                    error("Preparar partida offline não deve iniciar o espelho")
                override fun atualizarJogadores(jogadores: List<JogadorDoEspelho>) = Unit
                override fun marcarEsteAparelho(jogadorId: String?) = Unit
                override fun liberarLugar(jogadorId: String) = Unit
                override fun parar() = Unit
            }
            val vm = withContext(Dispatchers.Main) {
                SetupViewModel(r, espelho).also {
                    store.put("setup", it)
                    it.renomearJogador(0, "Ana")
                    it.renomearJogador(1, "Bruno")
                    it.definirRodadas(1)
                }
            }
            try {
                val inicial = withTimeout(10_000) { vm.uiState.first { it.baralhosCarregados } }
                assertTrue(inicial.podeComecar)

                val carta = abrir(r, "esgotamento")
                r.salvarProgresso(
                    "esgotamento",
                    ProgressoDaPartida(1, "ANUNCIO", listOf(0, 10), listOf(1), "j2"),
                    carta.clues.take(1),
                    true,
                )
                r.encerrarSessao("esgotamento")
                withContext(Dispatchers.Main) { vm.recarregarBaralhos() }
                val esgotado = withTimeout(10_000) { vm.uiState.first { it.semRespostasNoAparelho } }
                assertEquals(listOf("b"), esgotado.baralhosDisponiveis.map { it.id })
                assertEquals(MotivoDoBloqueio.SEM_RESPOSTAS_NO_APARELHO, esgotado.motivoDoBloqueio)
                assertFalse(esgotado.podeComecar)
                val historico = db.historicoDeDicasDao().usadas().toSet()
                val editorial = r.buscarPorIds(listOf("b")).single().cards.single()
                assertEquals(10, editorial.bancoDeDicas.size)
                assertEquals(9, SelecionadorDeDicas.disponiveis(editorial, historico).size)

                inserir(db, "novo", listOf(card("outro", "Outra resposta", total = 10)))
                withContext(Dispatchers.Main) { vm.recarregarBaralhos() }
                val recarregado = withTimeout(10_000) {
                    vm.uiState.first { it.baralhosDisponiveis.size == 2 }
                }
                assertEquals(setOf("b"), recarregado.baralhosSelecionados)
                assertEquals(MotivoDoBloqueio.SELECAO_SEM_RESPOSTAS, recarregado.motivoDoBloqueio)
                withContext(Dispatchers.Main) { vm.alternarBaralho("novo") }
                assertTrue(vm.uiState.value.podeComecar)
                assertEquals(historico, db.historicoDeDicasDao().usadas().toSet())
            } finally {
                withContext(Dispatchers.Main) { store.clear() }
            }
        }
    }

    @Test fun falhaDeCheckpointDesfazConsumoEHistoricoNaMesmaTransacao() = runBlocking {
        comBanco { db ->
            inserir(db)
            val r = repo(db)
            val b = r.prepararSessao("s", listOf("b"))
            val carta = r.prepararTurno("s", 1, b.single().cards.single(), 1)
            try {
                r.salvarProgresso("s", ProgressoDaPartida(1, "DICA_REVELADA", listOf(0, 0), listOf(1, 2)), listOf(carta.clues.first(), "Não pertence"), false)
                fail("Deveria rejeitar texto fora da carta")
            } catch (_: IllegalArgumentException) { }
            assertTrue(db.historicoDeDicasDao().usadas().isEmpty())
            assertTrue(db.historicoDeDicasDao().respostas().isEmpty())
            assertNull(r.progressoDaSessao("s"))
        }
    }

    @Test fun prepararDuasVezesEmConcorrenciaReservaUmaUnicaCarta() = runBlocking {
        comBanco { db ->
            inserir(db)
            val r = repo(db)
            val base = r.prepararSessao("s", listOf("b")).single().cards.single()
            val cartas = coroutineScope {
                val a = async { r.prepararTurno("s", 1, base, 1) }
                val b = async { r.prepararTurno("s", 1, base, 999) }
                listOf(a.await(), b.await())
            }
            assertEquals(cartas.first(), cartas.last())
            assertTrue(db.historicoDeDicasDao().usadas().isEmpty())
            assertTrue(db.historicoDeDicasDao().respostas().isEmpty())
        }
    }

    @Test fun abandonoLiberaOcultasEOutraSessaoNaoRetomaCartaAntiga() = runBlocking {
        comBanco { db ->
            inserir(db)
            val r = repo(db)
            val carta = abrir(r, "s")
            r.salvarProgresso("s", ProgressoDaPartida(1, "DICA_REVELADA", listOf(0, 0), listOf(4)), carta.clues.take(1), false)
            r.encerrarSessao("s")
            assertTrue(db.historicoDeDicasDao().reservadas().isEmpty())
            assertEquals(59, SelecionadorDeDicas.disponiveis(card(), db.historicoDeDicasDao().usadas().toSet()).size)
            try { r.prepararSessao("s", listOf("b")); fail("Sessão abandonada não pode reabrir") }
            catch (_: IllegalStateException) { }
            val seguinte = abrir(r, "s2")
            assertFalse(carta.clues.first() in seguinte.clues)
        }
    }

    @Test fun acervoCompartilhadoIncluiBaralhoNaoSelecionadoEHistoricoSobreviveRemocao() = runBlocking {
        comBanco { db ->
            inserir(db, "a", listOf(card("a", total = 30)))
            inserir(db, "b", listOf(card("b", inicio = 30, total = 30)))
            val r = repo(db)
            assertTrue(r.buscarTodos().all { it.cards.single().bancoDeDicas.size == 60 })
            val carta = abrir(r, "s", listOf("a"))
            r.salvarProgresso("s", ProgressoDaPartida(1, "ANUNCIO", listOf(0, 0), listOf(1)), carta.clues.take(1), true)
            db.cardDao().removerPorBaralho("a")
            db.cardDao().removerPorBaralho("b")
            assertTrue(db.historicoDeDicasDao().usadas().isNotEmpty())
            assertTrue(db.historicoDeDicasDao().respostas().isNotEmpty())
            assertEquals(carta, r.prepararTurno("s", 1, card(), 99))
        }
    }

    @Test fun ultimaAparicaoSoAvancaUmaVezEHistoricoDaSessaoFicaCongelado() = runBlocking {
        comBanco { db ->
            inserir(db, cards = listOf(card(), card("r2", "Outra resposta")))
            val r = repo(db)
            abrir(r, "s")
            val antes = db.historicoDeDicasDao().respostas()
            r.salvarProgresso("s", ProgressoDaPartida(1, "GRID", listOf(0, 0), emptyList()), emptyList(), false)
            assertEquals(antes, db.historicoDeDicasDao().respostas())
            assertTrue(r.historicoDaSessao("s").isEmpty())
            val novas = r.prepararSessao("s2", listOf("b"))
            val monte = RotacaoDeRespostas.ordenar(Baralho.uniaoDeterministica(novas), r.historicoDaSessao("s2"), 7)
            assertEquals("Outra resposta", monte.first().answer)
            assertTrue(db.historicoDeDicasDao().reservadas().isEmpty())
        }
    }

    @Test fun fecharReabrirBancoPreservaCartaCheckpointERotacaoMesmoSemConteudoOriginal() = runBlocking {
        val nome = "conceito-reabertura.db"
        contexto.deleteDatabase(nome)
        var db = Room.databaseBuilder(contexto, AppDatabase::class.java, nome).build()
        try {
            inserir(db)
            val r = repo(db)
            val carta = abrir(r, "s")
            val progresso = ProgressoDaPartida(1, "DICA_REVELADA", listOf(0, 0), listOf(7, 2, 9))
            r.salvarProgresso("s", progresso, carta.clues.take(3), false)
            db.cardDao().removerPorBaralho("b")
            db.close()
            db = Room.databaseBuilder(contexto, AppDatabase::class.java, nome).build()
            val restaurado = repo(db)
            val base = restaurado.prepararSessao("s", listOf("b")).single().cards.single()
            assertEquals(carta, restaurado.prepararTurno("s", 1, base, 999))
            assertEquals(progresso, restaurado.progressoDaSessao("s"))
            assertEquals(57, SelecionadorDeDicas.disponiveis(base, db.historicoDeDicasDao().usadas().toSet()).size)
            assertTrue(restaurado.historicoDaSessao("s").isEmpty())
            assertTrue(db.historicoDeDicasDao().respostas().isNotEmpty())
        } finally { db.close(); contexto.deleteDatabase(nome) }
    }

    @Test fun upgrade5Para6PreservaHistoricoConservadorDaVersaoAnterior() = runBlocking {
        val nome = "conceito-upgrade5.db"
        contexto.deleteDatabase(nome)
        val caminho = contexto.getDatabasePath(nome)
        caminho.parentFile!!.mkdirs()
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.quemsou.app.data.local.AppDatabase/5.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(caminho, null).use { banco ->
            val entidades = schema.getJSONArray("entities")
            for (i in 0 until entidades.length()) {
                val e = entidades.getJSONObject(i)
                banco.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
                val indices = e.getJSONArray("indices")
                for (j in 0 until indices.length()) banco.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
            }
            banco.execSQL("INSERT INTO dicas_utilizadas VALUES ('historico-conservado')")
            banco.execSQL("INSERT INTO sessoes_de_dicas VALUES ('antiga', '[]')")
            banco.version = 5
        }
        val db = Room.databaseBuilder(contexto, AppDatabase::class.java, nome).addMigrations(MIGRACAO_5_6).build()
        try {
            assertEquals(listOf("historico-conservado"), db.historicoDeDicasDao().usadas())
            assertEquals("{}", db.historicoDeDicasDao().sessao("antiga")!!.historicoJson)
            assertTrue(db.historicoDeDicasDao().reservadas().isEmpty())
        } finally { db.close(); contexto.deleteDatabase(nome) }
    }
}
