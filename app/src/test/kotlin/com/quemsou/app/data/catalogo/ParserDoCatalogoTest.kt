package com.quemsou.app.data.catalogo

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.EstadoDoBaralho
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserDoCatalogoTest {

    private val parser = ParserDoCatalogo()

    private fun jsonDeCard(id: String = "c1", dicas: Int = Card.QUANTIDADE_DE_DICAS) = """
        {
          "id": "$id",
          "type": "PESSOA",
          "answer": "Resposta $id",
          "clues": [${List(dicas) { "\"dica ${it + 1}\"" }.joinToString(", ")}]
        }
    """.trimIndent()

    private fun jsonDeBaralho(
        categoria: String = "PERSONAGEM_FILME",
        estado: String = "FINALIZADO",
        cards: String = jsonDeCard(),
    ) = """
        {
          "id": "cinema-1",
          "nome": "Cinema — Edição 1",
          "categoria": "$categoria",
          "versao": 1,
          "estado": "$estado",
          "cards": [$cards]
        }
    """.trimIndent()

    private fun violacoesDe(resultado: ResultadoDoParse<*>): List<ViolacaoDeFormato> =
        (resultado as ResultadoDoParse.Falha).violacoes

    // region Baralho

    @Test
    fun `baralho valido vira dominio com a categoria herdada pelos cards`() {
        val resultado = parser.parseBaralho(jsonDeBaralho())

        val baralho = (resultado as ResultadoDoParse.Sucesso).valor
        assertEquals("cinema-1", baralho.id)
        assertEquals(CardCategory.PERSONAGEM_FILME, baralho.categoria)
        assertEquals(EstadoDoBaralho.FINALIZADO, baralho.estado)
        assertEquals(1, baralho.quantidadeDeCards)
        // O card não declara categoria no JSON: herda a do baralho.
        assertEquals(CardCategory.PERSONAGEM_FILME, baralho.cards.single().category)
        assertEquals(CardType.PESSOA, baralho.cards.single().type)
    }

    @Test
    fun `card com 8 dicas vira violacao legivel com o id do card, nao excecao`() {
        val resultado = parser.parseBaralho(jsonDeBaralho(cards = jsonDeCard(id = "c9", dicas = 8)))

        val violacao = violacoesDe(resultado).single()
        assertEquals("cards[0].clues", violacao.caminho)
        assertTrue(violacao.mensagem.contains("c9"))
        assertTrue(violacao.mensagem.contains("8"))
    }

    @Test
    fun `json malformado vira violacao legivel, nao excecao`() {
        val resultado = parser.parseBaralho("{ isto nao é json ")

        assertTrue(violacoesDe(resultado).single().mensagem.startsWith("JSON inválido"))
    }

    @Test
    fun `categoria e estado desconhecidos viram violacoes legiveis`() {
        val resultado = parser.parseBaralho(jsonDeBaralho(categoria = "XADREZ", estado = "RASCUNHO"))

        val mensagens = violacoesDe(resultado).map { it.mensagem }
        assertEquals(2, mensagens.size)
        assertTrue(mensagens.any { it.contains("XADREZ") })
        assertTrue(mensagens.any { it.contains("RASCUNHO") })
    }

    @Test
    fun `violacoes de varios cards sao acumuladas`() {
        val cards = listOf(
            jsonDeCard(id = "c1", dicas = 9),
            jsonDeCard(id = "c2"),
            jsonDeCard(id = "c3", dicas = 11),
        ).joinToString(",\n")

        val resultado = parser.parseBaralho(jsonDeBaralho(cards = cards))

        val caminhos = violacoesDe(resultado).map { it.caminho }
        assertEquals(listOf("cards[0].clues", "cards[2].clues"), caminhos)
    }

    @Test
    fun `baralho acima do teto de 100 reprova no parse com mensagem legivel`() {
        val cards = List(101) { jsonDeCard(id = "c${it + 1}") }.joinToString(",\n")

        val resultado = parser.parseBaralho(jsonDeBaralho(cards = cards))

        assertTrue(violacoesDe(resultado).single().mensagem.contains("101"))
    }

    @Test
    fun `categoria LIVRE em baralho reprova no parse`() {
        val resultado = parser.parseBaralho(jsonDeBaralho(categoria = "LIVRE"))

        assertTrue(violacoesDe(resultado).single().mensagem.contains("LIVRE"))
    }

    // endregion

    // region Índice

    @Test
    fun `indice valido vira lista de entradas com enums reais`() {
        val json = """
            {
              "baralhos": [
                {
                  "id": "cinema-1",
                  "nome": "Cinema — Edição 1",
                  "categoria": "PERSONAGEM_FILME",
                  "versao": 2,
                  "estado": "EM_DESENVOLVIMENTO",
                  "quantidadeDeCards": 30,
                  "url": "https://exemplo.dev/baralhos/cinema-1.json",
                  "descricao": "Personagens clássicos do cinema."
                }
              ]
            }
        """.trimIndent()

        val entradas = (parser.parseIndice(json) as ResultadoDoParse.Sucesso).valor

        val entrada = entradas.single()
        assertEquals("cinema-1", entrada.id)
        assertEquals(CardCategory.PERSONAGEM_FILME, entrada.categoria)
        assertEquals(EstadoDoBaralho.EM_DESENVOLVIMENTO, entrada.estado)
        assertEquals(2, entrada.versao)
        assertEquals(30, entrada.quantidadeDeCards)
    }

    @Test
    fun `indice com entrada invalida acumula violacoes com o caminho da entrada`() {
        val json = """
            {
              "baralhos": [
                {
                  "id": "cinema-1",
                  "nome": "Cinema — Edição 1",
                  "categoria": "PERSONAGEM_FILME",
                  "versao": 1,
                  "estado": "FINALIZADO",
                  "quantidadeDeCards": 30,
                  "url": "https://exemplo.dev/baralhos/cinema-1.json",
                  "descricao": ""
                },
                {
                  "id": "quebrado-1",
                  "nome": "",
                  "categoria": "XADREZ",
                  "versao": 0,
                  "estado": "FINALIZADO",
                  "quantidadeDeCards": 10,
                  "url": "",
                  "descricao": ""
                }
              ]
            }
        """.trimIndent()

        val violacoes = violacoesDe(parser.parseIndice(json))

        assertTrue(violacoes.isNotEmpty())
        assertTrue(violacoes.all { it.caminho.startsWith("baralhos[1]") })
    }

    @Test
    fun `indice sem campo obrigatorio vira violacao legivel, nao excecao`() {
        val semVersao = """{ "baralhos": [ { "id": "x", "nome": "X" } ] }"""

        val resultado = parser.parseIndice(semVersao)

        assertTrue(violacoesDe(resultado).single().mensagem.startsWith("JSON inválido"))
    }

    // endregion
}
