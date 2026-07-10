package com.quemsou.app.domain.validacao

import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.Colecao
import com.quemsou.app.domain.model.EstadoDoBaralho
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorDeBaralhoTest {

    private val validador = ValidadorDeBaralho()

    private fun card(id: String, categoria: CardCategory = CardCategory.PERSONAGEM_FILME) = Card(
        id = id,
        type = CardType.PESSOA,
        category = categoria,
        answer = "Resposta $id",
        clues = List(Card.QUANTIDADE_DE_DICAS) { "Dica ${it + 1} de $id" },
    )

    private fun baralho(
        cards: List<Card>,
        categoria: CardCategory = CardCategory.PERSONAGEM_FILME,
    ) = Baralho(
        id = "b1",
        nome = "Baralho 1",
        categoria = categoria,
        colecao = Colecao(id = "colecao-teste", nome = "Coleção de Teste", icone = "🧪"),
        versao = 1,
        estado = EstadoDoBaralho.EM_DESENVOLVIMENTO,
        cards = cards,
    )

    private fun cards(quantidade: Int) = List(quantidade) { card("c%03d".format(it + 1)) }

    private fun violacoesDe(resultado: ResultadoValidacaoDeBaralho): List<RegraDeBaralho> =
        (resultado as ResultadoValidacaoDeBaralho.Reprovado).violacoes.map { it.regra }

    @Test
    fun `baralho valido e aprovado`() {
        assertEquals(ResultadoValidacaoDeBaralho.Aprovado, validador.validar(baralho(cards(3))))
    }

    @Test
    fun `teto de 100 - 99 e 100 passam, 101 reprova com mensagem legivel`() {
        assertEquals(ResultadoValidacaoDeBaralho.Aprovado, validador.validar(baralho(cards(99))))
        assertEquals(ResultadoValidacaoDeBaralho.Aprovado, validador.validar(baralho(cards(100))))

        val resultado = validador.validar(baralho(cards(101)))

        assertEquals(listOf(RegraDeBaralho.TETO_DE_CARDS_EXCEDIDO), violacoesDe(resultado))
        val mensagem = (resultado as ResultadoValidacaoDeBaralho.Reprovado).violacoes.single().mensagem
        assertTrue(mensagem.contains("101"))
        assertTrue(mensagem.contains("${Baralho.MAXIMO_DE_CARDS}"))
    }

    @Test
    fun `baralho vazio reprova`() {
        assertEquals(listOf(RegraDeBaralho.BARALHO_VAZIO), violacoesDe(validador.validar(baralho(emptyList()))))
    }

    @Test
    fun `ids de card repetidos reprovam com os ids na mensagem`() {
        val resultado = validador.validar(baralho(listOf(card("c1"), card("c2"), card("c1"))))

        assertEquals(listOf(RegraDeBaralho.IDS_DE_CARDS_REPETIDOS), violacoesDe(resultado))
        assertTrue(
            (resultado as ResultadoValidacaoDeBaralho.Reprovado).violacoes.single().mensagem.contains("c1"),
        )
    }

    @Test
    fun `categoria LIVRE reprova - livre e selecao, nao conteudo`() {
        val resultado = validador.validar(
            baralho(listOf(card("c1", CardCategory.LIVRE)), categoria = CardCategory.LIVRE),
        )

        assertEquals(listOf(RegraDeBaralho.CATEGORIA_LIVRE), violacoesDe(resultado))
    }

    @Test
    fun `card de categoria divergente reprova - pertencimento herda a categoria do baralho`() {
        val resultado = validador.validar(
            baralho(listOf(card("c1"), card("c2", CardCategory.MUNDO_DA_MUSICA))),
        )

        assertEquals(listOf(RegraDeBaralho.CARD_DE_CATEGORIA_DIVERGENTE), violacoesDe(resultado))
        assertTrue(
            (resultado as ResultadoValidacaoDeBaralho.Reprovado).violacoes.single().mensagem.contains("c2"),
        )
    }

    @Test
    fun `violacoes sao acumuladas, nao para na primeira`() {
        val resultado = validador.validar(
            baralho(
                cards = cards(99) + cards(3) + card("extra", CardCategory.MUNDO_DA_MUSICA),
            ),
        )

        val regras = violacoesDe(resultado)
        assertTrue(RegraDeBaralho.TETO_DE_CARDS_EXCEDIDO in regras)
        assertTrue(RegraDeBaralho.IDS_DE_CARDS_REPETIDOS in regras)
        assertTrue(RegraDeBaralho.CARD_DE_CATEGORIA_DIVERGENTE in regras)
    }
}
