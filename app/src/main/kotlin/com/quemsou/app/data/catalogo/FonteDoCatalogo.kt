package com.quemsou.app.data.catalogo

import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Fonte remota do catálogo estático. Abstraída para os testes JVM usarem
 * fakes — **nenhum teste bate na rede real**. Falhas de rede viram
 * [IOException]; o repositório as traduz em estado offline/erro legível.
 */
interface FonteDoCatalogo {

    /** Baixa o conteúdo bruto do índice do catálogo. */
    suspend fun buscarIndice(): String

    /**
     * Baixa o JSON completo de um baralho pela [url], reportando o progresso
     * (0f–1f) em [aoProgresso]. Cancelamento da coroutine cancela a chamada
     * HTTP.
     */
    suspend fun baixarBaralho(url: String, aoProgresso: (Float) -> Unit): String
}

/**
 * Implementação real com OkHttp puro — cliente HTTP leve; a única rede do
 * app é esta (tela de catálogo).
 */
class HttpFonteDoCatalogo @Inject constructor() : FonteDoCatalogo {

    private val client = OkHttpClient()

    override suspend fun buscarIndice(): String = baixar(URL_DO_INDICE) {}

    override suspend fun baixarBaralho(url: String, aoProgresso: (Float) -> Unit): String =
        baixar(url, aoProgresso)

    private suspend fun baixar(url: String, aoProgresso: (Float) -> Unit): String {
        val response = executar(Request.Builder().url(url).build())
        return withContext(Dispatchers.IO) {
            response.use { resposta ->
                if (!resposta.isSuccessful) {
                    throw IOException("HTTP ${resposta.code} ao baixar $url")
                }
                val corpo = resposta.body ?: throw IOException("Resposta sem corpo ao baixar $url")
                val total = corpo.contentLength()
                val bytes = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(TAMANHO_DO_BUFFER)
                corpo.byteStream().use { entrada ->
                    while (true) {
                        ensureActive()
                        val lidos = entrada.read(buffer)
                        if (lidos == -1) break
                        bytes.write(buffer, 0, lidos)
                        if (total > 0) aoProgresso(bytes.size().toFloat() / total)
                    }
                }
                aoProgresso(1f)
                bytes.toString(Charsets.UTF_8.name())
            }
        }
    }

    /** `Call.enqueue` como suspend, com cancelamento propagado à chamada HTTP. */
    private suspend fun executar(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response) { response.close() }
                    }
                },
            )
        }

    companion object {
        /**
         * URL do índice do catálogo estático — repositório `QuemSou-Baralhos`
         * no GitHub (raw). É a ÚNICA constante de URL do app.
         *
         * TODO_URL_CATALOGO: trocar pelo raw real assim que o Felipe criar o
         * repositório público `Fehhhh94/QuemSou-Baralhos` e publicar os
         * arquivos de `catalogo-seed/` (ver README da pasta).
         */
        const val URL_DO_INDICE =
            "https://raw.githubusercontent.com/Fehhhh94/QuemSou-Baralhos/main/indice.json"

        private const val TAMANHO_DO_BUFFER = 8 * 1024
    }
}
