package com.quemsou.app.domain.rules

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.RegrasPartida
import com.quemsou.app.domain.model.ResultadoTurno

/**
 * Regras de pontuação de um turno — especificação v3, "cabo de guerra"
 * (ver docs/GAME_RULES.md).
 */
object CalculadoraDePontos {

    /**
     * Calcula a pontuação de um turno em que alguém acertou.
     *
     * Acertar com N dicas reveladas vale `11 − N` pontos para o acertador
     * (dica 1 → 10 pontos ... dica 10 → 1 ponto). O leitor ganha 1 ponto por
     * dica revelada sem acerto — as `N − 1` dicas anteriores à do acerto —
     * se [RegrasPartida.leitorPontua]; caso contrário, 0. Com o leitor
     * pontuando, todo turno com acerto distribui exatamente 10 pontos no
     * total entre acertador e leitor.
     *
     * @param dicasUsadas quantidade de dicas reveladas até o acerto; deve estar em 1..10.
     * @throws IllegalArgumentException se [dicasUsadas] estiver fora de 1..10.
     */
    fun calcular(dicasUsadas: Int, regras: RegrasPartida): ResultadoTurno {
        require(dicasUsadas in 1..Card.QUANTIDADE_DE_DICAS) {
            "dicasUsadas deve estar entre 1 e ${Card.QUANTIDADE_DE_DICAS}, mas é $dicasUsadas."
        }
        return ResultadoTurno(
            pontosAcertador = Card.QUANTIDADE_DE_DICAS + 1 - dicasUsadas,
            pontosLeitor = if (regras.leitorPontua) dicasUsadas - 1 else 0,
        )
    }

    /**
     * Pontuação do turno em que ninguém acertou (card queimado): o acertador
     * não pontua e o leitor ganha as [Card.QUANTIDADE_DE_DICAS] dicas
     * reveladas sem acerto — 10 pontos — se [RegrasPartida.leitorPontua];
     * caso contrário, 0.
     */
    fun ninguemAcertou(regras: RegrasPartida): ResultadoTurno =
        ResultadoTurno(
            pontosAcertador = 0,
            pontosLeitor = if (regras.leitorPontua) Card.QUANTIDADE_DE_DICAS else 0,
        )
}
