package com.quemsou.app.data.catalogo

import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.EstadoDoBaralho
import kotlinx.serialization.Serializable

/**
 * Os dois formatos JSON do catálogo estático de baralhos, documentados em
 * `docs/CATALOG_FORMAT.md` (o repositório separado de baralhos os segue):
 * o **índice** ([IndiceDoCatalogoJson]) e o **baralho** ([BaralhoJson]).
 *
 * Enums viajam como `String` de propósito: um valor desconhecido vira
 * violação legível no [ParserDoCatalogo], nunca exceção crua de
 * desserialização — a mesma filosofia do `ValidadorEditorial`.
 */
@Serializable
data class IndiceDoCatalogoJson(
    val baralhos: List<EntradaDoIndiceJson>,
)

/**
 * Uma entrada do índice do catálogo: os metadados que a tela de catálogo
 * lista sem precisar baixar o baralho inteiro.
 */
@Serializable
data class EntradaDoIndiceJson(
    val id: String,
    val nome: String,
    val categoria: String,
    val versao: Int,
    val estado: String,
    val quantidadeDeCards: Int,
    val url: String,
    val descricao: String,
)

/**
 * Um baralho completo como publicado no catálogo — e também como embarcado
 * em `assets/cards.json` (mesmo formato, menos a `url`, que é do índice).
 * Os cards **herdam a categoria do baralho**: não há campo `category` por card.
 */
@Serializable
data class BaralhoJson(
    val id: String,
    val nome: String,
    val categoria: String,
    val versao: Int,
    val estado: String,
    val cards: List<CardDoBaralhoJson>,
)

/** Um card como declarado no JSON de um baralho. */
@Serializable
data class CardDoBaralhoJson(
    val id: String,
    val type: String,
    val answer: String,
    val clues: List<String>,
)

/**
 * Uma entrada do índice já validada pelo [ParserDoCatalogo] — enums reais,
 * pronta para a tela de catálogo (parte 2 da 5A).
 */
data class EntradaDoCatalogo(
    val id: String,
    val nome: String,
    val categoria: CardCategory,
    val versao: Int,
    val estado: EstadoDoBaralho,
    val quantidadeDeCards: Int,
    val url: String,
    val descricao: String,
)
