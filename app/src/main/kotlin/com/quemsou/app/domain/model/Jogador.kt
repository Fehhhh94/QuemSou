package com.quemsou.app.domain.model

/**
 * Um jogador da partida.
 *
 * @property id identificador único do jogador na partida.
 * @property nome nome exibido durante o jogo.
 * @property timeId time do jogador no modo [ModoDeJogo.TIMES]; ignorado no
 *   modo individual. O placar de time é a soma dos jogadores do time.
 */
data class Jogador(
    val id: String,
    val nome: String,
    val timeId: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Jogador com id vazio." }
        require(nome.isNotBlank()) { "Jogador '$id' com nome vazio." }
    }
}
