package com.quemsou.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Linha da tabela `cards` — representação persistida de um card.
 *
 * [type] e [category] guardam o `name` dos enums de domínio; [clues] é
 * serializada como JSON string via [Converters]. A conversão de/para o modelo
 * de domínio vive em `CardEntityMapper`.
 *
 * @property id identificador único do card (chave primária).
 * @property type nome do [com.quemsou.app.domain.model.CardType].
 * @property category nome da [com.quemsou.app.domain.model.CardCategory].
 * @property answer resposta secreta do card.
 * @property clues dicas na ordem de revelação.
 */
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val type: String,
    val category: String,
    val answer: String,
    val clues: List<String>,
)
