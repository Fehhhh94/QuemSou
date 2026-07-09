package com.quemsou.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.quemsou.app.data.RepositorioDeCardsLocal
import com.quemsou.app.data.importer.AssetsFonteDeCardsJson
import com.quemsou.app.data.importer.CardsVersionStore
import com.quemsou.app.data.importer.DataStoreCardsVersionStore
import com.quemsou.app.data.importer.FonteDeCardsJson
import com.quemsou.app.data.local.AppDatabase
import com.quemsou.app.data.local.BaralhoDao
import com.quemsou.app.data.local.CardDao
import com.quemsou.app.data.local.MIGRACAO_1_2
import com.quemsou.app.domain.repository.RepositorioDeCards
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt da camada de dados: banco Room, DAOs, DataStore e as
 * implementações reais das abstrações do importador de cards.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindFonteDeCardsJson(impl: AssetsFonteDeCardsJson): FonteDeCardsJson

    @Binds
    abstract fun bindCardsVersionStore(impl: DataStoreCardsVersionStore): CardsVersionStore

    @Binds
    abstract fun bindRepositorioDeCards(impl: RepositorioDeCardsLocal): RepositorioDeCards

    companion object {

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "quemsou.db")
                .addMigrations(MIGRACAO_1_2)
                .build()

        @Provides
        fun provideBaralhoDao(database: AppDatabase): BaralhoDao = database.baralhoDao()

        @Provides
        fun provideCardDao(database: AppDatabase): CardDao = database.cardDao()

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("quemsou_prefs") },
            )
    }
}
