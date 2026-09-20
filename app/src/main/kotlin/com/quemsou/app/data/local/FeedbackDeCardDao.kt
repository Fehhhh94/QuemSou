package com.quemsou.app.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Avaliações de cartas e de dicas. Cartas inserem histórico; dicas permitem
 * editar o voto da mesma ocorrência identificada pelo snapshot de contexto.
 */
@Dao
interface FeedbackDeCardDao {

    /** Insere um feedback novo — sempre uma linha própria, nunca substitui. */
    @Insert
    suspend fun inserir(feedback: FeedbackDeCardEntity): Long

    @Update
    suspend fun atualizar(feedback: FeedbackDeCardEntity)

    @Query("SELECT * FROM feedback_de_cards WHERE resultadoDoTurno = 'DICA_REVELADA' AND contextoJson = :contextoJson LIMIT 1")
    suspend fun buscarDica(contextoJson: String): FeedbackDeCardEntity?

    /** Voto repetido na mesma dica/rodada/sessão é uma edição, em transação. */
    @Transaction
    suspend fun registrar(feedback: FeedbackDeCardEntity): FeedbackDeCardEntity {
        val anterior = if (feedback.resultadoDoTurno == "DICA_REVELADA") buscarDica(feedback.contextoJson) else null
        if (anterior == null) {
            return feedback.copy(id = inserir(feedback))
        }
        val atualizado = feedback.copy(
            id = anterior.id,
            criadoEm = anterior.criadoEm,
            revisaoLocal = anterior.revisaoLocal + 1,
            sincronizadoEm = null,
        )
        atualizar(atualizado)
        return atualizado
    }

    @Query(
        "SELECT * FROM feedback_de_cards " +
            "WHERE resultadoDoTurno = 'DICA_REVELADA' AND sincronizadoEm IS NULL " +
            "ORDER BY criadoEm, id LIMIT :limite",
    )
    suspend fun buscarDicasPendentes(limite: Int): List<FeedbackDeCardEntity>

    @Query(
        "UPDATE feedback_de_cards SET sincronizadoEm = :sincronizadoEm " +
            "WHERE id = :id AND revisaoLocal = :revisaoLocal",
    )
    suspend fun marcarSincronizado(id: Long, revisaoLocal: Int, sincronizadoEm: Long): Int

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
