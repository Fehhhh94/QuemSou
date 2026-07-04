package com.quemsou.app.data.importer

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Guarda a última versão de `cards.json` importada para o banco. Abstraído
 * para os testes JVM do importador usarem um fake em memória.
 */
interface CardsVersionStore {

    /** Última versão importada; [VERSAO_NUNCA_IMPORTADA] se nunca houve importação. */
    suspend fun versaoImportada(): Int

    /** Registra [versao] como a última versão importada. */
    suspend fun salvarVersaoImportada(versao: Int)

    companion object {
        /** Valor default quando nenhuma importação aconteceu ainda. */
        const val VERSAO_NUNCA_IMPORTADA = 0
    }
}

/** Implementação real: persiste a versão no DataStore Preferences. */
class DataStoreCardsVersionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : CardsVersionStore {

    override suspend fun versaoImportada(): Int =
        dataStore.data.first()[CHAVE_VERSAO] ?: CardsVersionStore.VERSAO_NUNCA_IMPORTADA

    override suspend fun salvarVersaoImportada(versao: Int) {
        dataStore.edit { preferencias -> preferencias[CHAVE_VERSAO] = versao }
    }

    private companion object {
        val CHAVE_VERSAO = intPreferencesKey("cards_db_version")
    }
}
