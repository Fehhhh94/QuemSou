package com.quemsou.app.domain.model

import com.quemsou.app.domain.rules.CalculadoraDePontos
import com.quemsou.app.domain.rules.EmbaralhadorDeCards

/**
 * Um turno da partida: o leitor lê dicas do card e os adivinhadores tentam
 * acertar a resposta escolhendo posições no grid 1–10.
 *
 * Imutável: cada transição retorna um novo [Turno]. **Transições inválidas
 * lançam exceção** — [IllegalStateException] quando o estado atual não permite
 * a ação e [IllegalArgumentException] quando o argumento é inválido — a
 * abordagem única de erro do domínio (mesma dos `require` dos modelos).
 *
 * O grid é às cegas: [criar] embaralha as 10 dicas do card nas posições 1–10
 * com o PRNG determinístico por seed da Fase 1, então a posição **não** indica
 * dificuldade. A pontuação usa a quantidade de dicas usadas (11 − N), nunca o
 * número da posição tocada.
 *
 * @property card card da vez; a resposta só é exposta no fim do turno.
 * @property leitor quem lê as dicas; nunca escolhe posição nem adivinha.
 * @property adivinhadores todos os jogadores menos o leitor (1 a 3).
 * @property regras regras da partida (leitor pontua etc.).
 * @property dicasNoGrid as 10 dicas embaralhadas; índice 0 = posição 1 do grid.
 * @property indiceDoEscolhedor índice em [adivinhadores] de quem escolhe agora.
 * @property posicoesReveladas posições já reveladas, na ordem de revelação.
 * @property estado estado atual da máquina ([EstadoDoTurno]).
 */
data class Turno(
    val card: Card,
    val leitor: Jogador,
    val adivinhadores: List<Jogador>,
    val regras: RegrasPartida,
    val dicasNoGrid: List<String>,
    val indiceDoEscolhedor: Int = 0,
    val posicoesReveladas: List<Int> = emptyList(),
    val estado: EstadoDoTurno = EstadoDoTurno.EscolhendoDica,
) {
    init {
        require(adivinhadores.size in 1..MAXIMO_DE_ADIVINHADORES) {
            "Turno exige de 1 a $MAXIMO_DE_ADIVINHADORES adivinhadores, mas tem ${adivinhadores.size}."
        }
        require(adivinhadores.none { it.id == leitor.id }) {
            "O leitor '${leitor.id}' não pode ser adivinhador."
        }
        require(dicasNoGrid.size == Card.QUANTIDADE_DE_DICAS) {
            "O grid deve ter ${Card.QUANTIDADE_DE_DICAS} dicas, mas tem ${dicasNoGrid.size}."
        }
        require(indiceDoEscolhedor in adivinhadores.indices) {
            "indiceDoEscolhedor $indiceDoEscolhedor fora de ${adivinhadores.indices}."
        }
        require(posicoesReveladas.all { it in 1..Card.QUANTIDADE_DE_DICAS }) {
            "Posições reveladas fora do grid 1–${Card.QUANTIDADE_DE_DICAS}: $posicoesReveladas."
        }
        require(posicoesReveladas.toSet().size == posicoesReveladas.size) {
            "Posições reveladas repetidas: $posicoesReveladas."
        }
    }

    /** Adivinhador que escolhe a próxima posição do grid. */
    val escolhedorDaVez: Jogador
        get() = adivinhadores[indiceDoEscolhedor]

    /** Quantidade de dicas já usadas (reveladas). */
    val dicasUsadas: Int
        get() = posicoesReveladas.size

    /** Dica na [posicao] (1–10) do grid. */
    fun dicaNaPosicao(posicao: Int): String {
        require(posicao in 1..Card.QUANTIDADE_DE_DICAS) {
            "Posição $posicao fora do grid 1–${Card.QUANTIDADE_DE_DICAS}."
        }
        return dicasNoGrid[posicao - 1]
    }

    /**
     * O escolhedor da vez toca a [posicao] do grid; a dica é revelada.
     * Posição já revelada não pode ser revelada de novo.
     */
    fun revelarDica(posicao: Int): Turno {
        check(estado is EstadoDoTurno.EscolhendoDica) {
            "Só é possível revelar dica enquanto se escolhe uma posição (estado atual: $estado)."
        }
        require(posicao in 1..Card.QUANTIDADE_DE_DICAS) {
            "Posição $posicao fora do grid 1–${Card.QUANTIDADE_DE_DICAS}."
        }
        require(posicao !in posicoesReveladas) { "Posição $posicao já foi revelada." }
        return copy(
            posicoesReveladas = posicoesReveladas + posicao,
            estado = EstadoDoTurno.DicaRevelada(posicao),
        )
    }

    /**
     * Ninguém arriscou: volta a escolher dica, passando a vez ao próximo
     * adivinhador (rodízio circular). Se as 10 dicas já foram usadas, não há
     * outra dica — o card queima.
     */
    fun outraDica(): Turno {
        check(estado is EstadoDoTurno.DicaRevelada) {
            "Só é possível pedir outra dica com uma dica revelada (estado atual: $estado)."
        }
        if (dicasUsadas == Card.QUANTIDADE_DE_DICAS) return queimar()
        return copy(
            indiceDoEscolhedor = (indiceDoEscolhedor + 1) % adivinhadores.size,
            estado = EstadoDoTurno.EscolhendoDica,
        )
    }

    /**
     * O adivinhador [jogadorId] acertou a resposta com a dica atual revelada.
     * Pontua 11 − dicas usadas via [CalculadoraDePontos]; o leitor acompanha
     * conforme [RegrasPartida.leitorPontua].
     */
    fun registrarAcerto(jogadorId: String): Turno {
        check(estado is EstadoDoTurno.DicaRevelada) {
            "Só é possível registrar acerto com uma dica revelada (estado atual: $estado)."
        }
        require(adivinhadores.any { it.id == jogadorId }) {
            "Jogador '$jogadorId' não é adivinhador deste turno."
        }
        val resultado = CalculadoraDePontos.calcular(dicasUsadas, regras)
        return copy(
            estado = EstadoDoTurno.TurnoEncerrado.Acerto(
                resposta = card.answer,
                dicasUsadas = dicasUsadas,
                acertadorId = jogadorId,
                pontosAcertador = resultado.pontosAcertador,
                pontosLeitor = resultado.pontosLeitor,
            ),
        )
    }

    /** Os adivinhadores desistem: o card queima e ninguém pontua. */
    fun queimarCard(): Turno {
        check(estado !is EstadoDoTurno.TurnoEncerrado) { "O turno já foi encerrado." }
        return queimar()
    }

    private fun queimar(): Turno = copy(
        estado = EstadoDoTurno.TurnoEncerrado.Queimado(
            resposta = card.answer,
            dicasUsadas = dicasUsadas,
        ),
    )

    companion object {
        /** Máximo de adivinhadores por turno (partida de 4 jogadores). */
        const val MAXIMO_DE_ADIVINHADORES = 3

        /**
         * Cria o turno embaralhando as 10 dicas do [card] nas posições do grid
         * de forma determinística pela [seedDasDicas] — mesmo turno, mesmo grid
         * em qualquer aparelho (base do multiplayer da Fase 4).
         */
        fun criar(
            card: Card,
            leitor: Jogador,
            adivinhadores: List<Jogador>,
            regras: RegrasPartida,
            seedDasDicas: Long,
        ): Turno = Turno(
            card = card,
            leitor = leitor,
            adivinhadores = adivinhadores,
            regras = regras,
            dicasNoGrid = EmbaralhadorDeCards.embaralhar(card.clues, seedDasDicas),
        )
    }
}
