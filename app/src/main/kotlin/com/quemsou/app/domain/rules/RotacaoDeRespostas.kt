package com.quemsou.app.domain.rules

import com.quemsou.app.domain.model.Card

/** Inéditas primeiro, depois as ausentes há mais tempo. Empates mantêm o sorteio pela seed. */
object RotacaoDeRespostas {
    fun ordenar(cards: List<Card>, ultimasAparicoes: Map<String, Long>, seed: Long): List<Card> =
        EmbaralhadorDeCards.embaralhar(AcervoDeRespostas.semRepeticoes(cards), seed)
            .sortedBy { card -> AcervoDeRespostas.chaves(card).maxOf { ultimasAparicoes[it] ?: 0L } }
}
