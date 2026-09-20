package com.quemsou.app.data.catalogo

import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FonteDoCatalogoFirestoreTest {

    private class ClienteFake(
        override val configurado: Boolean = true,
        var manifestos: List<ManifestoDoCatalogoNaNuvem> = emptyList(),
        var versao: VersaoDoBaralhoNaNuvem? = null,
        var blocos: List<BlocoDeCardsNaNuvem> = emptyList(),
    ) : ClienteDoCatalogoNaNuvem {
        override suspend fun listarManifestos() = manifestos

        override suspend fun buscarVersao(baralhoId: String, versaoId: String) =
            checkNotNull(versao)

        override suspend fun buscarBlocos(baralhoId: String, versaoId: String) = blocos
    }

    private fun manifesto(id: String = "tema-1", versao: Int = 2) =
        ManifestoDoCatalogoNaNuvem(
            id = id,
            nome = "Tema",
            categoria = "PERSONAGEM_FILME",
            colecaoId = "cinema",
            colecaoNome = "Cinema",
            colecaoIcone = "🎬",
            versao = versao,
            versaoId = "v$versao",
            estado = "EM_DESENVOLVIMENTO",
            quantidadeDeCards = 2,
            quantidadeDeBlocos = 2,
            descricao = "Descrição",
            tamanhoEmBytes = 1234,
            hashDoConteudo = "a".repeat(64),
            visibilidade = "PUBLICO",
        )

    private fun versao(id: String = "tema-1", hash: String) = VersaoDoBaralhoNaNuvem(
        id = id,
        nome = "Tema",
        categoria = "PERSONAGEM_FILME",
        colecaoId = "cinema",
        colecaoNome = "Cinema",
        colecaoIcone = "🎬",
        versao = 2,
        estado = "EM_DESENVOLVIMENTO",
        quantidadeDeCards = 2,
        quantidadeDeBlocos = 2,
        hashDoConteudo = hash,
    )

    private fun card(id: String) = CardDoBaralhoJson(
        id = id,
        type = "PESSOA",
        answer = "Resposta $id",
        clues = List(10) { "Dica ${it + 1} de $id" },
    )

    private fun sha256(texto: String): String = MessageDigest.getInstance("SHA-256")
        .digest(texto.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun bloco(ordem: Int, card: CardDoBaralhoJson): BlocoDeCardsNaNuvem {
        val conteudo = Json.encodeToString(listOf(card))
        return BlocoDeCardsNaNuvem(ordem, 1, conteudo, sha256(conteudo))
    }

    private fun conteudoCompleto(cards: List<CardDoBaralhoJson>): String = Json.encodeToString(
        BaralhoJson(
            id = "tema-1",
            nome = "Tema",
            categoria = "PERSONAGEM_FILME",
            colecao = ColecaoJson("cinema", "Cinema", "🎬"),
            versao = 2,
            estado = "EM_DESENVOLVIMENTO",
            cards = cards,
        ),
    )

    @Test
    fun `indice do Firestore preserva metadados e usa endereco opaco`() = runTest {
        val cliente = ClienteFake(manifestos = listOf(manifesto()))

        val bruto = FonteDoCatalogoFirestore(cliente).buscarIndice()
        val indice = Json.decodeFromString<IndiceDoCatalogoJson>(bruto)

        val entrada = indice.baralhos.single()
        assertEquals("tema-1", entrada.id)
        assertEquals(2, entrada.versao)
        assertEquals("Descrição", entrada.descricao)
        assertEquals("firestore://catalogo/tema-1/versoes/v2", entrada.url)
    }

    @Test
    fun `manifesto incoerente nao substitui o ultimo indice valido`() = runTest {
        val cliente = ClienteFake(
            manifestos = listOf(manifesto().copy(versaoId = "v3")),
        )

        val falha = try {
            FonteDoCatalogoFirestore(cliente).buscarIndice()
            null
        } catch (erro: IOException) {
            erro
        }

        assertTrue(falha?.message!!.contains("versão inválida"))
    }

    @Test
    fun `serializacao do hash coincide com o JSON canonico produzido pelo painel`() {
        val conteudo = conteudoCompleto(listOf(card("c1")))

        assertEquals(
            "7324c51353114025df39d94cc15e8fa7d04514496c59915a1c6137d8b13b20d9",
            sha256(conteudo),
        )
    }

    @Test
    fun `download remonta blocos ordenados e preserva todo o card`() = runTest {
        val cards = listOf(card("c1"), card("c2"))
        val cliente = ClienteFake(
            versao = versao(hash = sha256(conteudoCompleto(cards))),
            blocos = listOf(
                bloco(1, cards[1]),
                bloco(0, cards[0]),
            ),
        )
        val progresso = mutableListOf<Float>()

        val bruto = FonteDoCatalogoFirestore(cliente).baixarBaralho(
            "firestore://catalogo/tema-1/versoes/v2",
        ) { progresso += it }
        val baralho = Json.decodeFromString<BaralhoJson>(bruto)

        assertEquals(listOf("c1", "c2"), baralho.cards.map { it.id })
        assertEquals(listOf(0.5f, 1f), progresso)
        assertEquals(2, baralho.versao)
        assertEquals("EM_DESENVOLVIMENTO", baralho.estado)
    }

    @Test
    fun `bloco ausente recusa o snapshot antes do parser e do Room`() = runTest {
        val cards = listOf(card("c1"), card("c2"))
        val cliente = ClienteFake(
            versao = versao(hash = sha256(conteudoCompleto(cards))),
            blocos = listOf(bloco(0, cards[0])),
        )

        val falha = try {
            FonteDoCatalogoFirestore(cliente).baixarBaralho(
                "firestore://catalogo/tema-1/versoes/v2",
            ) {}
            null
        } catch (erro: IOException) {
            erro
        }

        assertTrue(falha?.message!!.contains("blocos"))
    }

    @Test
    fun `build sem Firebase continua falhando como rede indisponivel`() = runTest {
        val cliente = ClienteFake(configurado = false)

        val falha = try {
            FonteDoCatalogoFirestore(cliente).buscarIndice()
            null
        } catch (erro: IOException) {
            erro
        }

        assertTrue(falha?.message!!.contains("não configurado"))
    }
}
