package com.quemsou.app.domain.model

/**
 * Modo de disputa da partida, configurável antes de começar.
 */
enum class ModoDeJogo {
    /** Cada jogador pontua por si; o vencedor é o jogador com mais pontos. */
    INDIVIDUAL,

    /**
     * Jogadores agrupados por [Jogador.timeId]; o placar do time é a soma dos
     * pontos dos seus jogadores.
     */
    TIMES,
}
