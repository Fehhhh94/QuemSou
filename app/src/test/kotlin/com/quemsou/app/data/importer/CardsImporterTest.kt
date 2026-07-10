package com.quemsou.app.data.importer

import com.quemsou.app.data.catalogo.BaralhoJson
import com.quemsou.app.data.catalogo.CardDoBaralhoJson
import com.quemsou.app.data.catalogo.ColecaoJson
import com.quemsou.app.data.catalogo.ParserDoCatalogo
import com.quemsou.app.data.local.BaralhoDao
import com.quemsou.app.data.local.BaralhoEntity
import com.quemsou.app.data.local.CardDao
import com.quemsou.app.data.local.CardEntity
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardsImporterTest {

    /** DAOs em memória — sem Room nem Robolectric; inserir REPLACE = upsert por id. */
    private class FakeBaralhoDao : BaralhoDao {
        val baralhos = mutableListOf<BaralhoEntity>()

        override suspend fun inserirTodos(baralhos: List<BaralhoEntity>) {
            baralhos.forEach { novo ->
                this.baralhos.removeAll { it.id == novo.id }
                this.baralhos += novo
            }
        }

        override suspend fun buscarPorIds(ids: List<String>) = baralhos.filter { it.id in ids }

        override suspend fun buscarTodos() = baralhos.toList()
    }

    private class FakeCardDao : CardDao {
        val cards = mutableListOf<CardEntity>()

        override suspend fun inserirTodos(cards: List<CardEntity>) {
            cards.forEach { novo ->
                this.cards.removeAll { it.id == novo.id }
                this.cards += novo
            }
        }

        override suspend fun removerPorBaralho(baralhoId: String) {
            cards.removeAll { it.baralhoId == baralhoId }
        }

        override suspend fun buscarPorBaralhos(baralhoIds: List<String>) =
            cards.filter { it.baralhoId in baralhoIds }
    }

    /** Guarda a versão em memória e registra se houve escrita. */
    private class FakeCardsVersionStore(private var versao: Int) : CardsVersionStore {
        var versaoSalva: Int? = null

        override suspend fun versaoImportada() = versao

        override suspend fun salvarVersaoImportada(versao: Int) {
            versaoSalva = versao
            this.versao = versao
        }
    }

    private fun baralhoJson(id: String, quantidadeDeCards: Int = 2, dicasPorCard: Int = 10) = BaralhoJson(
        id = id,
        nome = "Baralho $id",
        categoria = "PERSONAGEM_FILME",
        colecao = ColecaoJson(id = "colecao-teste", nome = "Coleção de Teste", icone = "🧪"),
        versao = 1,
        estado = "FINALIZADO",
        cards = List(quantidadeDeCards) { indice ->
            CardDoBaralhoJson(
                id = "$id-card-${indice + 1}",
                type = "PESSOA",
                answer = "Resposta ${indice + 1}",
                clues = List(dicasPorCard) { "dica ${it + 1}" },
            )
        },
    )

    private fun jsonCom(versao: Int, baralhos: List<BaralhoJson>) =
        Json.encodeToString(CardsJson(version = versao, baralhos = baralhos))

    private fun importer(json: String, baralhoDao: BaralhoDao, cardDao: CardDao, store: CardsVersionStore) =
        CardsImporter(
            fonte = { json },
            parser = ParserDoCatalogo(),
            baralhoDao = baralhoDao,
            cardDao = cardDao,
            versionStore = store,
        )

    @Test
    fun `asset mais novo que o banco importa baralhos e cards e salva a versao`() = runTest {
        val baralhoDao = FakeBaralhoDao()
        val cardDao = FakeCardDao()
        val store = FakeCardsVersionStore(versao = 0)
        val json = jsonCom(versao = 1, baralhos = listOf(baralhoJson("b1"), baralhoJson("b2", quantidadeDeCards = 3)))

        val resultado = importer(json, baralhoDao, cardDao, store).importarSeNecessario()

        assertEquals(ResultadoImportacao.Importado(quantidade = 5, versao = 1), resultado)
        assertEquals(listOf("b1", "b2"), baralhoDao.baralhos.map { it.id })
        assertEquals(5, cardDao.cards.size)
        assertEquals(setOf("b1", "b2"), cardDao.cards.map { it.baralhoId }.toSet())
        assertEquals(1, store.versaoSalva)
    }

    @Test
    fun `asset na mesma versao do banco nao importa`() = runTest {
        val baralhoDao = FakeBaralhoDao()
        val cardDao = FakeCardDao()
        val store = FakeCardsVersionStore(versao = 1)
        val json = jsonCom(versao = 1, baralhos = listOf(baralhoJson("b1")))

        val resultado = importer(json, baralhoDao, cardDao, store).importarSeNecessario()

        assertEquals(ResultadoImportacao.NadaAFazer(versao = 1), resultado)
        assertEquals(0, baralhoDao.baralhos.size)
        assertEquals(0, cardDao.cards.size)
        assertNull(store.versaoSalva)
    }

    @Test
    fun `reimportacao substitui o baralho embarcado e preserva os baixados do catalogo`() = runTest {
        // "baixado-1" veio do catálogo; "b1" é embarcado e será substituído.
        val baralhoDao = FakeBaralhoDao()
        baralhoDao.baralhos += BaralhoEntity(
            id = "baixado-1",
            nome = "Baixado do Catálogo",
            categoria = "MUNDO_DA_MUSICA",
            versao = 2,
            estado = "EM_DESENVOLVIMENTO",
            colecaoId = "baixada",
            colecaoNome = "Baixada",
            colecaoIcone = "⬇️",
        )
        val cardDao = FakeCardDao()
        cardDao.cards += CardEntity(
            id = "baixado-01",
            type = "COISA",
            category = "MUNDO_DA_MUSICA",
            answer = "BAIXADO",
            clues = List(10) { "dica ${it + 1}" },
            baralhoId = "baixado-1",
        )
        cardDao.cards += CardEntity(
            id = "b1-card-velho",
            type = "COISA",
            category = "PERSONAGEM_FILME",
            answer = "VELHO",
            clues = List(10) { "dica ${it + 1}" },
            baralhoId = "b1",
        )
        val store = FakeCardsVersionStore(versao = 1)
        val json = jsonCom(versao = 2, baralhos = listOf(baralhoJson("b1", quantidadeDeCards = 3)))

        val resultado = importer(json, baralhoDao, cardDao, store).importarSeNecessario()

        assertEquals(ResultadoImportacao.Importado(quantidade = 3, versao = 2), resultado)
        // O baixado ficou intacto; o embarcado foi substituído por inteiro.
        assertEquals(setOf("baixado-1", "b1"), baralhoDao.baralhos.map { it.id }.toSet())
        assertEquals(
            listOf("baixado-01", "b1-card-1", "b1-card-2", "b1-card-3"),
            cardDao.cards.map { it.id },
        )
        assertEquals(2, store.versaoSalva)
    }

    @Test
    fun `baralho embarcado invalido interrompe com as violacoes legiveis na mensagem`() = runTest {
        val json = jsonCom(versao = 1, baralhos = listOf(baralhoJson("b1", dicasPorCard = 8)))
        val importador = importer(json, FakeBaralhoDao(), FakeCardDao(), FakeCardsVersionStore(0))

        val excecao = try {
            importador.importarSeNecessario()
            null
        } catch (excecao: IllegalArgumentException) {
            excecao
        }

        val mensagem = requireNotNull(excecao) { "esperava IllegalArgumentException" }.message!!
        assertTrue(mensagem.contains("b1"))
        assertTrue(mensagem.contains("8"))
    }
}
