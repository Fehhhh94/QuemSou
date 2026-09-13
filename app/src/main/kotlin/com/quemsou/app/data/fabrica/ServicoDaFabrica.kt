package com.quemsou.app.data.fabrica

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.quemsou.app.data.catalogo.BaralhoJson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class PedidoDeBaralho(val id: String, val tema: String, val quantidade: Int,
    val orientacoes: String, val feedback: String = "", val baralhoBase: String? = null)

@Serializable
data class PedidoNaFila(val id: String, val tema: String, val estado: String,
    val baralhoId: String? = null, val versao: Int = 0, val quantidade: Int = 10,
    val erro: String? = null, val podeAmpliar: Boolean = true)

/** A tentativa completa é persistida antes do POST, inclusive o snapshot das avaliações. */
interface ConexaoDaFabrica {
    suspend fun conectado(): Boolean
    suspend fun conectar(codigo: String)
    suspend fun listar(): List<PedidoNaFila>
    suspend fun cache(): List<PedidoNaFila>
    suspend fun baixar(id: String): BaralhoJson
    suspend fun pendente(): PedidoDeBaralho?
    suspend fun guardar(pedido: PedidoDeBaralho)
    suspend fun enviarPendente()
    suspend fun confirmar(id: String)
}

/** HTTPS autenticado. O token identifica o dono no servidor; o cliente não escolhe outro usuário. */
@Singleton
class ServicoDaFabrica internal constructor(private val preferencias: DataStore<Preferences>) : ConexaoDaFabrica {
    @Inject constructor(@ApplicationContext contexto: Context) : this(PreferenceDataStoreFactory.create {
        java.io.File(contexto.noBackupFilesDir, "fabrica.preferences_pb")
    })
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val cliente = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()

    override suspend fun conectado(): Boolean = preferencias.data.first()[CONEXAO] != null

    override suspend fun conectar(codigo: String) {
        val partes = codigo.trim().split('|')
        require(partes.size == 2 && partes[1].length in 32..256)
        val url = partes[0].toHttpUrl()
        require(url.isHttps && url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null)
        require(partes[1].matches(Regex("[A-Za-z0-9_-]{32,256}")))
        val anterior = preferencias.data.first()[CONEXAO]
        require(anterior == codigo.trim() || pendente() == null)
        json.decodeFromString<List<PedidoNaFila>>(requisitar("pedidos", null, codigo.trim()))
        preferencias.edit {
            if (anterior != codigo.trim()) it.remove(CACHE)
            it[CONEXAO] = codigo.trim()
        }
    }

    override suspend fun listar(): List<PedidoNaFila> {
        val lista = json.decodeFromString<List<PedidoNaFila>>(requisitar("pedidos"))
        preferencias.edit { it[CACHE] = json.encodeToString(lista) }
        return lista
    }
    override suspend fun cache(): List<PedidoNaFila> =
        preferencias.data.first()[CACHE]?.let { json.decodeFromString(it) } ?: emptyList()

    override suspend fun baixar(id: String): BaralhoJson {
        require(java.util.UUID.fromString(id).toString() == id)
        return json.decodeFromString(requisitar("pedidos/$id/baralho"))
    }
    override suspend fun pendente(): PedidoDeBaralho? =
        preferencias.data.first()[PENDENTE]?.let { json.decodeFromString(it) }

    override suspend fun guardar(pedido: PedidoDeBaralho) {
        preferencias.edit {
            check(it[PENDENTE] == null) { "Já existe uma tentativa aguardando confirmação" }
            it[PENDENTE] = json.encodeToString(pedido)
        }
    }

    override suspend fun enviarPendente() {
        val pedido = pendente() ?: return
        requisitar("pedidos", json.encodeToString(pedido))
        confirmar(pedido.id)
    }

    override suspend fun confirmar(id: String) {
        preferencias.edit {
            if (it[PENDENTE]?.let { valor -> json.decodeFromString<PedidoDeBaralho>(valor).id } == id) {
                it.remove(PENDENTE)
            }
        }
    }

    private suspend fun requisitar(caminho: String, corpo: String? = null, codigo: String? = null): String {
        val partes = (codigo ?: preferencias.data.first()[CONEXAO] ?: error("Sem conexão")).split('|')
        val url = partes[0].toHttpUrl().newBuilder().addPathSegments(caminho).build()
        val request = Request.Builder().url(url).header("Authorization", "Bearer ${partes[1]}")
            .apply { if (corpo != null) post(corpo.toRequestBody("application/json".toMediaType())) }.build()
        return withContext(Dispatchers.IO) {
            cliente.newCall(request).execute().use { resposta ->
                if (!resposta.isSuccessful) throw IOException("Fábrica indisponível (${resposta.code})")
                val entrada = resposta.body?.byteStream() ?: throw IOException("Resposta vazia")
                val bytes = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val lidos = entrada.read(buffer)
                    if (lidos < 0) break
                    require(bytes.size() + lidos <= LIMITE)
                    bytes.write(buffer, 0, lidos)
                }
                bytes.toString(Charsets.UTF_8.name())
            }
        }
    }

    companion object {
        private val CONEXAO = stringPreferencesKey("fabrica_conexao")
        private val CACHE = stringPreferencesKey("pedidos_cache")
        private val PENDENTE = stringPreferencesKey("pedido_pendente")
        private const val LIMITE = 8 * 1024 * 1024
    }
}
