package com.quemsou.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Banco de dados Room do QuemSou.
 *
 * O conteúdo da tabela `cards` é um espelho de `assets/cards.json`, recarregado
 * pelo `CardsImporter` quando a versão do asset avança — por isso o schema pode
 * evoluir com migrações simples ou destrutivas sem perda de dados do jogador.
 */
@Database(entities = [CardEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
}
