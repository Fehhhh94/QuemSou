package com.quemsou.app.domain.model

/**
 * Regras configuráveis de uma partida.
 *
 * Novos campos de regra devem entrar aqui conforme as decisões de design forem
 * fechadas — por exemplo, o destino do card queimado (decisão ainda em aberto,
 * por isso ainda sem campo correspondente).
 *
 * @property leitorPontua se `true` (padrão), o leitor ganha os mesmos pontos que o acertador.
 * @property numeroDeRodadas quantidade de rodadas da partida; deve ser maior que zero.
 */
data class RegrasPartida(
    val leitorPontua: Boolean = true,
    val numeroDeRodadas: Int = 5,
) {
    init {
        require(numeroDeRodadas > 0) {
            "numeroDeRodadas deve ser maior que zero, mas é $numeroDeRodadas."
        }
    }
}
