package com.quemsou.app.data.espelho

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Desfecho de uma tentativa de subir o espelho. */
sealed interface ResultadoDoInicio {

    /** No ar; [endereco] é a URL completa a anunciar no QR. */
    data class NoAr(val endereco: String) : ResultadoDoInicio

    /** Sem interface de rede local — não adianta subir servidor nenhum. */
    data object SemRede : ResultadoDoInicio

    /** Nenhuma porta livre na faixa tentada. */
    data object SemPorta : ResultadoDoInicio
}

/**
 * Servidor HTTP local do espelho de leitura (4A).
 *
 * O aparelho do anfitrião vira o servidor da mesa: os outros jogadores abrem
 * o endereço no navegador do próprio celular e, na vez deles de ler, veem
 * dica e resposta ali. Nada sai do Wi-Fi da mesa — não há serviço externo,
 * conta nem nuvem no caminho.
 *
 * Abstraído como interface para o `SetupViewModel` ser testável na JVM sem
 * subir socket nenhum, no mesmo espírito de `FonteDoCatalogo`.
 */
interface ServidorDoEspelho {

    /** URL a anunciar (ex.: `http://192.168.0.7:8080`), ou `null` se parado. */
    val endereco: StateFlow<String?>

    /** Mapa jogadorId → já entrou pela rede; alimenta o ✓ ao vivo do Setup. */
    val conectados: StateFlow<Map<String, Boolean>>

    /** Quem segura o aparelho anfitrião, ou `null` se ninguém foi marcado. */
    val esteAparelho: StateFlow<String?>

    /** Sobe o servidor com o elenco atual da partida. */
    suspend fun iniciar(jogadores: List<JogadorDoEspelho>): ResultadoDoInicio

    /** Reflete no espelho uma mudança na lista de jogadores; no-op se parado. */
    fun atualizarJogadores(jogadores: List<JogadorDoEspelho>)

    /** Marca quem está com o celular na mão; `null` desmarca. */
    fun marcarEsteAparelho(jogadorId: String?)

    /** Devolve o lugar de [jogadorId] à mesa, esquecendo a sessão de rede. */
    fun liberarLugar(jogadorId: String)

    /** Derruba o servidor e esquece as sessões. */
    fun parar()
}

/**
 * Implementação Android com Ktor embarcado, engine CIO, sobre coroutines e
 * sem Netty (que não roda bem em Android).
 *
 * Rotas:
 * - `GET /` e `GET /app.js` → cliente estático, servido de `assets/espelho/`
 *   (sem CDN: a mesa pode estar sem internet).
 * - `GET /jogadores` → elenco com quais nomes já estão em uso.
 * - `POST /entrar` → reivindica um jogador e devolve o token da sessão.
 * - `GET /estado?jogador=<id>&token=<token>` → canal SSE autenticado pelo
 *   par da sessão. **Nesta parte 1 emite só `{"fase":"aguardando"}` e
 *   keep-alives**: a estrutura do canal fica pronta; o conteúdo (dica e
 *   resposta da vez) vem na parte 2.
 *
 * O JSON é montado à mão com kotlinx.serialization e devolvido como texto —
 * o plugin de negociação de conteúdo só somaria dependência para quatro
 * rotas.
 */
@Singleton
class KtorServidorDoEspelho @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : ServidorDoEspelho {

    private val sessoes = RegistroDeSessoes()

    private val _endereco = MutableStateFlow<String?>(null)
    override val endereco: StateFlow<String?> = _endereco.asStateFlow()

    override val conectados: StateFlow<Map<String, Boolean>> = sessoes.conectados

    override val esteAparelho: StateFlow<String?> = sessoes.esteAparelho

    private val json = Json { ignoreUnknownKeys = true }

    /** Escopo próprio: parar o servidor bloqueia e não pode segurar a UI. */
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var servidor: EmbeddedServer<*, *>? = null

    /** Serializa a publicação e a retirada do servidor ativo. */
    private val travaDoServidor = Any()

    /**
     * Invalida uma subida que ainda esteja em andamento quando [parar] for
     * chamado (por exemplo, porque o ViewModel saiu da tela).
     */
    private val geracao = AtomicLong(0L)

