package com.quemsou.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PartidaTest {

    private fun jogadores(quantidade: Int, comTimes: Boolean = false) =
        List(quantidade) { indice ->
            Jogador(
                id = "j${indice + 1}",
                nome = "Jogador ${indice + 1}",
                timeId = if (comTimes) "time-${indice % 2}" else null,
            )
        }

    private fun baralho(tamanho: Int) = List(tamanho) { indice ->
        Card(
            id = "card-${indice + 1}",
            type = CardType.COISA,
            category = CardCategory.LIVRE,
            answer = "Resposta ${indice + 1}",
            clues = List(Card.QUANTIDADE_DE_DICAS) { "Dica ${it + 1} do card ${indice + 1}" },
        )
    }

    private fun partida(
        quantidadeDeJogadores: Int = 3,
        rodadas: Int = 3,
        modoDeJogo: ModoDeJogo = ModoDeJogo.INDIVIDUAL,
        regras: RegrasPartida = RegrasPartida(numeroDeRodadas = rodadas),
        comTimes: Boolean = modoDeJogo == ModoDeJogo.TIMES,
    ) = Partida(
        jogadores = jogadores(quantidadeDeJogadores, comTimes),
        modoDeJogo = modoDeJogo,
        regras = regras,
        baralho = baralho(rodadas),
        seed = 42L,
    )

    /** Encerra a rodada atual com o [acertadorId] acertando com 1 dica usada. */
    private fun Partida.rodadaComAcertoDe(acertadorId: String): Partida =
        encerrarTurno(iniciarTurno().revelarDica(posicao = 1).registrarAcerto(acertadorId))

    /** Encerra a rodada atual queimando o card. */
    private fun Partida.rodadaQueimada(): Partida =
        encerrarTurno(iniciarTurno().revelarDica(posicao = 1).queimarCard())

    @Test
    fun `partida exige de 2 a 4 jogadores`() {
        assertThrows(IllegalArgumentException::class.java) { partida(quantidadeDeJogadores = 1) }
        assertThrows(IllegalArgumentException::class.java) { partida(quantidadeDeJogadores = 5) }
        assertEquals(2, partida(quantidadeDeJogadores = 2).jogadores.size)
        assertEquals(4, partida(quantidadeDeJogadores = 4).jogadores.size)
    }

    @Test
    fun `leitor gira entre turnos e e circular`() {
        var partida = partida(quantidadeDeJogadores = 3, rodadas = 4)
        assertEquals("j1", partida.leitorDaVez.id)

        partida = partida.rodadaQueimada()
        assertEquals("j2", partida.leitorDaVez.id)

        partida = partida.rodadaQueimada()
        assertEquals("j3", partida.leitorDaVez.id)

        partida = partida.rodadaQueimada()
        assertEquals("j1", partida.leitorDaVez.id)
    }

    @Test
    fun `turno usa o card da rodada e os demais jogadores como adivinhadores`() {
        val partida = partida(quantidadeDeJogadores = 3)
        val turno = partida.iniciarTurno()

        assertEquals("card-1", turno.card.id)
        assertEquals("j1", turno.leitor.id)
        assertEquals(listOf("j2", "j3"), turno.adivinhadores.map { it.id })
    }

    @Test
    fun `partida encerra apos o total de rodadas`() {
        var partida = partida(rodadas = 2)

        partida = partida.rodadaQueimada()
        assertTrue(!partida.encerrada)

        partida = partida.rodadaQueimada()
        assertTrue(partida.encerrada)
        assertThrows(IllegalStateException::class.java) { partida.iniciarTurno() }
        assertThrows(IllegalStateException::class.java) {
            partida.encerrarTurno(partida.copy(encerrada = false).iniciarTurno())
        }
    }

    @Test
    fun `acerto soma pontos ao acertador e ao leitor`() {
        val partida = partida(quantidadeDeJogadores = 3).rodadaComAcertoDe("j2")

        assertEquals(10, partida.placar.pontosDe("j2"))
        assertEquals(10, partida.placar.pontosDe("j1"))
        assertEquals(0, partida.placar.pontosDe("j3"))
    }

    @Test
    fun `card queimado nao pontua ninguem`() {
        val partida = partida().rodadaQueimada()

        assertTrue(partida.placar.pontosPorJogador.values.all { it == 0 })
    }

    @Test
    fun `encerrar turno que nao terminou e rejeitado`() {
        val partida = partida()

        assertThrows(IllegalArgumentException::class.java) {
            partida.encerrarTurno(partida.iniciarTurno().revelarDica(posicao = 1))
        }
    }

    @Test
    fun `empate e declarado com todos os vencedores`() {
        var partida = partida(quantidadeDeJogadores = 2, rodadas = 2)

        partida = partida.rodadaComAcertoDe("j2") // leitor j1: ambos +10
        partida = partida.rodadaComAcertoDe("j1") // leitor j2: ambos +10

        assertTrue(partida.encerrada)
        assertEquals(listOf("j1", "j2"), partida.vencedores())
    }

    @Test
    fun `modo times exige timeId em todos os jogadores e pelo menos 2 times`() {
        assertThrows(IllegalArgumentException::class.java) {
            partida(modoDeJogo = ModoDeJogo.TIMES, comTimes = false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Partida(
                jogadores = jogadores(2).map { it.copy(timeId = "time-unico") },
                modoDeJogo = ModoDeJogo.TIMES,
                regras = RegrasPartida(numeroDeRodadas = 2),
                baralho = baralho(2),
                seed = 42L,
            )
        }
    }

    @Test
    fun `vencedores no modo times somam os pontos do time`() {
        // 4 jogadores: j1/j3 no time-0, j2/j4 no time-1. Leitor não pontua para
        // o resultado depender só do acertador.
        var partida = partida(
            quantidadeDeJogadores = 4,
            rodadas = 2,
            modoDeJogo = ModoDeJogo.TIMES,
            regras = RegrasPartida(leitorPontua = false, numeroDeRodadas = 2),
        )

        partida = partida.rodadaComAcertoDe("j2") // time-1 +10
        partida = partida.rodadaComAcertoDe("j4") // time-1 +10

        assertEquals(listOf("time-1"), partida.vencedores())
    }

    @Test
    fun `baralho menor que o total de rodadas e rejeitado`() {
        assertThrows(IllegalArgumentException::class.java) {
            Partida(
                jogadores = jogadores(2),
                modoDeJogo = ModoDeJogo.INDIVIDUAL,
                regras = RegrasPartida(numeroDeRodadas = 5),
                baralho = baralho(4),
                seed = 42L,
            )
        }
    }
}
