package com.quemsou.app.data.catalogo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cache em disco do último índice do catálogo baixado com sucesso: offline,
 * a tela de catálogo mostra o último estado conhecido (baixados funcionais;
 * não-baixados esmaecidos). Sem expiração — o último índice vale até ser
 * substituído pelo próximo download bem-sucedido.
 */
interface CacheDoIndice {

    /** Conteúdo bruto do último índice salvo, ou `null` se nunca houve um. */
    suspend fun ler(): String?

    /** Substitui o cache pelo [conteudo] recém-baixado. */
    suspend fun salvar(conteudo: String)
}

/** Implementação real: um arquivo em `filesDir`. */
class ArquivoCacheDoIndice @Inject constructor(
    @ApplicationContext private val context: Context,
) : CacheDoIndice {

    private val arquivo: File
        get() = File(context.filesDir, NOME_DO_ARQUIVO)

    override suspend fun ler(): String? = withContext(Dispatchers.IO) {
        arquivo.takeIf { it.exists() }?.readText()
    }

    override suspend fun salvar(conteudo: String) = withContext(Dispatchers.IO) {
        arquivo.writeText(conteudo)
    }

    private companion object {
        const val NOME_DO_ARQUIVO = "catalogo_indice.json"
    }
}
