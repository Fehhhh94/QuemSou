package com.quemsou.app.data.importer

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Estrutura de `assets/cards.json`: uma versão inteira crescente e a lista
 * completa de cards. O importador só recarrega o banco quando [version] é
 * maior que a última versão importada.
 */
@Serializable
data class CardsJson(
    val version: Int,
    val cards: List<CardJson>,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Faz o parse do conteúdo de `cards.json`. Lança
         * [kotlinx.serialization.SerializationException] se a estrutura for
         * inválida ou faltar campo obrigatório.
         */
        fun deJson(conteudo: String): CardsJson = json.decodeFromString(conteudo)
    }
}

/** Um card como declarado no JSON de assets. */
@Serializable
data class CardJson(
    val id: String,
    val type: CardType,
    val category: CardCategory,
    val answer: String,
    val clues: List<String>,
)

/**
 * Valida e converte o card do JSON no modelo de domínio.
 *
 * Falha ruidosa por design: card inválido lança [IllegalArgumentException]
 * com o id do card — melhor quebrar no debug do que jogar com card capenga.
 */
fun CardJson.paraDominio(): Card {
    require(answer.isNotBlank()) { "Card '$id' inválido: answer vazio." }
    require(clues.size == Card.QUANTIDADE_DE_DICAS) {
        "Card '$id' inválido: deve ter exatamente ${Card.QUANTIDADE_DE_DICAS} dicas, mas tem ${clues.size}."
    }
    require(clues.none { it.isBlank() }) { "Card '$id' inválido: contém dica vazia." }
    return Card(id = id, type = type, category = category, answer = answer, clues = clues)
}
