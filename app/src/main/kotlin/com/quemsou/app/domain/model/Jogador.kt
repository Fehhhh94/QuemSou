package com.quemsou.app.domain.model

/**
 * Um jogador da partida.
 *
 * O agrupamento (jogar solo ou em time) não vive aqui: todo jogador pertence
 * a um [Grupo] da [Partida], que é quem acumula os pontos.
 *
 * @property id identificador único do jogador na partida.
 * @property nome nome exibido durante o jogo.
 */
data class Jogador(
    val id: String,
    val nome: String,
) {
    init {
        require(id.isNotBlank()) { "Jogador com id vazio." }
        require(nome.isNotBlank()) { "Jogador '$id' com nome vazio." }
    }
}
