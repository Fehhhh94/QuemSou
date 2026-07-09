package com.quemsou.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Acesso à tabela `baralhos`. Todas as operações são one-shot (`suspend`);
 * queries observáveis com `Flow` entram quando a tela de catálogo (5A
 * parte 2) precisar delas.
 */
@Dao
interface BaralhoDao {

    /** Insere todos os baralhos da lista, substituindo em caso de conflito de id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(baralhos: List<BaralhoEntity>)

    /**
     * Remove todos os baralhos da tabela — os cards vão junto, em cascata
     * (FK de [CardEntity]).
     */
    @Query("DELETE FROM baralhos")
    suspend fun limparTabela()

    /** Busca os baralhos dos [ids] informados. */
    @Query("SELECT * FROM baralhos WHERE id IN (:ids)")
    suspend fun buscarPorIds(ids: List<String>): List<BaralhoEntity>
}
