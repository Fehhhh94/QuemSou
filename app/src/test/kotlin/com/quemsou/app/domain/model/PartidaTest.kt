package com.quemsou.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PartidaTest {

    private fun jogadores(quantidade: Int) = List(quantidade) { indice ->
        Jogador(id = "j${indice + 1}", nome = "Jogador ${indice + 1}")
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
        regras: RegrasPartida = RegrasPartida(numeroDeRodadas = rodadas),
        grupos: List<Grupo>? = null,
    ): Partida {
        val jogadores = jogadores(quantidadeDeJogadores)
        return Partida(
            jogadores = jogadores,
            regras = regras,
            baralho = baralho(rodadas),
            seed = 42L,
            grupos = grupos ?: Grupo.individuais(jogadores),
        )
    }

    /** Partida mista da especificação v4: "j1 & j2" num grupo, j3 e j4 solo. */
    private fun partidaMista(rodadas: Int = 3): Partida {
        val jogadores = jogadores(4)
        return Partida(
            jogadores = jogadores,
            regras = RegrasPartida(numeroDeRodadas = rodadas),
            baralho = baralho(rodadas),
            seed = 42L,
            grupos = listOf(
                Grupo.criar(id = "g1", membros = listOf(jogadores[0], jogadores[1])),
                Grupo.criar(id = "j3", membros = listOf(jogadores[2])),
                Grupo.criar(id = "j4", membros = listOf(jogadores[3])),
            ),
        )
    }

    /** Encerra a rodada atual com o [acertadorId] acertando após [dicasUsadas] dicas. */
    private fun Partida.rodadaComAcertoDe(acertadorId: String, dicasUsadas: Int = 1): Partida {
        var turno = iniciarTurno()
        repeat(dicasUsadas - 1) { indice ->
            turno = turno.revelarDica(posicao = indice + 1).outraDica()
        }
        turno = turno.revelarDica(posicao = dicasUsadas).registrarAcerto(acertadorId)
        return encerrarTurno(turno)
    }

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
    fun `grupo padrao de 1 jogador se comporta exatamente como o individual de antes`() {
        // Regressão explícita da especificação v4: sem agrupamento, cada
        // jogador é seu próprio grupo de 1 — mesmo id, mesmo nome e os mesmos
        // pontos que o modelo antigo dava ao jogador isolado.
        val partida = partida(quantidadeDeJogadores = 3).rodadaComAcertoDe("j2")

        assertEquals(listOf("j1", "j2", "j3"), partida.grupos.map { it.id })
        assertEquals(listOf("Jogador 1", "Jogador 2", "Jogador 3"), partida.grupos.map { it.nome })
        assertEquals(10, partida.grupoDe("j2").pontos)
        assertEquals(0, partida.grupoDe("j1").pontos)
        assertEquals(0, partida.grupoDe("j3").pontos)
    }

    @Test
    fun `card queimado da 10 pontos ao grupo do leitor e nada aos demais`() {
        val partida = partida().rodadaQueimada()

        assertEquals(10, partida.grupoDe("j1").pontos)
        assertEquals(0, partida.grupoDe("j2").pontos)
        assertEquals(0, partida.grupoDe("j3").pontos)
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

        partida = partida.rodadaComAcertoDe("j2") // j2 (acertador) +10, j1 (leitor) +0
        partida = partida.rodadaComAcertoDe("j1") // j1 (acertador) +10, j2 (leitor) +0

        assertTrue(partida.encerrada)
        assertEquals(listOf("j1", "j2"), partida.vencedores().map { it.id })
    }

    @Test
    fun `todo jogador precisa pertencer a exatamente um grupo`() {
        val jogadores = jogadores(3)

        // j3 fora de qualquer grupo.
        assertThrows(IllegalArgumentException::class.java) {
            partida(grupos = listOf(Grupo.criar("g1", listOf(jogadores[0], jogadores[1]))))
        }
        // j2 em dois grupos.
        assertThrows(IllegalArgumentException::class.java) {
            partida(
                grupos = listOf(
                    Grupo.criar("g1", listOf(jogadores[0], jogadores[1])),
                    Grupo.criar("g2", listOf(jogadores[1], jogadores[2])),
                ),
            )
        }
        // Ids de grupo repetidos.
        assertThrows(IllegalArgumentException::class.java) {
            partida(
                grupos = listOf(
                    Grupo.criar("g1", listOf(jogadores[0], jogadores[1])),
                    Grupo.criar("g1", listOf(jogadores[2])),
                ),
            )
        }
    }

    @Test
    fun `grupo de 2 soma os pontos de qualquer membro que pontue`() {
        val jogadores = jogadores(4)
        var partida = partida(
            quantidadeDeJogadores = 4,
            rodadas = 2,
            grupos = listOf(
                Grupo.criar(id = "j1", membros = listOf(jogadores[0])),
                Grupo.criar(id = "g1", membros = listOf(jogadores[1], jogadores[3])),
                Grupo.criar(id = "j3", membros = listOf(jogadores[2])),
            ),
        )

        partida = partida.rodadaComAcertoDe("j2") // g1 +10
        partida = partida.rodadaComAcertoDe("j4") // g1 +10

        assertEquals("Jogador 2 & Jogador 4", partida.grupoDe("j2").nome)
        assertEquals(partida.grupoDe("j2"), partida.grupoDe("j4"))
        assertEquals(20, partida.grupoDe("j2").pontos)
        assertEquals(listOf("g1"), partida.vencedores().map { it.id })
    }

    @Test
    fun `partida mista credita cada acao ao grupo do jogador que agiu`() {
        // Grupo "Jogador 1 & Jogador 2" + dois jogadores solo (j3 e j4).
        var partida = partidaMista(rodadas = 3)

        // Rodada 1 (leitor j1): j3 acerta na 1ª dica → grupo solo de j3 +10.
        partida = partida.rodadaComAcertoDe("j3")
        // Rodada 2 (leitor j2): j1 acerta na 3ª dica → acertador +8 e leitor
        // +2, ambos para o MESMO grupo — companheiro de time do leitor pontua.
        partida = partida.rodadaComAcertoDe("j1", dicasUsadas = 3)
        // Rodada 3 (leitor j3): card queimado → grupo solo de j3 +10.
        partida = partida.rodadaQueimada()

        assertTrue(partida.encerrada)
        assertEquals(10, partida.grupoDe("j1").pontos)
        assertEquals(20, partida.grupoDe("j3").pontos)
        assertEquals(0, partida.grupoDe("j4").pontos)
        assertEquals(
            listOf("Jogador 3", "Jogador 1 & Jogador 2", "Jogador 4"),
            partida.ranking().map { it.nome },
        )
        assertEquals(listOf("j3"), partida.vencedores().map { it.id })
    }

    @Test
    fun `com o leitor pontuando todo turno distribui 10 pontos somando por grupo`() {
        // A invariante da especificação v3 continua valendo na v4, agora
        // somando por grupo: acerto cedo, acerto tardio e card queimado.
        var partida = partidaMista(rodadas = 3)
        var rodadasJogadas = 0

        listOf<(Partida) -> Partida>(
            { it.rodadaComAcertoDe("j3") },
            { it.rodadaComAcertoDe("j4", dicasUsadas = 7) },
            { it.rodadaQueimada() },
        ).forEach { rodada ->
            partida = rodada(partida)
            rodadasJogadas++
            assertEquals(10 * rodadasJogadas, partida.grupos.sumOf { it.pontos })
        }
    }

    @Test
    fun `baralho menor que o total de rodadas e rejeitado`() {
        assertThrows(IllegalArgumentException::class.java) {
            Partida(
                jogadores = jogadores(2),
                regras = RegrasPartida(numeroDeRodadas = 5),
                baralho = baralho(4),
                seed = 42L,
            )
        }
    }
}
