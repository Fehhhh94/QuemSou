package com.quemsou.app.domain.rules

import com.quemsou.app.domain.model.*
import com.quemsou.app.domain.usecase.CriarPartida
import org.junit.Assert.*
import org.junit.Test

class AcervoERotacaoTest {
    private fun card(id: String, resposta: String = id, inicio: Int = 0, total: Int = 60) = Card(
        id, CardType.PESSOA, CardCategory.PERSONAGEM_FILME, resposta,
        List(10) { "Fato ${it + inicio} de $resposta" }, resposta,
        List(total) { DicaDoBanco("d${it + inicio}", "Fato ${it + inicio} de $resposta") },
    )
    private fun baralho(id: String, cards: List<Card>) = Baralho(id, id, CardCategory.PERSONAGEM_FILME,
        Colecao("c", "Cinema", "C"), 1, EstadoDoBaralho.EM_DESENVOLVIMENTO, cards)

    @Test fun `tres reveladas deixam 57 livres incluindo as sete reservadas nao vistas`() {
        val resposta = card("hp")
        val carta = SelecionadorDeDicas.preparar(resposta, emptySet(), 4)!!
        val vistas = carta.bancoDeDicas.take(3).flatMap { SelecionadorDeDicas.chaves(carta, it) }.toSet()
        val livres = SelecionadorDeDicas.disponiveis(resposta, vistas)
        assertEquals(57, livres.size)
        assertTrue(livres.containsAll(carta.bancoDeDicas.drop(3)))
        assertTrue(SelecionadorDeDicas.preparar(resposta, vistas, 4)!!.clues.intersect(carta.clues.take(3).toSet()).isEmpty())
    }

    @Test fun `nove dicas restantes nao completam uma carta e nao sao perdidas`() {
        val resposta = card("hp", total = 12)
        val carta = SelecionadorDeDicas.preparar(resposta, emptySet(), 4)!!
        val vistas = carta.bancoDeDicas.take(3).flatMap { SelecionadorDeDicas.chaves(carta, it) }.toSet()
        assertEquals(9, SelecionadorDeDicas.disponiveis(resposta, vistas).size)
        assertNull(SelecionadorDeDicas.preparar(resposta, vistas, 5))
    }

    @Test fun `baralhos e temas compartilham acervo sem duplicar resposta na partida`() {
        val a = baralho("a", listOf(card("hp-a", "hp", total = 30)))
        val b = baralho("b", listOf(card("hp-b", "hp", inicio = 30, total = 30).copy(category = CardCategory.MUNDO_DA_MUSICA)))
        val resposta = AcervoDeRespostas.organizar(listOf(a, b)).single()
        assertEquals(60, resposta.dicas.size)
        assertEquals(2, resposta.temas.size)
        val compartilhados = AcervoDeRespostas.compartilhar(listOf(a, b))
        assertTrue(compartilhados.all { it.cards.single().bancoDeDicas.size == 60 })
        assertEquals(1, RotacaoDeRespostas.ordenar(Baralho.uniaoDeterministica(compartilhados), emptyMap(), 1).size)
        assertEquals(compartilhados, AcervoDeRespostas.compartilhar(listOf(b, a)).sortedBy { it.id })
    }

    @Test fun `aliases transitivos mantem historia de dicas e resposta`() {
        val a = card("a", "Harry Potter").copy(respostaId = "hp")
        val b = a.copy(id = "b", respostaId = "outro-id")
        val c = b.copy(id = "c", answer = "Harry James Potter")
        val lista = AcervoDeRespostas.compartilhar(listOf(baralho("a", listOf(a, b, c)))).single().cards
        assertEquals(1, AcervoDeRespostas.semRepeticoes(lista).size)
        val usadas = SelecionadorDeDicas.chaves(a, a.bancoDeDicas.first())
        assertEquals(59, SelecionadorDeDicas.disponiveis(lista.last(), usadas).size)
        assertTrue(AcervoDeRespostas.chaves(lista.last()).contains("id:hp"))
    }

    @Test fun `correcao do mesmo id usa versao recente e nao recicla dica vista`() {
        val antiga = card("a")
        val nova = antiga.copy(id = "b", bancoDeDicas = antiga.bancoDeDicas.mapIndexed { i, d -> if (i == 0) d.copy(texto = "Fato corrigido") else d })
        val compartilhados = AcervoDeRespostas.compartilhar(listOf(baralho("a", listOf(antiga)), baralho("b", listOf(nova)).copy(versao = 2)))
        val acervo = compartilhados.first().cards.single()
        assertEquals("Fato corrigido", acervo.bancoDeDicas.first().texto)
        assertEquals(59, SelecionadorDeDicas.disponiveis(acervo, SelecionadorDeDicas.chaves(antiga, antiga.bancoDeDicas.first())).size)
    }

    @Test fun `quarenta respostas rendem quatro partidas completas antes de repetir`() {
        val cards = List(40) { card("r$it") }
        val historico = mutableMapOf<String, Long>()
        val vistas = mutableListOf<String>()
        var ordem = 0L
        repeat(4) { rodada ->
            val selecionadas = RotacaoDeRespostas.ordenar(cards, historico, rodada.toLong()).take(10)
            assertTrue(selecionadas.none { it.id in vistas })
            selecionadas.forEach { c -> AcervoDeRespostas.chaves(c).forEach { historico[it] = ++ordem }; vistas.add(c.id) }
        }
        assertEquals(40, vistas.toSet().size)
        val seguinte = RotacaoDeRespostas.ordenar(cards, historico, 98).take(10)
        assertEquals(vistas.take(10).toSet(), seguinte.map { it.id }.toSet())
    }

    @Test fun `conteudo novo tem prioridade e respostas recentes ficam no fim`() {
        val cards = List(4) { card("r$it") }
        val historico = cards.take(3).flatMapIndexed { i, c -> AcervoDeRespostas.chaves(c).map { it to i + 1L } }.toMap()
        assertEquals(listOf("r3", "r0", "r1", "r2"), RotacaoDeRespostas.ordenar(cards, historico, 8).map { it.id })
    }

    @Test fun `alias que conecta duas referencias anteriores nao duplica resposta nem contador`() {
        val a = card("a", "Nome A").copy(respostaId = "id-a")
        val b = card("b", "Nome B").copy(respostaId = "id-b")
        val ponte = card("c", "Nome B").copy(respostaId = "id-a")
        val cards = listOf(a, b, ponte)
        assertEquals(1, AcervoDeRespostas.contarIdentidades(cards.map(AcervoDeRespostas::chaves)))
        assertEquals(listOf(a), AcervoDeRespostas.semRepeticoes(cards))
    }

    @Test fun `partida nao preenche rodadas com respostas duplicadas`() {
        val b = baralho("a", listOf(card("a", "hp"), card("b", "hp")))
        assertThrows(IllegalArgumentException::class.java) {
            CriarPartida.executar("LOBO", listOf(Jogador("1", "Ana"), Jogador("2", "Bia")), RegrasPartida(numeroDeRodadas = 2), listOf(b))
        }
    }
}
