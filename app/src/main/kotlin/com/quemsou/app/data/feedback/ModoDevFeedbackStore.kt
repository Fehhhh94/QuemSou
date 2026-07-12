package com.quemsou.app.data.feedback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Preferência do **modo dev de feedback** (5B parte 2) — ferramenta de
 * desenvolvedor, não feature: com o modo desligado (padrão), nenhum composable
 * de feedback entra na composição e o jogador comum nunca vê nada. A única
 * entrada de UI para alternar é o easter egg de toque longo no título da Home.
 *
 * Abstraído para os testes JVM dos ViewModels usarem um fake em memória.
 */
interface ModoDevFeedbackStore {

    /** `true` com o modo dev de feedback ligado; padrão `false`. */
    val modoDevFeedback: Flow<Boolean>

    /** Alterna o modo e retorna o novo valor (para o aviso da Home). */
    suspend fun alternar(): Boolean
}

/** Implementação real: persiste a preferência no DataStore Preferences. */
class DataStoreModoDevFeedbackStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ModoDevFeedbackStore {

    override val modoDevFeedback: Flow<Boolean> =
        dataStore.data.map { preferencias -> preferencias[CHAVE_MODO_DEV] ?: false }

    override suspend fun alternar(): Boolean {
        val novas = dataStore.edit { preferencias ->
            preferencias[CHAVE_MODO_DEV] = !(preferencias[CHAVE_MODO_DEV] ?: false)
        }
        return novas[CHAVE_MODO_DEV] ?: false
    }

    private companion object {
        val CHAVE_MODO_DEV = booleanPreferencesKey("modo_dev_feedback")
    }
}
