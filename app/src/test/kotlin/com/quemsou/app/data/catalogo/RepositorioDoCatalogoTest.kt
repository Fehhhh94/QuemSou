package com.quemsou.app.data.catalogo

import com.quemsou.app.data.local.BaralhoDao
import com.quemsou.app.data.local.BaralhoEntity
import com.quemsou.app.data.local.CardDao
import com.quemsou.app.data.local.CardEntity
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositorioDoCatalogoTest {

    // region Fakes — nenhum teste bate na rede real

    private class FonteFake(
        var indice: String? = null,
        var baralho: String? = null,
    ) : FonteDoCatalogo {
        override suspend fun buscarIndice(): String =
            indice ?: throw IOException("sem rede (fake)")

        override suspend fun baixarBaralho(url: String, aoProgresso: (Float) -> Unit): String {
            val conteudo = baralho ?: throw IOException("sem rede (fake)")
            aoProgresso(0.5f)
            aoProgresso(1f)
            return conteudo
        }
    }

    private class CacheFake(var conteudo: String? = null) : CacheDoIndice {
        override suspend fun ler(): String? = conteudo

        override suspend fun salvar(conteudo: String) {
            this.conteudo = conteudo
        }
    }

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

    // endregion

    private fun entradaJson(id: String, versao: Int = 1) = """
        {
          "id": "$id",
          "nome": "Baralho $id",
          "categoria": "PERSONAGEM_FILME",
          "colecao": { "id": "cinema", "nome": "Cinema", "icone": "🎬" },
          "versao": $versao,
          "estado": "EM_DESENVOLVIMENTO",
          "quantidadeDeCards": 1,
          "url": "https://exemplo.dev/$id.json",
          "descricao": "Baralho de teste."
        }
    """.trimIndent()

    private fun indiceJson(vararg entradas: String) =
        """{ "baralhos": [ ${entradas.joinToString(",\n")} ] }"""

    private fun baralhoJson(id: String, dicas: Int = 10) = """
        {
          "id": "$id",
          "nome": "Baralho $id",
          "categoria": "PERSONAGEM_FILME",
          "colecao": { "id": "cinema", "nome": "Cinema", "icone": "🎬" },
          "versao": 1,
          "estado": "EM_DESENVOLVIMENTO",
          "cards": [
            {
              "id": "$id-c1",
              "type": "PESSOA",
              "answer": "Resposta",
              "clues": [${List(dicas) { "\"dica ${it + 1}\"" }.joinToString(", ")}]
            }
          ]
        }
    """.trimIndent()

    private fun entidadeLocal(id: String, versao: Int) = BaralhoEntity(
        id = id,
        nome = "Baralho $id",
        categoria = "PERSONAGEM_FILME",
        versao = versao,
        estado = "EM_DESENVOLVIMENTO",
        colecaoId = "cinema",
        colecaoNome = "Cinema",
        colecaoIcone = "🎬",
    )

    private fun repositorio(
        fonte: FonteDoCatalogo,
        cache: CacheDoIndice = CacheFake(),
        baralhoDao: BaralhoDao = FakeBaralhoDao(),
        cardDao: CardDao = FakeCardDao(),
    ) = RepositorioDoCatalogo(fonte, cache, ParserDoCatalogo(), baralhoDao, cardDao)

    @Test
    fun `indice da rede deriva os tres estados locais e salva o cache`() = runTest {
        val cache = CacheFake()
        val baralhoDao = FakeBaralhoDao()
        baralhoDao.baralhos += entidadeLocal("baixado-1", versao = 1)
        baralhoDao.baralhos += entidadeLocal("desatualizado-1", versao = 1)
        val indice = indiceJson(
            entradaJson("baixado-1", versao = 1),
            entradaJson("desatualizado-1", versao = 2),
            entradaJson("novo-1", versao = 1),
        )

        val resultado = repositorio(FonteFake(indice = indice), cache, baralhoDao).carregarCatalogo()

        val catalogo = (resultado as ResultadoDoCatalogo.Disponivel).catalogo
        assertEquals(false, catalogo.offline)
        assertEquals(indice, cache.conteudo)
        val estadoPorId = catalogo.itens.associate { it.entrada.id to it.estadoLocal }
        assertEquals(EstadoLocalDoBaralho.BAIXADO, estadoPorId["baixado-1"])
        assertEquals(EstadoLocalDoBaralho.ATUALIZACAO_DISPONIVEL, estadoPorId["desatualizado-1"])
        assertEquals(EstadoLocalDoBaralho.NAO_BAIXADO, estadoPorId["novo-1"])
    }

    @Test
    fun `sem rede o catalogo vem do cache com a flag offline`() = runTest {
        val cache = CacheFake(conteudo = indiceJson(entradaJson("b1")))

        val resultado = repositorio(FonteFake(indice = null), cache).carregarCatalogo()

        val catalogo = (resultado as ResultadoDoCatalogo.Disponivel).catalogo
        assertTrue(catalogo.offline)
        assertEquals(listOf("b1"), catalogo.itens.map { it.entrada.id })
    }

    @Test
    fun `sem rede e sem cache o catalogo fica indisponivel`() = runTest {
        val resultado = repositorio(FonteFake(indice = null), CacheFake(conteudo = null)).carregarCatalogo()

        assertEquals(ResultadoDoCatalogo.Indisponivel, resultado)
    }

    @Test
    fun `indice remoto invalido cai para o cache valido`() = runTest {
        val cache = CacheFake(conteudo = indiceJson(entradaJson("b1")))

        val resultado = repositorio(FonteFake(indice = "{ quebrado "), cache).carregarCatalogo()

        val catalogo = (resultado as ResultadoDoCatalogo.Disponivel).catalogo
        assertTrue(catalogo.offline)
        // O cache não foi sobrescrito pelo conteúdo quebrado.
        assertEquals(indiceJson(entradaJson("b1")), cache.conteudo)
    }

    @Test
    fun `download valido grava baralho e cards no Room e reporta progresso`() = runTest {
        val baralhoDao = FakeBaralhoDao()
        val cardDao = FakeCardDao()
        val progresso = mutableListOf<Float>()
        val entrada = entradaDe("b1")

        val resultado = repositorio(
            FonteFake(baralho = baralhoJson("b1")),
            baralhoDao = baralhoDao,
            cardDao = cardDao,
        ).baixarBaralho(entrada) { progresso += it }

        assertTrue(resultado is ResultadoDoDownload.Sucesso)
        assertEquals(listOf("b1"), baralhoDao.baralhos.map { it.id })
        assertEquals(listOf("b1-c1"), cardDao.cards.map { it.id })
        assertEquals(listOf(0.5f, 1f), progresso)
    }

    @Test
    fun `baralho invalido NUNCA entra no Room e a falha e legivel`() = runTest {
        val baralhoDao = FakeBaralhoDao()
        val cardDao = FakeCardDao()

        val resultado = repositorio(
            FonteFake(baralho = baralhoJson("b1", dicas = 8)),
            baralhoDao = baralhoDao,
            cardDao = cardDao,
        ).baixarBaralho(entradaDe("b1")) {}

        val falha = resultado as ResultadoDoDownload.Falha
        assertTrue(falha.mensagem.contains("8"))
        assertTrue(baralhoDao.baralhos.isEmpty())
        assertTrue(cardDao.cards.isEmpty())
    }

    @Test
    fun `baralho com id divergente da entrada e recusado`() = runTest {
        val baralhoDao = FakeBaralhoDao()

        val resultado = repositorio(
            FonteFake(baralho = baralhoJson("outro-1")),
            baralhoDao = baralhoDao,
        ).baixarBaralho(entradaDe("b1")) {}

        assertTrue(resultado is ResultadoDoDownload.Falha)
        assertTrue(baralhoDao.baralhos.isEmpty())
    }

    @Test
    fun `sem rede o download falha com mensagem legivel sem tocar no Room`() = runTest {
        val baralhoDao = FakeBaralhoDao()

        val resultado = repositorio(FonteFake(baralho = null), baralhoDao = baralhoDao)
            .baixarBaralho(entradaDe("b1")) {}

        val falha = resultado as ResultadoDoDownload.Falha
        assertTrue(falha.mensagem.contains("conexão"))
        assertTrue(baralhoDao.baralhos.isEmpty())
    }

    private fun entradaDe(id: String): EntradaDoCatalogo {
        val resultado = ParserDoCatalogo().parseIndice(indiceJson(entradaJson(id)))
        return (resultado as ResultadoDoParse.Sucesso).valor.single()
    }
}