    override suspend fun iniciar(jogadores: List<JogadorDoEspelho>): ResultadoDoInicio {
        parar()
        val minhaGeracao = geracao.incrementAndGet()
        val ip = withContext(Dispatchers.IO) { EnderecoLocal.descobrir() }
            ?: return ResultadoDoInicio.SemRede
        coroutineContext.ensureActive()

        val url = try {
            withContext(Dispatchers.IO) {
                subirNaPrimeiraPorta(minhaGeracao, jogadores, ip)
            }
        } catch (cancelamento: CancellationException) {
            parar()
            throw cancelamento
        } ?: return ResultadoDoInicio.SemPorta
        return ResultadoDoInicio.NoAr(url)
    }

    override fun atualizarJogadores(jogadores: List<JogadorDoEspelho>) {
        if (servidor == null) return
        sessoes.definirJogadores(jogadores)
    }

    override fun marcarEsteAparelho(jogadorId: String?) {
        if (servidor == null) return
        sessoes.marcarEsteAparelho(jogadorId)
    }

    override fun liberarLugar(jogadorId: String) {
        if (servidor == null) return
        sessoes.liberarLugar(jogadorId)
    }

    override fun parar() {
        val emAndamento = synchronized(travaDoServidor) {
            geracao.incrementAndGet()
            servidor.also {
                servidor = null
                _endereco.value = null
                sessoes.limpar()
            }
        }
        if (emAndamento != null) {
            escopo.launch { emAndamento.stop(GRACA_AO_PARAR_MS, ESPERA_AO_PARAR_MS) }
        }
    }

    private fun Application.rotas() {
        routing {
            get("/") { call.responderAsset("index.html", ContentType.Text.Html) }
            get("/app.js") { call.responderAsset("app.js", ContentType.Application.JavaScript) }

            get("/jogadores") {
                val emUso = sessoes.conectados.value
                val corpo = JogadoresJson(
                    sessoes.jogadoresOferecidos().map { jogador ->
                        JogadorJson(
                            id = jogador.id,
                            nome = jogador.nome,
                            emUso = emUso[jogador.id] == true,
                        )
                    },
                )
                call.responderJson(json.encodeToString(corpo))
            }

            post("/entrar") {
                val pedido = runCatching {
                    json.decodeFromString<PedidoDeEntrada>(call.receiveText())
                }.getOrNull()
                if (pedido == null) {
                    call.responderJson(
                        json.encodeToString(ErroJson(ERRO_PEDIDO_INVALIDO)),
                        HttpStatusCode.BadRequest,
                    )
                    return@post
                }
                when (val resultado = sessoes.reivindicar(pedido.jogadorId, pedido.token)) {
                    is ResultadoDaReivindicacao.Sucesso -> call.responderJson(
                        json.encodeToString(
                            EntradaAceitaJson(
                                token = resultado.token,
                                jogadorId = resultado.jogador.id,
                                nome = resultado.jogador.nome,
                            ),
                        ),
                    )

                    ResultadoDaReivindicacao.JaTomado -> call.responderJson(
                        json.encodeToString(ErroJson(ERRO_JA_TOMADO)),
                        HttpStatusCode.Conflict,
                    )

                    ResultadoDaReivindicacao.JogadorDesconhecido -> call.responderJson(
                        json.encodeToString(ErroJson(ERRO_DESCONHECIDO)),
                        HttpStatusCode.NotFound,
                    )
                }
            }

            get("/estado") {
                val jogadorId = call.request.queryParameters["jogador"]
                val token = call.request.queryParameters["token"]
                if (jogadorId == null || token == null || sessoes.jogadorDe(token) != jogadorId) {
                    call.responderJson(
                        json.encodeToString(ErroJson(ERRO_SESSAO_INVALIDA)),
                        HttpStatusCode.Unauthorized,
                    )
                    return@get
                }
                call.response.headers.append(CABECALHO_CACHE, SEM_CACHE)
                val aguardando = json.encodeToString(EstadoJson(FASE_AGUARDANDO))
                call.respondTextWriter(
                    contentType = ContentType.Text.EventStream.withCharset(Charsets.UTF_8),
                ) {
                    // Reconexão automática do EventSource: se o canal cair,
                    // o navegador volta sozinho sem o jogador fazer nada.
                    write("retry: $RECONEXAO_SSE_MS\n\n")
                    write("data: $aguardando\n\n")
                    flush()
                    // Sem tráfego, roteador e navegador derrubam a conexão
                    // ociosa; o comentário SSE mantém o canal vivo até a
                    // parte 2 ter o que dizer de verdade. A escrita falha
                    // quando o jogador fecha a aba — fim natural da coroutine.
                    runCatching {
                        while (true) {
                            delay(KEEP_ALIVE_MS)
                            write(": keep-alive\n\n")
                            flush()
                        }
                    }
                }
            }
        }
    }

