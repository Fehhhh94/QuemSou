package com.quemsou.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.quemsou.app.data.RepositorioDeCardsLocal
import com.quemsou.app.data.catalogo.ArquivoCacheDoIndice
import com.quemsou.app.data.catalogo.CacheDoIndice
import com.quemsou.app.data.catalogo.ClienteDoCatalogoFirestore
import com.quemsou.app.data.catalogo.ClienteDoCatalogoNaNuvem
import com.quemsou.app.data.catalogo.FonteDoCatalogo
import com.quemsou.app.data.catalogo.FonteDoCatalogoFirestore
import com.quemsou.app.data.espelho.KtorServidorDoEspelho
import com.quemsou.app.data.espelho.ServidorDoEspelho
import com.quemsou.app.data.feedback.DataStoreModoDevFeedbackStore
import com.quemsou.app.data.feedback.ModoDevFeedbackStore
import com.quemsou.app.data.feedback.RegistroDeFeedback
import com.quemsou.app.data.feedback.RegistroDeFeedbackLocal
import com.quemsou.app.data.feedback.AgendadorDaSincronizacaoDeFeedback
import com.quemsou.app.data.feedback.AgendadorDaSincronizacaoWorkManager
import com.quemsou.app.data.feedback.DestinoDeFeedbackNaNuvem
import com.quemsou.app.data.feedback.DestinoDeFeedbackNoFirestore
import com.quemsou.app.data.importer.AssetsFonteDeCardsJson
import com.quemsou.app.data.importer.CardsVersionStore
import com.quemsou.app.data.importer.DataStoreCardsVersionStore
import com.quemsou.app.data.importer.FonteDeCardsJson
import com.quemsou.app.data.local.AppDatabase
import com.quemsou.app.data.local.BaralhoDao
import com.quemsou.app.data.local.CardDao
import com.quemsou.app.data.local.FeedbackDeCardDao
import com.quemsou.app.data.local.MIGRACAO_1_2
import com.quemsou.app.data.local.MIGRACAO_2_3
import com.quemsou.app.data.local.MIGRACAO_3_4
import com.quemsou.app.data.local.MIGRACAO_4_5
import com.quemsou.app.data.local.MIGRACAO_5_6
import com.quemsou.app.data.local.MIGRACAO_6_7
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
    abstract fun bindConexaoDaFabrica(impl: com.quemsou.app.data.fabrica.ServicoDaFabrica): com.quemsou.app.data.fabrica.ConexaoDaFabrica

    @Binds
    abstract fun bindDestinoDeBaralhos(impl: com.quemsou.app.data.catalogo.InstaladorDeBaralhos): com.quemsou.app.data.catalogo.DestinoDeBaralhos

    @Binds
    abstract fun bindFonteDeCardsJson(impl: AssetsFonteDeCardsJson): FonteDeCardsJson

    @Binds
    abstract fun bindCardsVersionStore(impl: DataStoreCardsVersionStore): CardsVersionStore

    @Binds
    abstract fun bindRepositorioDeCards(impl: RepositorioDeCardsLocal): RepositorioDeCards

    @Binds
    abstract fun bindFonteDoCatalogo(impl: FonteDoCatalogoFirestore): FonteDoCatalogo

    @Binds
    abstract fun bindClienteDoCatalogoNaNuvem(
        impl: ClienteDoCatalogoFirestore,
    ): ClienteDoCatalogoNaNuvem

    @Binds
    abstract fun bindCacheDoIndice(impl: ArquivoCacheDoIndice): CacheDoIndice

    @Binds
    abstract fun bindModoDevFeedbackStore(impl: DataStoreModoDevFeedbackStore): ModoDevFeedbackStore

    @Binds
    abstract fun bindRegistroDeFeedback(impl: RegistroDeFeedbackLocal): RegistroDeFeedback

    @Binds
    abstract fun bindAgendadorDaSincronizacaoDeFeedback(
        impl: AgendadorDaSincronizacaoWorkManager,
    ): AgendadorDaSincronizacaoDeFeedback

    @Binds
    abstract fun bindDestinoDeFeedbackNaNuvem(
        impl: DestinoDeFeedbackNoFirestore,
    ): DestinoDeFeedbackNaNuvem

    /** Singleton: só pode existir um servidor disputando a porta do espelho. */
    @Binds
    @Singleton
    abstract fun bindServidorDoEspelho(impl: KtorServidorDoEspelho): ServidorDoEspelho

    companion object {

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "quemsou.db")
                .addMigrations(
                    MIGRACAO_1_2,
                    MIGRACAO_2_3,
                    MIGRACAO_3_4,
                    MIGRACAO_4_5,
                    MIGRACAO_5_6,
                    MIGRACAO_6_7,
                )
                .build()

        @Provides
        fun provideBaralhoDao(database: AppDatabase): BaralhoDao = database.baralhoDao()

        @Provides
        fun provideCardDao(database: AppDatabase): CardDao = database.cardDao()

        @Provides
        fun provideFeedbackDeCardDao(database: AppDatabase): FeedbackDeCardDao =
            database.feedbackDeCardDao()

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("quemsou_prefs") },
            )
    }
}
