package com.quemsou.app.data.importer

import com.quemsou.app.data.local.CardDao
import com.quemsou.app.data.local.CardEntity
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardsImporterTest {

    /** DAO em memória — sem Room nem Robolectric. */
    private class FakeCardDao : CardDao {
        val cards = mutableListOf<CardEntity>()

        override suspend fun inserirTodos(cards: List<CardEntity>) {
            this.cards += cards
        }

        override suspend fun limparTabela() = cards.clear()

        override suspend fun buscarPorCategoria(categoria: String) =
            cards.filter { it.category == categoria }

        override suspend fun buscarTodas() = cards.toList()

        override suspend fun contar() = cards.size
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

    private fun jsonComCards(versao: Int, quantidade: Int = 2): String {
        val cards = List(quantidade) { indice ->
            CardJson(
                id = "teste-${indice + 1}",
                type = CardType.PESSOA,
                category = CardCategory.LIVRE,
                answer = "TESTE_${indice + 1}",
                clues = List(10) { "dica ${it + 1}" },
            )
        }
        return Json.encodeToString(CardsJson(version = versao, cards = cards))
    }

    private fun importer(json: String, dao: CardDao, store: CardsVersionStore) =
        CardsImporter(fonte = { json }, cardDao = dao, versionStore = store)

    @Test
    fun `asset mais novo que o banco importa e salva a versao`() = runTest {
        val dao = FakeCardDao()
        val store = FakeCardsVersionStore(versao = 0)

        val resultado = importer(jsonComCards(versao = 1), dao, store).importarSeNecessario()

        assertEquals(ResultadoImportacao.Importado(quantidade = 2, versao = 1), resultado)
        assertEquals(2, dao.contar())
        assertEquals(1, store.versaoSalva)
    }

    @Test
    fun `asset na mesma versao do banco nao importa`() = runTest {
        val dao = FakeCardDao()
        val store = FakeCardsVersionStore(versao = 1)

        val resultado = importer(jsonComCards(versao = 1), dao, store).importarSeNecessario()

        assertEquals(ResultadoImportacao.NadaAFazer(versao = 1), resultado)
        assertEquals(0, dao.contar())
        assertNull(store.versaoSalva)
    }

    @Test
    fun `reimportacao limpa a tabela antes de inserir os cards novos`() = runTest {
        val dao = FakeCardDao()
        dao.cards += CardEntity(
            id = "antigo-01",
            type = "COISA",
            category = "LIVRE",
            answer = "ANTIGO",
            clues = List(10) { "dica ${it + 1}" },
        )
        val store = FakeCardsVersionStore(versao = 1)

        val resultado = importer(jsonComCards(versao = 2, quantidade = 3), dao, store).importarSeNecessario()

        assertEquals(ResultadoImportacao.Importado(quantidade = 3, versao = 2), resultado)
        assertEquals(listOf("teste-1", "teste-2", "teste-3"), dao.buscarTodas().map { it.id })
        assertEquals(2, store.versaoSalva)
    }
}
