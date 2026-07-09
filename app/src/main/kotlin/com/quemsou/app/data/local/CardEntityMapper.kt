package com.quemsou.app.data.local

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType

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
)

/** Converte o modelo de domínio na entidade persistida, dona do vínculo com o baralho. */
fun Card.paraEntidade(baralhoId: String): CardEntity = CardEntity(
    id = id,
    type = type.name,
    category = category.name,
    answer = answer,
    clues = clues,
    baralhoId = baralhoId,
)