    private suspend fun ApplicationCall.responderJson(
        corpo: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) {
        response.headers.append(CABECALHO_CACHE, SEM_CACHE)
        respondText(
            text = corpo,
            contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8),
            status = status,
        )
    }

    private suspend fun ApplicationCall.responderAsset(arquivo: String, tipo: ContentType) {
        val bytes = withContext(Dispatchers.IO) {
            contexto.assets.open("$PASTA_DO_CLIENTE/$arquivo").use { it.readBytes() }
        }
        respondBytes(bytes, tipo.withCharset(Charsets.UTF_8))
    }

    /**
     * Tenta subir o Ktor em cada porta da faixa. Testar com `ServerSocket` e
     * fechá-lo antes do bind real deixava uma janela de corrida; só a subida
     * da engine prova que a porta foi conquistada.
     */
    private fun subirNaPrimeiraPorta(
        minhaGeracao: Long,
        jogadores: List<JogadorDoEspelho>,
        ip: String,
    ): String? {
        for (porta in PORTA_INICIAL until PORTA_INICIAL + PORTAS_TENTADAS) {
            val candidato = embeddedServer(CIO, port = porta, host = HOST_DE_ESCUTA) { rotas() }
            try {
                candidato.start(wait = false)
                val publicado = synchronized(travaDoServidor) {
                    if (geracao.get() != minhaGeracao) {
                        false
                    } else {
                        sessoes.definirJogadores(jogadores)
                        servidor = candidato
                        _endereco.value = "http://$ip:$porta"
                        true
                    }
                }
                if (publicado) return _endereco.value
                candidato.stop(GRACA_AO_PARAR_MS, ESPERA_AO_PARAR_MS)
                return null
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                runCatching { candidato.stop(0L, 0L) }
            }
        }
        return null
    }

    private companion object {
        /** Escuta em todas as interfaces: o QR é que anuncia o IP específico. */
        const val HOST_DE_ESCUTA = "0.0.0.0"
        const val PORTA_INICIAL = 8080
        const val PORTAS_TENTADAS = 10
        const val PASTA_DO_CLIENTE = "espelho"

        const val GRACA_AO_PARAR_MS = 200L
        const val ESPERA_AO_PARAR_MS = 1_000L
        const val KEEP_ALIVE_MS = 15_000L
        const val RECONEXAO_SSE_MS = 3_000

        const val CABECALHO_CACHE = "Cache-Control"
        const val SEM_CACHE = "no-cache"

        const val FASE_AGUARDANDO = "aguardando"
        const val ERRO_JA_TOMADO = "ja_tomado"
        const val ERRO_DESCONHECIDO = "desconhecido"
        const val ERRO_PEDIDO_INVALIDO = "pedido_invalido"
        const val ERRO_SESSAO_INVALIDA = "sessao_invalida"
    }
}

@Serializable
private data class JogadoresJson(val jogadores: List<JogadorJson>)

@Serializable
private data class JogadorJson(val id: String, val nome: String, val emUso: Boolean)

@Serializable
private data class PedidoDeEntrada(val jogadorId: String, val token: String? = null)

@Serializable
private data class EntradaAceitaJson(val token: String, val jogadorId: String, val nome: String)

@Serializable
private data class ErroJson(val erro: String)

/** Envelope do canal SSE. Na parte 2 ganha dica, resposta e vez de quem lê. */
@Serializable
private data class EstadoJson(val fase: String)
