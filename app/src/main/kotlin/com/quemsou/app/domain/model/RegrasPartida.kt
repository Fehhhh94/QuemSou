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
 * @property modoShot se `true` (padrão NÃO), algumas posições do grid pedem um
 *   shot antes de revelar a dica — pedágio, não armadilha: a dica é revelada
 *   normalmente depois e a pontuação não muda em nada.
 * @property quantidadeDeShots quantas posições do grid têm shot por turno, de
 *   [MINIMO_DE_SHOTS] a [MAXIMO_DE_SHOTS]; só tem efeito com [modoShot] ligado.
 */
data class RegrasPartida(
    val leitorPontua: Boolean = true,
    val numeroDeRodadas: Int = 5,
    val descartarCardQueimado: Boolean = true,
    val modoShot: Boolean = false,
    val quantidadeDeShots: Int = QUANTIDADE_PADRAO_DE_SHOTS,
) {
    init {
        require(numeroDeRodadas > 0) {
            "numeroDeRodadas deve ser maior que zero, mas é $numeroDeRodadas."
        }
        require(quantidadeDeShots in MINIMO_DE_SHOTS..MAXIMO_DE_SHOTS) {
            "quantidadeDeShots deve estar entre $MINIMO_DE_SHOTS e $MAXIMO_DE_SHOTS, mas é $quantidadeDeShots."
        }
    }

    companion object {
        /** Mínimo de posições com shot por turno (Modo Shot). */
        const val MINIMO_DE_SHOTS = 1

        /** Máximo de posições com shot por turno (Modo Shot). */
        const val MAXIMO_DE_SHOTS = 3

        /** Quantidade padrão de posições com shot por turno (Modo Shot). */
        const val QUANTIDADE_PADRAO_DE_SHOTS = 2
    }
}
