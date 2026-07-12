package com.quemsou.app.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Acesso à tabela `feedback_de_cards` (modo dev de feedback). Só inserção,
 * contagem, leitura para export e limpeza total — não existe edição de
 * feedback: cada avaliação é uma linha nova.
 */
@Dao
interface FeedbackDeCardDao {

    /** Insere um feedback novo — sempre uma linha própria, nunca substitui. */
    @Insert
    suspend fun inserir(feedback: FeedbackDeCardEntity)

    /** Contagem viva de registros — dirige o "Exportar feedback (N)" da Home. */
    @Query("SELECT COUNT(*) FROM feedback_de_cards")
    fun contar(): Flow<Int>

    /**
     * Todos os feedbacks com a resposta do card junto (LEFT JOIN: a resposta
     * vem `null` se o card não existe mais no aparelho), na ordem de criação.
     */
    @Query(
        "SELECT feedback_de_cards.*, cards.answer AS resposta " +
            "FROM feedback_de_cards " +
            "LEFT JOIN cards ON cards.id = feedback_de_cards.cardId " +
            "ORDER BY feedback_de_cards.criadoEm, feedback_de_cards.id",
    )
    suspend fun buscarTodosComResposta(): List<FeedbackComResposta>

    /** Apaga todos os feedbacks (ação "Limpar feedback" da Home). */
    @Query("DELETE FROM feedback_de_cards")
    suspend fun apagarTudo()
}

/** Um feedback acompanhado da resposta do card avaliado (join para o export). */
data class FeedbackComResposta(
    @Embedded val feedback: FeedbackDeCardEntity,
    val resposta: String?,
)
