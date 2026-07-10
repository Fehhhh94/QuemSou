package com.quemsou.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Acesso à tabela `cards`. Todas as operações são one-shot (`suspend`);
 * queries observáveis com `Flow` entram quando a UI precisar delas.
 */
@Dao
interface CardDao {

    /** Insere todos os cards da lista, substituindo em caso de conflito de id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(cards: List<CardEntity>)

    /** Remove os cards do baralho [baralhoId] — e só os dele. */
    @Query("DELETE FROM cards WHERE baralhoId = :baralhoId")
    suspend fun removerPorBaralho(baralhoId: String)

    /** Busca os cards dos baralhos com os [baralhoIds] informados. */
    @Query("SELECT * FROM cards WHERE baralhoId IN (:baralhoIds)")
    suspend fun buscarPorBaralhos(baralhoIds: List<String>): List<CardEntity>
}
