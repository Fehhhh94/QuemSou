package com.quemsou.app.domain.rules

import com.quemsou.app.domain.model.*

/** Reúne referências editoriais da mesma resposta, inclusive entre temas diferentes. */
object AcervoDeRespostas {
    fun contarIdentidades(identidades: List<Set<String>>): Int = agrupar(identidades).size

    private fun agrupar(identidades: List<Set<String>>): List<Set<String>> {
        val grupos = mutableListOf<Set<String>>()
        identidades.forEach { identidade ->
            val relacionados = grupos.filter { it.intersect(identidade).isNotEmpty() }
            grupos.removeAll(relacionados.toSet())
            grupos.add(identidade + relacionados.flatten())
        }
        return grupos
    }

    fun chaves(card: Card): Set<String> = card.chavesDaResposta.ifEmpty { setOf(
        "id:${SelecionadorDeDicas.resposta(card)}", "nome:${SelecionadorDeDicas.normalizar(card.answer)}",
    ) }

    fun organizar(baralhos: List<Baralho>): List<Resposta> {
        val grupos = mutableListOf<MutableList<Card>>()
        // Versões recentes prevalecem em correções do mesmo id de dica. Desempate estável.
        val ordenados = baralhos.sortedWith(compareByDescending<Baralho> { it.versao }.thenBy { it.id })
            .flatMap { it.cards.sortedBy(Card::id) }
        val identidades = ordenados.associate { it.id to chaves(it) }
        ordenados.forEach { card ->
                val identidade = identidades.getValue(card.id)
                val relacionadas = grupos.filter { grupo -> grupo.any { identidades.getValue(it.id).any(identidade::contains) } }
                val unido = mutableListOf(card)
                relacionadas.forEach { unido.addAll(it) }
                grupos.removeAll(relacionadas.toSet())
                grupos.add(unido)
            }
        val ordem = ordenados.mapIndexed { index, card -> card.id to index }.toMap()
        return grupos.map { grupo ->
            val referencias = grupo.sortedBy { ordem[it.id] }
            val base = referencias.first()
            Resposta(
                id = SelecionadorDeDicas.resposta(base), nome = base.answer,
                temas = referencias.map { it.category }.toSet(),
                dicas = referencias.flatMap(SelecionadorDeDicas::banco).distinctBy { it.id }
                    .distinctBy { SelecionadorDeDicas.normalizar(it.texto) },
                chaves = referencias.flatMap { identidades.getValue(it.id) }.toSet(), referencias = referencias,
            )
        }.sortedBy { it.id }
    }

    /** Preserva a composição de cada baralho e liga suas referências ao banco compartilhado. */
    fun compartilhar(baralhos: List<Baralho>): List<Baralho> {
        val porCard = organizar(baralhos).flatMap { resposta -> resposta.referencias.map { it.id to resposta } }.toMap()
        return baralhos.map { baralho -> baralho.copy(cards = baralho.cards.map { card ->
            val resposta = porCard.getValue(card.id)
            card.copy(bancoDeDicas = resposta.dicas, chavesDaResposta = resposta.chaves)
        }) }
    }

    /** A mesma resposta ocupa uma única rodada, qualquer que seja sua edição. */
    fun semRepeticoes(cards: List<Card>): List<Card> {
        val identidades = cards.map(::chaves)
        val grupos = agrupar(identidades)
        val vistos = mutableSetOf<Int>()
        return cards.filterIndexed { index, _ ->
            vistos.add(grupos.indexOfFirst { grupo -> identidades[index].any(grupo::contains) })
        }
    }
}
