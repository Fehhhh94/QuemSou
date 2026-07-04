package com.quemsou.app.domain.model

/**
 * Pontuação resultante de um turno.
 *
 * @property pontosAcertador pontos de quem acertou a resposta (0 se ninguém acertou).
 * @property pontosLeitor pontos do leitor do card, conforme [RegrasPartida.leitorPontua].
 */
data class ResultadoTurno(
    val pontosAcertador: Int,
    val pontosLeitor: Int,
)
