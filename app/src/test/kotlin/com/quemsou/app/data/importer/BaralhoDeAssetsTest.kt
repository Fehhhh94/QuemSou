package com.quemsou.app.data.importer

import com.quemsou.app.data.catalogo.ParserDoCatalogo
import com.quemsou.app.data.catalogo.ResultadoDoParse
import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.Colecao
import com.quemsou.app.domain.model.EstadoDoBaralho
import com.quemsou.app.domain.validacao.ResultadoValidacao
import com.quemsou.app.domain.validacao.ValidadorEditorial
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Valida os baralhos reais embarcados em `app/src/main/assets/cards.json` —
 * garante que o conteúdo editorial nunca quebra o app em runtime. Cada
 * baralho passa pela validação completa do [ParserDoCatalogo] (estrutura +
 * `ValidadorDeBaralho`: teto de 100, ids únicos, categoria herdada) e cada
 * card pela régua do [ValidadorEditorial] — as mesmas réguas da fábrica
 * interna e do download do catálogo. O working directory dos testes JVM é o
 * diretório do módulo `app`, por isso o caminho relativo.
 */
class BaralhoDeAssetsTest {

    private val cardsJson: CardsJson by lazy {
        CardsJson.deJson(File("src/main/assets/cards.json").readText())
    }

    private val parser = ParserDoCatalogo()

    private val baralhos: List<Baralho> by lazy {
        cardsJson.baralhos.map { baralhoJson ->
            when (val resultado = parser.validarBaralho(baralhoJson)) {
                is ResultadoDoParse.Sucesso -> resultado.valor
                is ResultadoDoParse.Falha -> throw AssertionError(
                    "Baralho '${baralhoJson.id}' inválido: " +
                        resultado.violacoes.joinToString("; ") { it.mensagem },
                )
            }
        }
    }

    @Test
    fun `todos os baralhos embarcados passam na validacao completa do parser`() {
        // Estrutura + ValidadorDeBaralho (teto de 100, ids únicos por
        // baralho, categoria real e herdada) — materializar a lista valida.
        assertEquals(2, baralhos.size)
    }

    @Test
    fun `todos os cards de todos os baralhos passam na regua editorial`() {
        val validador = ValidadorEditorial()
        val reprovados = baralhos.flatMap { baralho ->
            baralho.cards.mapNotNull { card ->
                (validador.validar(card) as? ResultadoValidacao.Reprovado)
                    ?.let { reprovado -> card.id to reprovado.violacoes.map { it.mensagem } }
            }
        }

        assertTrue(
            "Cards reprovados na régua editorial: " +
                reprovados.joinToString("; ") { (id, mensagens) -> "$id → $mensagens" },
            reprovados.isEmpty(),
        )
    }

    @Test
    fun `os dois baralhos embarcados sao os esperados, finalizados e com 30 cards cada`() {
        val porId = baralhos.associateBy { it.id }

        val cinema = requireNotNull(porId["cinema-classico-1"])
        assertEquals("Cinema Clássico — Edição 1", cinema.nome)
        assertEquals(CardCategory.PERSONAGEM_FILME, cinema.categoria)
        assertEquals(EstadoDoBaralho.FINALIZADO, cinema.estado)
        assertEquals(30, cinema.quantidadeDeCards)
        assertEquals(Colecao(id = "cinema-classico", nome = "Cinema Clássico", icone = "🎬"), cinema.colecao)

        val musica = requireNotNull(porId["mundo-da-musica-1"])
        assertEquals("Mundo da Música — Edição 1", musica.nome)
        assertEquals(CardCategory.MUNDO_DA_MUSICA, musica.categoria)
        assertEquals(EstadoDoBaralho.FINALIZADO, musica.estado)
        assertEquals(30, musica.quantidadeDeCards)
        assertEquals(Colecao(id = "mundo-da-musica", nome = "Mundo da Música", icone = "🎸"), musica.colecao)
    }

    @Test
    fun `ids de baralho sao unicos e ids de card sao unicos no conjunto embarcado`() {
        val idsDeBaralho = baralhos.map { it.id }
        assertEquals(idsDeBaralho.size, idsDeBaralho.toSet().size)

        // Únicos por baralho já é regra do ValidadorDeBaralho; no conjunto
        // embarcado inteiro é qualidade de asset (evita confusão na revisão).
        val idsDeCard = baralhos.flatMap { baralho -> baralho.cards.map { it.id } }
        assertEquals(idsDeCard.size, idsDeCard.toSet().size)
    }

    @Test
    fun `nenhum baralho embarcado passa do teto de cards`() {
        baralhos.forEach { baralho ->
            assertTrue(
                "Baralho '${baralho.id}' tem ${baralho.quantidadeDeCards} cards (teto ${Baralho.MAXIMO_DE_CARDS}).",
                baralho.quantidadeDeCards <= Baralho.MAXIMO_DE_CARDS,
            )
        }
    }
}
