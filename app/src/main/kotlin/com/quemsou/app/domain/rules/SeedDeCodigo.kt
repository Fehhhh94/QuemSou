package com.quemsou.app.domain.rules

/**
 * Converte o código da partida (ex.: "LOBO") em uma seed determinística.
 *
 * A mesma seed em dois aparelhos gera o mesmo baralho — é a base do
 * multiplayer offline por código de partida.
 */
object SeedDeCodigo {

    private const val BASE = 31L
    private const val SEED_INICIAL = 1125899906842597L // primo grande, evita seeds pequenas para códigos curtos

    /**
     * Gera a seed do [codigo] informado.
     *
     * O código é normalizado antes (trim + uppercase), então `"lobo"` e
     * `" LOBO "` geram a mesma seed. O hash é polinomial (base 31) sobre os
     * code points da string, implementado à mão — não usa `hashCode()` da
     * plataforma, garantindo que a mesma string produza a mesma seed em
     * qualquer versão de Kotlin/JVM, para sempre.
     */
    fun gerar(codigo: String): Long {
        val normalizado = codigo.trim().uppercase()
        var seed = SEED_INICIAL
        for (char in normalizado) {
            seed = BASE * seed + char.code
        }
        return seed
    }
}
