package com.quemsou.app.data.local

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.DicaDoBanco
import com.quemsou.app.data.catalogo.DicaJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Converte a entidade persistida no modelo de domínio. Lança
 * [IllegalArgumentException] se [CardEntity.type] ou [CardEntity.category]
 * não corresponderem a um enum conhecido.
 */
fun CardEntity.paraDominio(): Card = Card(
    id = id,
    type = CardType.valueOf(type),
    category = CardCategory.valueOf(category),
    answer = answer,
    clues = clues,
    respostaId = respostaId,
    bancoDeDicas = Json.decodeFromString<List<DicaJson>>(bancoDeDicasJson).map { DicaDoBanco(it.id, it.texto) },
)

/** Converte o modelo de domínio na entidade persistida, dona do vínculo com o baralho. */
fun Card.paraEntidade(baralhoId: String): CardEntity = CardEntity(
    id = id,
    type = type.name,
    category = category.name,
    answer = answer,
    clues = clues,
    baralhoId = baralhoId,
    respostaId = respostaId,
    bancoDeDicasJson = Json.encodeToString(bancoDeDicas.map { DicaJson(it.id, it.texto) }),
)
