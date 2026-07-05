package com.quemsou.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TurnoTest {

    private val leitor = Jogador(id = "leitor", nome = "Leitor")

    private fun adivinhadores(quantidade: Int) =
        List(quantidade) { Jogador(id = "adv-${it + 1}", nome = "Adivinhador ${it + 1}") }

    private fun card() = Card(
        id = "card-1",
        type = CardType.PESSOA,
        category = CardCategory.LIVRE,
        answer = "Resposta Secreta",
        clues = List(Card.QUANTIDADE_DE_DICAS) { "Dica ${it + 1}" },
    )

    private fun turno(
        quantidadeDeAdivinhadores: Int = 3,
        regras: RegrasPartida = RegrasPartida(),
        seed: Long = 42L,
    ) = Turno.criar(card(), leitor, adivinhadores(quantidadeDeAdivinhadores), regras, seed)

    /** Revela as 10 posições do grid pedindo outra dica entre uma e outra. */
    private fun turnoComDezDicasReveladas(): Turno {
        var turno = turno()
        for (posicao in 1..9) {
            turno = turno.revelarDica(posicao).outraDica()
        }
        return turno.revelarDica(10)
    }

    // region Máquina de estados

    @Test
    fun `caminho feliz - acerto na primeira dica vale 10 pontos para acertador e leitor`() {
        val encerrado = turno().revelarDica(posicao = 5).registrarAcerto("adv-1")

        assertEquals(
            EstadoDoTurno.TurnoEncerrado.Acerto(
                resposta = "Resposta Secreta",
                dicasUsadas = 1,
                acertadorId = "adv-1",
                pontosAcertador = 10,
                pontosLeitor = 10,
            ),
            encerrado.estado,
        )
    }

    @Test
    fun `acerto com as 10 dicas usadas vale 1 ponto`() {
        val encerrado = turnoComDezDicasReveladas().registrarAcerto("adv-2")

        val acerto = encerrado.estado as EstadoDoTurno.TurnoEncerrado.Acerto
        assertEquals(10, acerto.dicasUsadas)
        assertEquals(1, acerto.pontosAcertador)
        assertEquals(1, acerto.pontosLeitor)
    }

    @Test
    fun `leitor nao pontua quando a regra esta desligada`() {
        val encerrado = turno(regras = RegrasPartida(leitorPontua = false))
            .revelarDica(posicao = 1)
            .registrarAcerto("adv-1")

        val acerto = encerrado.estado as EstadoDoTurno.TurnoEncerrado.Acerto
        assertEquals(10, acerto.pontosAcertador)
        assertEquals(0, acerto.pontosLeitor)
    }

    @Test
    fun `queimar card por desistencia encerra com a resposta revelada`() {
        val encerrado = turno().revelarDica(posicao = 3).queimarCard()

        assertEquals(
            EstadoDoTurno.TurnoEncerrado.Queimado(resposta = "Resposta Secreta", dicasUsadas = 1),
            encerrado.estado,
        )
    }

    @Test
    fun `outra dica apos a decima queima o card`() {
        val encerrado = turnoComDezDicasReveladas().outraDica()

        assertEquals(
            EstadoDoTurno.TurnoEncerrado.Queimado(resposta = "Resposta Secreta", dicasUsadas = 10),
            encerrado.estado,
        )
    }

    @Test
    fun `posicao ja revelada nao pode ser revelada de novo`() {
        val turno = turno().revelarDica(posicao = 3).outraDica()

        assertThrows(IllegalArgumentException::class.java) { turno.revelarDica(posicao = 3) }
    }

    @Test
    fun `posicao fora do grid e rejeitada`() {
        assertThrows(IllegalArgumentException::class.java) { turno().revelarDica(posicao = 0) }
        assertThrows(IllegalArgumentException::class.java) { turno().revelarDica(posicao = 11) }
    }

    @Test
    fun `acertar sem dica revelada e rejeitado`() {
        assertThrows(IllegalStateException::class.java) { turno().registrarAcerto("adv-1") }
    }

    @Test
    fun `revelar dica com outra dica ja revelada e rejeitado`() {
        val turno = turno().revelarDica(posicao = 1)

        assertThrows(IllegalStateException::class.java) { turno.revelarDica(posicao = 2) }
    }

    @Test
    fun `acoes apos o turno encerrado sao rejeitadas`() {
        val encerrado = turno().revelarDica(posicao = 1).queimarCard()

        assertThrows(IllegalStateException::class.java) { encerrado.revelarDica(posicao = 2) }
        assertThrows(IllegalStateException::class.java) { encerrado.outraDica() }
        assertThrows(IllegalStateException::class.java) { encerrado.registrarAcerto("adv-1") }
        assertThrows(IllegalStateException::class.java) { encerrado.queimarCard() }
    }

    @Test
    fun `acerto de quem nao e adivinhador e rejeitado`() {
        val turno = turno().revelarDica(posicao = 1)

        assertThrows(IllegalArgumentException::class.java) { turno.registrarAcerto("leitor") }
        assertThrows(IllegalArgumentException::class.java) { turno.registrarAcerto("desconhecido") }
    }

    // endregion

    // region Rodízio do escolhedor

    @Test
    fun `rodizio com 3 adivinhadores gira e e circular`() {
        var turno = turno(quantidadeDeAdivinhadores = 3)
        assertEquals("adv-1", turno.escolhedorDaVez.id)

        turno = turno.revelarDica(posicao = 1).outraDica()
        assertEquals("adv-2", turno.escolhedorDaVez.id)

        turno = turno.revelarDica(posicao = 2).outraDica()
        assertEquals("adv-3", turno.escolhedorDaVez.id)

        turno = turno.revelarDica(posicao = 3).outraDica()
        assertEquals("adv-1", turno.escolhedorDaVez.id)
    }

    @Test
    fun `rodizio com 2 adivinhadores alterna`() {
        var turno = turno(quantidadeDeAdivinhadores = 2)
        assertEquals("adv-1", turno.escolhedorDaVez.id)

        turno = turno.revelarDica(posicao = 1).outraDica()
        assertEquals("adv-2", turno.escolhedorDaVez.id)

        turno = turno.revelarDica(posicao = 2).outraDica()
        assertEquals("adv-1", turno.escolhedorDaVez.id)
    }

    @Test
    fun `rodizio com 1 adivinhador mantem o mesmo escolhedor`() {
        var turno = turno(quantidadeDeAdivinhadores = 1)
        assertEquals("adv-1", turno.escolhedorDaVez.id)

        turno = turno.revelarDica(posicao = 1).outraDica()
        assertEquals("adv-1", turno.escolhedorDaVez.id)
    }

    @Test
    fun `leitor entre os adivinhadores e rejeitado`() {
        assertThrows(IllegalArgumentException::class.java) {
            Turno.criar(card(), leitor, adivinhadores(2) + leitor, RegrasPartida(), seedDasDicas = 42L)
        }
    }

    // endregion

    // region Embaralhamento das dicas no grid

    @Test
    fun `grid cobre as 10 dicas do card sem repeticao`() {
        val turno = turno()

        assertEquals(Card.QUANTIDADE_DE_DICAS, turno.dicasNoGrid.size)
        assertEquals(card().clues.toSet(), turno.dicasNoGrid.toSet())
    }

    @Test
    fun `mesma seed gera o mesmo grid`() {
        assertEquals(turno(seed = 7L).dicasNoGrid, turno(seed = 7L).dicasNoGrid)
    }

    @Test
    fun `seeds diferentes geram grids diferentes`() {
        assertNotEquals(turno(seed = 1L).dicasNoGrid, turno(seed = 2L).dicasNoGrid)
    }

    // endregion
}
