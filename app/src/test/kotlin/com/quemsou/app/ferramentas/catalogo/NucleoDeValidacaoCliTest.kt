package com.quemsou.app.ferramentas.catalogo

import com.quemsou.app.domain.model.Card
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NucleoDeValidacaoCliTest {

    @get:Rule
    val pasta = TemporaryFolder()

    private fun jsonDeCard(
        id: String = "c1",
        resposta: String = "Resposta $id",
        dicas: List<String> = List(Card.QUANTIDADE_DE_DICAS) { "dica ${it + 1}" },
    ) = """
        {
          "id": "$id",
          "type": "PESSOA",
          "answer": "$resposta",
          "clues": [${dicas.joinToString(", ") { "\"$it\"" }}]
        }
    """.trimIndent()

    private fun jsonDeBaralho(id: String = "baralho-1", versao: Int = 1, cards: String = jsonDeCard()) = """
        {
          "id": "$id",
          "nome": "Baralho de Teste",
          "categoria": "PERSONAGEM_FILME",
          "colecao": { "id": "teste", "nome": "Teste", "icone": "🧪" },
          "versao": $versao,
          "estado": "EM_DESENVOLVIMENTO",
          "cards": [$cards]
        }
    """.trimIndent()

    private fun jsonDeIndice(
        id: String = "baralho-1",
        versao: Int = 1,
        quantidadeDeCards: Int = 1,
        tamanhoEmBytes: Long? = null,
    ) = """
        {
          "baralhos": [
            {
              "id": "$id",
              "nome": "Baralho de Teste",
              "categoria": "PERSONAGEM_FILME",
              "colecao": { "id": "teste", "nome": "Teste", "icone": "🧪" },
              "versao": $versao,
              "estado": "EM_DESENVOLVIMENTO",
              "quantidadeDeCards": $quantidadeDeCards,
              "url": "https://exemplo.dev/baralhos/$id.json",
              "descricao": ""${tamanhoEmBytes?.let { ", \"tamanhoEmBytes\": $it" }.orEmpty()}
            }
          ]
        }
    """.trimIndent()

    // region validarArquivoDeBaralho

    @Test
    fun `baralho valido eh aprovado`() {
        val arquivo = pasta.newFile("valido.json")
        arquivo.writeText(jsonDeBaralho())

        val resultado = validarArquivoDeBaralho(arquivo)

        assertTrue(resultado.aprovado)
        assertEquals(1, resultado.baralho?.quantidadeDeCards)
    }

    @Test
    fun `arquivo inexistente vira violacao sem lancar excecao`() {
        val resultado = validarArquivoDeBaralho(File(pasta.root, "nao-existe.json"))

        assertFalse(resultado.aprovado)
        assertTrue(resultado.violacoes.single().contains("não encontrado"))
    }

    @Test
    fun `violacao estrutural do parser aparece na lista com o caminho do card`() {
        val arquivo = pasta.newFile("estrutural.json")
        arquivo.writeText(jsonDeBaralho(cards = jsonDeCard(dicas = List(8) { "dica ${it + 1}" })))

        val resultado = validarArquivoDeBaralho(arquivo)

        assertFalse(resultado.aprovado)
        assertTrue(resultado.violacoes.single().contains("cards[0].clues"))
    }

    @Test
    fun `dica que nomeia a resposta reprova mesmo com estrutura valida`() {
        val arquivo = pasta.newFile("editorial.json")
        val dicas = List(Card.QUANTIDADE_DE_DICAS) { if (it == 2) "É o próprio Godzilla" else "dica ${it + 1}" }
        arquivo.writeText(jsonDeBaralho(cards = jsonDeCard(id = "c1", resposta = "Godzilla", dicas = dicas)))

        val resultado = validarArquivoDeBaralho(arquivo)

        assertFalse(resultado.aprovado)
        assertTrue(resultado.violacoes.single().contains("nomeia a resposta"))
        // Estrutura passou: o baralho parseado continua disponível p/ o cruzamento do catálogo.
        assertEquals(1, resultado.baralho?.quantidadeDeCards)
    }

    // endregion

    // region validarPastaDoCatalogo

    @Test
    fun `catalogo consistente eh aprovado`() {
        pasta.newFile("indice.json").writeText(jsonDeIndice())
        pasta.newFolder("baralhos")
        File(pasta.root, "baralhos/baralho-1.json").writeText(jsonDeBaralho())

        val resultado = validarPastaDoCatalogo(pasta.root)

        assertEquals(0, resultado.totalDeViolacoes)
    }

    @Test
    fun `versao do indice divergente do arquivo vira violacao cruzada`() {
        pasta.newFile("indice.json").writeText(jsonDeIndice(versao = 2))
        pasta.newFolder("baralhos")
        File(pasta.root, "baralhos/baralho-1.json").writeText(jsonDeBaralho(versao = 1))

        val resultado = validarPastaDoCatalogo(pasta.root)

        assertTrue(
            resultado.violacoesCruzadas.any { it.contains("versão 2 no índice") && it.contains("1 no arquivo") },
        )
    }

    @Test
    fun `quantidade declarada divergente da real vira violacao cruzada`() {
        pasta.newFile("indice.json").writeText(jsonDeIndice(quantidadeDeCards = 5))
        pasta.newFolder("baralhos")
        File(pasta.root, "baralhos/baralho-1.json").writeText(jsonDeBaralho())

        val resultado = validarPastaDoCatalogo(pasta.root)

        assertTrue(resultado.violacoesCruzadas.any { it.contains("declara 5 card(s)") })
    }

    @Test
    fun `tamanho declarado igual aos bytes UTF8 do arquivo eh aprovado`() {
        pasta.newFolder("baralhos")
        val arquivo = File(pasta.root, "baralhos/baralho-1.json")
        arquivo.writeText(jsonDeBaralho().replace("Baralho de Teste", "Baralho de ação 🧪"), Charsets.UTF_8)
        assertTrue(arquivo.length() > arquivo.readText(Charsets.UTF_8).length)
        pasta.newFile("indice.json").writeText(jsonDeIndice(tamanhoEmBytes = arquivo.length()))

        assertEquals(0, validarPastaDoCatalogo(pasta.root).totalDeViolacoes)
    }

    @Test
    fun `tamanho declarado divergente dos bytes reais reprova catalogo`() {
        pasta.newFolder("baralhos")
        val arquivo = File(pasta.root, "baralhos/baralho-1.json")
        arquivo.writeText(jsonDeBaralho())
        val declarado = arquivo.length() + 7
        pasta.newFile("indice.json").writeText(jsonDeIndice(tamanhoEmBytes = declarado))

        val resultado = validarPastaDoCatalogo(pasta.root)

        assertEquals(1, resultado.totalDeViolacoes)
        assertTrue(resultado.violacoesCruzadas.single().contains("$declarado bytes no índice"))
        assertTrue(resultado.violacoesCruzadas.single().contains("${arquivo.length()} bytes no arquivo"))
    }

    @Test
    fun `tamanho zero permanece valido como metadado nao informado`() {
        pasta.newFolder("baralhos")
        File(pasta.root, "baralhos/baralho-1.json").writeText(jsonDeBaralho())
        pasta.newFile("indice.json").writeText(jsonDeIndice(tamanhoEmBytes = 0))

        assertEquals(0, validarPastaDoCatalogo(pasta.root).totalDeViolacoes)
    }

    @Test
    fun `tamanho mede espacos e quebras de linha presentes no arquivo`() {
        pasta.newFolder("baralhos")
        val arquivo = File(pasta.root, "baralhos/baralho-1.json")
        val json = jsonDeBaralho()
        arquivo.writeText(json.replace("\n", "\r\n") + "\r\n", Charsets.UTF_8)
        val tamanhoSemFormatacaoExtra = json.toByteArray(Charsets.UTF_8).size.toLong()
        pasta.newFile("indice.json").writeText(jsonDeIndice(tamanhoEmBytes = tamanhoSemFormatacaoExtra))

        assertEquals(1, validarPastaDoCatalogo(pasta.root).totalDeViolacoes)
    }

    @Test
    fun `entrada do indice sem arquivo correspondente vira violacao`() {
        pasta.newFile("indice.json").writeText(jsonDeIndice(id = "fantasma"))
        pasta.newFolder("baralhos")

        val resultado = validarPastaDoCatalogo(pasta.root)

        assertTrue(resultado.violacoesCruzadas.single().contains("não existe"))
    }

    @Test
    fun `arquivo orfao sem entrada no indice vira violacao`() {
        pasta.newFile("indice.json").writeText("""{ "baralhos": [] }""")
        pasta.newFolder("baralhos")
        File(pasta.root, "baralhos/orfao.json").writeText(jsonDeBaralho(id = "orfao"))

        val resultado = validarPastaDoCatalogo(pasta.root)

        assertTrue(resultado.violacoesCruzadas.single().contains("não tem entrada correspondente"))
    }

    @Test
    fun `indice ausente vira violacao sem lancar excecao`() {
        pasta.newFolder("baralhos")

        val resultado = validarPastaDoCatalogo(pasta.root)

        assertTrue(resultado.violacoesDoIndice.single().contains("não encontrado"))
    }

    // endregion
}
