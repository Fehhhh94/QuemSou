package com.quemsou.app.data.importer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fonte do conteúdo bruto de `cards.json`. Abstraída para os testes JVM do
 * importador não dependerem de `Context`/assets Android.
 */
fun interface FonteDeCardsJson {
    /** Lê o conteúdo completo do JSON de cards. */
    suspend fun ler(): String
}

/** Implementação real: lê `cards.json` dos assets do APK. */
class AssetsFonteDeCardsJson @Inject constructor(
    @ApplicationContext private val context: Context,
) : FonteDeCardsJson {

    override suspend fun ler(): String = withContext(Dispatchers.IO) {
        context.assets.open(NOME_DO_ARQUIVO).bufferedReader().use { it.readText() }
    }

    private companion object {
        const val NOME_DO_ARQUIVO = "cards.json"
    }
}
