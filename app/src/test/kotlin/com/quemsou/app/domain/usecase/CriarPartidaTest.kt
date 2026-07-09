package com.quemsou.app.domain.usecase

import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.EstadoDoBaralho
import com.quemsou.app.domain.model.Jogador
import com.quemsou.app.domain.model.RegrasPartida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CriarPartidaTest {

    private val jogadores = listOf(
        Jogador(id = "j1", nome = "Ana"),
        Jogador(id = "j2", nome = "Bia"),
    )

    private fun card(id: String, categoria: CardCategory) = Card(
        id = id,
        type = CardType.PESSOA,
        category = categoria,
        answer = "Resposta $id",
        clues = List(Card.QUANTIDADE_DE_DICAS) { "Dica ${it + 1}" },
    )

    private fun baralho(id: String, categoria: CardCategory, cards: List<Card>) = Baralho(
        id = id,
        nome = "Baralho $id",
        categoria = categoria,
        versao = 1,
        estado = EstadoDoBaralho.FINALIZADO,
        cards = cards,
    )

    private val cinema = baralho(
        id = "cinema-1",
        categoria = CardCategory.PERSONAGEM_FILME,
        cards = List(10) { card("pf_%03d".format(it + 1), CardCategory.PERSONAGEM_FILME) },
    )

    private val musica = baralho(
        id = "musica-1",
        categoria = CardCategory.MUNDO_DA_MUSICA,
        cards = List(10) { card("mm_%03d".format(it + 1), CardCategory.MUNDO_DA_MUSICA) },
    )

    private fun criar(
        codigo: String,
        baralhos: List<Baralho> = listOf(cinema, musica),
    ) = CriarPartida.executar(
        codigo = codigo,
        jogadores = jogadores,
        regras = RegrasPartida(),
        baralhosSelecionados = baralhos,
    )

    @Test
    fun `mesmo codigo e mesma selecao geram a mesma partida`() {
        val primeira = criar("LOBO")
        val segunda = criar("lobo") // seed normaliza trim + uppercase

        assertEquals(primeira.seed, segunda.seed)
        assertEquals(primeira.monte, segunda.monte)
    }

    @Test
    fun `codigos diferentes geram montes diferentes`() {
        assertNotEquals(criar("LOBO").monte, criar("GATO").monte)
    }

    @Test
    fun `selecao em ordens diferentes gera o mesmo monte embaralhado`() {
        // A união determinística ordena por chave estável antes do
        // embaralhamento: a ordem da seleção (ou do download) não muda nada.
        assertEquals(
            criar("LOBO", baralhos = listOf(cinema, musica)).monte,
            criar("LOBO", baralhos = listOf(musica, cinema)).monte,
        )
    }

    @Test
    fun `monte e permutacao da uniao dos baralhos selecionados`() {
        val monte = criar("LOBO").monte

        val idsEsperados = (cinema.cards + musica.cards).map { it.id }.sorted()
        assertEquals(idsEsperados, monte.map { it.id }.sorted())
    }

    @Test
    fun `selecao de um unico baralho monta a partida so com os cards dele`() {
        val monte = criar("LOBO", baralhos = listOf(cinema)).monte

        assertEquals(cinema.cards.map { it.id }.sorted(), monte.map { it.id }.sorted())
    }

    @Test
    fun `sem grupos informados cada jogador nasce em grupo proprio de 1`() {
        val partida = criar("LOBO")

        assertEquals(listOf("j1", "j2"), partida.grupos.map { it.id })
        assertEquals(listOf("Ana", "Bia"), partida.grupos.map { it.nome })
    }
}
