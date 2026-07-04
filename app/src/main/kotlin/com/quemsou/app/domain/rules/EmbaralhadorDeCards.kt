package com.quemsou.app.domain.rules

import com.quemsou.app.domain.model.Card

/**
 * Embaralhamento determinístico de cards: a mesma seed produz sempre a mesma
 * ordem, em qualquer aparelho e em qualquer versão de Kotlin/JVM — é a base do
 * multiplayer offline por código de partida.
 *
 * Usa Fisher–Yates com um PRNG xorshift64 implementado neste arquivo.
 * Não usa `kotlin.random.Random` nem `java.util.Random`: o algoritmo deles não
 * tem garantia de estabilidade entre versões de Kotlin/JVM.
 */
object EmbaralhadorDeCards {

    // Constante do xorshift64 usada como estado substituto quando a seed é 0,
    // único valor com o qual o xorshift64 ficaria preso para sempre.
    private const val ESTADO_PARA_SEED_ZERO = -0x61C8864680B583EBL

    /**
     * Retorna uma nova lista com os mesmos [cards] em ordem embaralhada
     * de forma determinística pela [seed].
     */
    fun embaralhar(cards: List<Card>, seed: Long): List<Card> {
        val resultado = cards.toMutableList()
        var estado = if (seed == 0L) ESTADO_PARA_SEED_ZERO else seed
        for (i in resultado.lastIndex downTo 1) {
            estado = proximo(estado)
            val j = indicePositivo(estado, limite = i + 1)
            val temporario = resultado[i]
            resultado[i] = resultado[j]
            resultado[j] = temporario
        }
        return resultado
    }

    /** Passo do xorshift64: próximo estado do PRNG. */
    private fun proximo(estado: Long): Long {
        var x = estado
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        return x
    }

    /** Reduz [estado] a um índice em `0 until limite`, sempre não negativo. */
    private fun indicePositivo(estado: Long, limite: Int): Int {
        val resto = (estado % limite + limite) % limite
        return resto.toInt()
    }
}
