package com.quemsou.app.domain.model

/**
 * Regras configuráveis de uma partida.
 *
 * Novos campos de regra devem entrar aqui conforme as decisões de design forem
 * fechadas.
 *
 * @property leitorPontua se `true` (padrão), o leitor ganha 1 ponto por dica revelada sem
 *   acerto — especificação v3, "cabo de guerra" (ver [com.quemsou.app.domain.rules.CalculadoraDePontos]).
 * @property numeroDeRodadas quantidade de rodadas da partida; deve ser maior que zero.
 * @property descartarCardQueimado se `true` (padrão), o card que ninguém acertou
 *   é descartado e não volta ao baralho da partida.
 */
data class RegrasPartida(
    val leitorPontua: Boolean = true,
    val numeroDeRodadas: Int = 5,
    val descartarCardQueimado: Boolean = true,
) {
    init {
        require(numeroDeRodadas > 0) {
            "numeroDeRodadas deve ser maior que zero, mas é $numeroDeRodadas."
        }
    }
}
