package com.quemsou.app.data.importer

import com.quemsou.app.data.catalogo.ParserDoCatalogo
import com.quemsou.app.data.catalogo.ResultadoDoParse
import com.quemsou.app.data.catalogo.paraJsonModelo
import com.quemsou.app.domain.model.*
import com.quemsou.app.domain.rules.AcervoDeRespostas
import com.quemsou.app.domain.rules.SelecionadorDeDicas
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** O APK só leva um envelope vazio. Conteúdo editorial mora fora do Git. */
class BaralhoDeAssetsTest {
    @Test fun `apk nao embarca respostas nem dicas de producao`() {
        val texto = File("src/main/assets/cards.json").readText()
        val envelope = CardsJson.deJson(texto)
        assertEquals(8, envelope.version)
        assertTrue(envelope.baralhos.isEmpty())
        val objeto = kotlinx.serialization.json.Json.parseToJsonElement(texto)
            as kotlinx.serialization.json.JsonObject
        assertEquals(setOf("version", "baralhos"), objeto.keys)
    }

    @Test fun `pacotes ficticios nao reciclam dicas consumidas nem limitam o acervo unificado`() {
        val parser = ParserDoCatalogo()
        val pacotes = (0..5).map { pacote ->
            val dicas = (0..99).map {
                DicaDoBanco("p$pacote-d$it", "Fato sintético do pacote $pacote número $it")
            }
            Baralho(
                id = "pacote-$pacote", nome = "Pacote fictício $pacote",
                categoria = CardCategory.ESPECIAIS,
                colecao = Colecao("teste", "Teste", "🧪"), versao = 1,
                estado = EstadoDoBaralho.EM_DESENVOLVIMENTO,
                cards = listOf(Card(
                    id = "referencia-$pacote", type = CardType.COISA,
                    category = CardCategory.ESPECIAIS, answer = "Objeto Imaginário",
                    respostaId = "identidade-unica", clues = dicas.take(10).map { it.texto },
                    bancoDeDicas = dicas,
                )),
            ).also { assertTrue(parser.validarBaralho(it.paraJsonModelo()) is ResultadoDoParse.Sucesso) }
        }
        val unificado = AcervoDeRespostas.compartilhar(pacotes).first().cards.single()
        assertEquals(600, unificado.bancoDeDicas.size)
        val primeira = SelecionadorDeDicas.preparar(unificado, emptySet(), 42)!!
        val consumidas = primeira.bancoDeDicas.take(3).flatMap { SelecionadorDeDicas.chaves(primeira, it) }.toSet()
        assertEquals(597, SelecionadorDeDicas.disponiveis(unificado, consumidas).size)
        val segunda = SelecionadorDeDicas.preparar(unificado, consumidas, 42)!!
        assertEquals(10, segunda.clues.size)
        assertTrue(segunda.bancoDeDicas.none { it in primeira.bancoDeDicas.take(3) })
    }
}
