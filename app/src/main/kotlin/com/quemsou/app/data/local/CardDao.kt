package com.quemsou.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Acesso à tabela `cards`. Todas as operações são one-shot (`suspend`);
 * queries observáveis com `Flow` entram quando a UI precisar delas (Fase 3).
 */
@Dao
interface CardDao {

    /** Insere todos os cards da lista, substituindo em caso de conflito de id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(cards: List<CardEntity>)

    /** Remove todos os cards da tabela. */
    @Query("DELETE FROM cards")
    suspend fun limparTabela()

    /** Busca os cards de uma categoria (`name` da [com.quemsou.app.domain.model.CardCategory]). */
    @Query("SELECT * FROM cards WHERE category = :categoria")
    suspend fun buscarPorCategoria(categoria: String): List<CardEntity>

    /** Busca todos os cards da tabela. */
    @Query("SELECT * FROM cards")
    suspend fun buscarTodas(): List<CardEntity>

    /** Conta quantos cards existem na tabela. */
    @Query("SELECT COUNT(*) FROM cards")
    suspend fun contar(): Int
}
