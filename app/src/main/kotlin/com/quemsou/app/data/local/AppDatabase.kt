package com.quemsou.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Banco de dados Room do QuemSou.
 *
 * O conteúdo das tabelas `baralhos` e `cards` é um espelho dos baralhos
 * embarcados em `assets/cards.json` (e, na 5A parte 2, dos baixados do
 * catálogo), recarregado pelo `CardsImporter` quando a versão do asset avança
 * — por isso o schema pode evoluir com migrações simples sem perda de dados
 * do jogador.
 *
 * `feedback_de_cards` (modo dev de feedback, 5B parte 2) é a exceção: dado
 * do desenvolvedor gerado no aparelho, nunca reimportado — as migrações
 * precisam preservá-la.
 */
@Database(
    entities = [BaralhoEntity::class, CardEntity::class, FeedbackDeCardEntity::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun baralhoDao(): BaralhoDao

    abstract fun cardDao(): CardDao

    abstract fun feedbackDeCardDao(): FeedbackDeCardDao
}
