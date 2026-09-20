package com.quemsou.app.data.catalogo

/**
 * Transporte do catálogo Firebase. Testes usam fakes; não há fallback GitHub.
 * Falhas de rede são traduzidas pelo repositório em estado offline/erro.
 */
interface FonteDoCatalogo {
    suspend fun buscarIndice(): String
    suspend fun baixarBaralho(url: String, aoProgresso: (Float) -> Unit): String
}
