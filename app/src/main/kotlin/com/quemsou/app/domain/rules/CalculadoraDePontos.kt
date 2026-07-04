package com.quemsou.app.domain.rules

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.RegrasPartida
import com.quemsou.app.domain.model.ResultadoTurno

/**
 * Regras de pontuação de um turno (ver docs/GAME_RULES.md).
 */
object CalculadoraDePontos {

    /**
     * Calcula a pontuação de um turno em que alguém acertou.
     *
     * Acertar com N dicas reveladas vale `11 − N` pontos para o acertador
     * (dica 1 → 10 pontos ... dica 10 → 1 ponto). O leitor ganha os mesmos
     * pontos se [RegrasPartida.leitorPontua]; caso contrário, 0.
     *
     * @param dicasUsadas quantidade de dicas reveladas até o acerto; deve estar em 1..10.
     * @throws IllegalArgumentException se [dicasUsadas] estiver fora de 1..10.
     */
    fun calcular(dicasUsadas: Int, regras: RegrasPartida): ResultadoTurno {
        require(dicasUsadas in 1..Card.QUANTIDADE_DE_DICAS) {
            "dicasUsadas deve estar entre 1 e ${Card.QUANTIDADE_DE_DICAS}, mas é $dicasUsadas."
        }
        val pontos = Card.QUANTIDADE_DE_DICAS + 1 - dicasUsadas
        return ResultadoTurno(
            pontosAcertador = pontos,
            pontosLeitor = if (regras.leitorPontua) pontos else 0,
        )
    }

    /** Pontuação do turno em que ninguém acertou: ninguém pontua. */
    fun ninguemAcertou(): ResultadoTurno =
        ResultadoTurno(pontosAcertador = 0, pontosLeitor = 0)
}
