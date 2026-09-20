package com.quemsou.app.data.catalogo

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Metadados publicados que formam o índice do catálogo no Firestore. */
data class ManifestoDoCatalogoNaNuvem(
    val id: String,
    val nome: String,
    val categoria: String,
    val colecaoId: String,
    val colecaoNome: String,
    val colecaoIcone: String,
    val versao: Int,
    val versaoId: String,
    val estado: String,
    val quantidadeDeCards: Int,
    val quantidadeDeBlocos: Int,
    val descricao: String,
    val tamanhoEmBytes: Long,
    val hashDoConteudo: String,
    val visibilidade: String,
)

/** Cabeçalho imutável de uma versão completa de baralho. */
data class VersaoDoBaralhoNaNuvem(
    val id: String,
    val nome: String,
    val categoria: String,
    val colecaoId: String,
    val colecaoNome: String,
    val colecaoIcone: String,
    val versao: Int,
    val estado: String,
    val quantidadeDeCards: Int,
    val quantidadeDeBlocos: Int,
    val hashDoConteudo: String,
)

/** Bloco imutável de cards; o JSON preserva inclusive o banco ampliado de dicas. */
data class BlocoDeCardsNaNuvem(
    val ordem: Int,
    val quantidadeDeCards: Int,
    val conteudoJson: String,
    val hashDoConteudo: String,
)

/** Fronteira testável entre o contrato do catálogo e o SDK Android do Firebase. */
interface ClienteDoCatalogoNaNuvem {
    val configurado: Boolean
    suspend fun listarManifestos(): List<ManifestoDoCatalogoNaNuvem>
    suspend fun buscarVersao(baralhoId: String, versaoId: String): VersaoDoBaralhoNaNuvem
    suspend fun buscarBlocos(baralhoId: String, versaoId: String): List<BlocoDeCardsNaNuvem>
}

/**
 * Adapta o catálogo versionado do Firestore ao contrato JSON já validado pelo app.
 * Assim, parser, UI, cache em disco e instalação no Room continuam sendo um único
 * caminho, sem uma segunda regra editorial para a nuvem.
 */
@Singleton
class FonteDoCatalogoFirestore @Inject constructor(
    private val cliente: ClienteDoCatalogoNaNuvem,
) : FonteDoCatalogo {

    override suspend fun buscarIndice(): String = proteger {
        exigirConfiguracao()
        val manifestos = cliente.listarManifestos()
        if (manifestos.map { it.id }.distinct().size != manifestos.size) {
            throw IOException("O Firestore devolveu ids de baralho duplicados.")
        }
        val entradas = manifestos.sortedBy { it.id }.map { manifesto ->
            manifesto.validarEstrutura()
            EntradaDoIndiceJson(
                id = manifesto.id,
                nome = manifesto.nome,
                categoria = manifesto.categoria,
                colecao = ColecaoJson(
                    id = manifesto.colecaoId,
                    nome = manifesto.colecaoNome,
                    icone = manifesto.colecaoIcone,
                ),
                versao = manifesto.versao,
                estado = manifesto.estado,
                quantidadeDeCards = manifesto.quantidadeDeCards,
                url = EnderecoDoBaralhoNoFirestore.criar(manifesto.id, manifesto.versaoId),
                descricao = manifesto.descricao,
                tamanhoEmBytes = manifesto.tamanhoEmBytes,
            )
        }
        JSON.encodeToString(IndiceDoCatalogoJson(entradas))
    }

    override suspend fun baixarBaralho(
        url: String,
        aoProgresso: (Float) -> Unit,
    ): String = proteger {
        exigirConfiguracao()
        val endereco = EnderecoDoBaralhoNoFirestore.ler(url)
        val versao = cliente.buscarVersao(endereco.baralhoId, endereco.versaoId)
        if (versao.id != endereco.baralhoId) {
            throw IOException("A versão publicada pertence a outro baralho.")
        }
        if ("v${versao.versao}" != endereco.versaoId) {
            throw IOException("A versão publicada não corresponde ao endereço do catálogo.")
        }
        if (versao.quantidadeDeCards !in 1..500 || versao.quantidadeDeBlocos !in 1..250) {
            throw IOException("O cabeçalho do baralho publicado é inválido.")
        }
        val blocos = cliente.buscarBlocos(endereco.baralhoId, endereco.versaoId)
            .sortedBy { it.ordem }
        if (blocos.map { it.ordem } != (0 until versao.quantidadeDeBlocos).toList()) {
            throw IOException("O baralho publicado está com blocos ausentes ou duplicados.")
        }
        val cards = buildList {
            blocos.forEachIndexed { indice, bloco ->
                if (sha256(bloco.conteudoJson) != bloco.hashDoConteudo) {
                    throw IOException("O bloco ${bloco.ordem} não corresponde ao hash publicado.")
                }
                val doBloco = try {
                    JSON.decodeFromString<List<CardDoBaralhoJson>>(bloco.conteudoJson)
                } catch (falha: Exception) {
                    throw IOException("O bloco ${bloco.ordem} contém JSON inválido.", falha)
                }
                if (doBloco.size != bloco.quantidadeDeCards || doBloco.size !in 1..25) {
                    throw IOException("O bloco ${bloco.ordem} declara uma quantidade inválida de cards.")
                }
                addAll(doBloco)
                aoProgresso((indice + 1).toFloat() / versao.quantidadeDeBlocos)
            }
        }
        if (cards.size != versao.quantidadeDeCards) {
            throw IOException(
                "O baralho publicou ${versao.quantidadeDeCards} cards, mas entregou ${cards.size}.",
            )
        }
        val conteudo = JSON.encodeToString(
            BaralhoJson(
                id = versao.id,
                nome = versao.nome,
                categoria = versao.categoria,
                colecao = ColecaoJson(
                    id = versao.colecaoId,
                    nome = versao.colecaoNome,
                    icone = versao.colecaoIcone,
                ),
                versao = versao.versao,
                estado = versao.estado,
                cards = cards,
            ),
        )
        if (sha256(conteudo) != versao.hashDoConteudo) {
            throw IOException("O conteúdo do baralho não corresponde ao hash publicado.")
        }
        conteudo
    }

    private fun exigirConfiguracao() {
        if (!cliente.configurado) throw IOException("Firebase não configurado neste build.")
    }

    private fun ManifestoDoCatalogoNaNuvem.validarEstrutura() {
        if (versao < 1 || versaoId != "v$versao") {
            throw IOException("O manifesto '$id' aponta para uma versão inválida.")
        }
        if (quantidadeDeCards !in 1..500) {
            throw IOException("O manifesto '$id' declara uma quantidade inválida de cards.")
        }
        val minimoDeBlocos = (quantidadeDeCards + 24) / 25
        if (quantidadeDeBlocos !in minimoDeBlocos..quantidadeDeCards) {
            throw IOException("O manifesto '$id' declara uma quantidade inválida de blocos.")
        }
        if (tamanhoEmBytes < 1 || !HASH_SHA_256.matches(hashDoConteudo)) {
            throw IOException("O manifesto '$id' contém integridade inválida.")
        }
        if (visibilidade != "PUBLICO" && visibilidade != "PRIVADO") {
            throw IOException("O manifesto '$id' contém visibilidade inválida.")
        }
    }

    private suspend fun <T> proteger(acao: suspend () -> T): T = try {
        acao()
    } catch (cancelamento: CancellationException) {
        throw cancelamento
    } catch (falha: IOException) {
        throw falha
    } catch (falha: Exception) {
        throw IOException("Não foi possível consultar o catálogo no Firestore.", falha)
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
        val HASH_SHA_256 = Regex("^[a-f0-9]{64}$")

        fun sha256(texto: String): String = MessageDigest.getInstance("SHA-256")
            .digest(texto.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}

/** Endereço opaco usado apenas dentro do app; não é uma URL pública. */
private data class EnderecoDoBaralhoNoFirestore(
    val baralhoId: String,
    val versaoId: String,
) {
    companion object {
        private val PADRAO = Regex(
            "^firestore://catalogo/([A-Za-z0-9._-]{1,120})/versoes/(v[1-9][0-9]*)$",
        )

        fun criar(baralhoId: String, versaoId: String): String {
            require(PADRAO.matches("firestore://catalogo/$baralhoId/versoes/$versaoId")) {
                "Id de baralho ou versão incompatível com o Firestore."
            }
            return "firestore://catalogo/$baralhoId/versoes/$versaoId"
        }

        fun ler(url: String): EnderecoDoBaralhoNoFirestore {
            val grupos = PADRAO.matchEntire(url)?.groupValues
                ?: throw IOException("Endereço de baralho do Firestore inválido.")
            return EnderecoDoBaralhoNoFirestore(grupos[1], grupos[2])
        }
    }
}

/** Implementação Android real. Leituras exigem servidor para o cache ficar sob nosso controle. */
@Singleton
class ClienteDoCatalogoFirestore @Inject constructor(
    @ApplicationContext private val context: Context,
) : ClienteDoCatalogoNaNuvem {

    private val firebaseApp: FirebaseApp?
        get() = FirebaseApp.getApps(context).firstOrNull()

    override val configurado: Boolean
        get() = firebaseApp != null

    override suspend fun listarManifestos(): List<ManifestoDoCatalogoNaNuvem> {
        val firestore = firestoreAutenticado()
        val publicos = firestore.collection(COLECAO_CATALOGO)
            .whereEqualTo("publicado", true)
            .whereEqualTo("visibilidade", VISIBILIDADE_PUBLICA)
            .get(Source.SERVER)
            .aguardarCatalogo()
            .documents
            .map { it.paraManifesto() }
        val privados = try {
            firestore.collection(COLECAO_CATALOGO)
                .whereEqualTo("publicado", true)
                .whereEqualTo("visibilidade", VISIBILIDADE_PRIVADA)
                .get(Source.SERVER)
                .aguardarCatalogo()
                .documents
                .map { it.paraManifesto() }
        } catch (falha: FirebaseFirestoreException) {
            if (falha.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw falha
            emptyList()
        }
        return publicos + privados
    }

    override suspend fun buscarVersao(
        baralhoId: String,
        versaoId: String,
    ): VersaoDoBaralhoNaNuvem = firestoreAutenticado()
        .collection(COLECAO_CATALOGO)
        .document(baralhoId)
        .collection(COLECAO_VERSOES)
        .document(versaoId)
        .get(Source.SERVER)
        .aguardarCatalogo()
        .takeIf { it.exists() }
        ?.paraVersao()
        ?: throw IOException("A versão publicada do baralho não existe.")

    override suspend fun buscarBlocos(
        baralhoId: String,
        versaoId: String,
    ): List<BlocoDeCardsNaNuvem> = firestoreAutenticado()
        .collection(COLECAO_CATALOGO)
        .document(baralhoId)
        .collection(COLECAO_VERSOES)
        .document(versaoId)
        .collection(COLECAO_BLOCOS)
        .orderBy("ordem")
        .get(Source.SERVER)
        .aguardarCatalogo()
        .documents
        .map { it.paraBloco() }

    private suspend fun firestoreAutenticado(): FirebaseFirestore {
        val app = checkNotNull(firebaseApp) { "Firebase não configurado neste build." }
        autenticar(FirebaseAuth.getInstance(app))
        return FirebaseFirestore.getInstance(app)
    }

    private suspend fun autenticar(auth: FirebaseAuth): FirebaseUser {
        auth.currentUser?.let { return it }
        return checkNotNull(auth.signInAnonymously().aguardarCatalogo().user) {
            "O Firebase não devolveu o usuário anônimo."
        }
    }

    private fun DocumentSnapshot.paraManifesto(): ManifestoDoCatalogoNaNuvem =
        ManifestoDoCatalogoNaNuvem(
            id = texto("id").also { require(it == id) { "Id divergente no manifesto." } },
            nome = texto("nome"),
            categoria = texto("categoria"),
            colecaoId = texto("colecaoId"),
            colecaoNome = texto("colecaoNome"),
            colecaoIcone = texto("colecaoIcone"),
            versao = inteiro("versao"),
            versaoId = texto("versaoId"),
            estado = texto("estado"),
            quantidadeDeCards = inteiro("quantidadeDeCards"),
            quantidadeDeBlocos = inteiro("quantidadeDeBlocos"),
            descricao = getString("descricao") ?: "",
            tamanhoEmBytes = longo("tamanhoEmBytes"),
            hashDoConteudo = texto("hashDoConteudo"),
            visibilidade = texto("visibilidade"),
        )

    private fun DocumentSnapshot.paraVersao(): VersaoDoBaralhoNaNuvem =
        VersaoDoBaralhoNaNuvem(
            id = texto("id"),
            nome = texto("nome"),
            categoria = texto("categoria"),
            colecaoId = texto("colecaoId"),
            colecaoNome = texto("colecaoNome"),
            colecaoIcone = texto("colecaoIcone"),
            versao = inteiro("versao"),
            estado = texto("estado"),
            quantidadeDeCards = inteiro("quantidadeDeCards"),
            quantidadeDeBlocos = inteiro("quantidadeDeBlocos"),
            hashDoConteudo = texto("hashDoConteudo"),
        )

    private fun DocumentSnapshot.paraBloco(): BlocoDeCardsNaNuvem = BlocoDeCardsNaNuvem(
        ordem = inteiro("ordem"),
        quantidadeDeCards = inteiro("quantidadeDeCards"),
        conteudoJson = texto("conteudoJson"),
        hashDoConteudo = texto("hashDoConteudo"),
    )

    private fun DocumentSnapshot.texto(campo: String): String =
        requireNotNull(getString(campo)) { "Campo '$campo' ausente no documento '$id'." }

    private fun DocumentSnapshot.longo(campo: String): Long =
        requireNotNull(getLong(campo)) { "Campo '$campo' ausente no documento '$id'." }

    private fun DocumentSnapshot.inteiro(campo: String): Int {
        val valor = longo(campo)
        require(valor in Int.MIN_VALUE..Int.MAX_VALUE) { "Campo '$campo' fora do intervalo inteiro." }
        return valor.toInt()
    }

    private companion object {
        const val COLECAO_CATALOGO = "catalogo"
        const val COLECAO_VERSOES = "versoes"
        const val COLECAO_BLOCOS = "blocos"
        const val VISIBILIDADE_PUBLICA = "PUBLICO"
        const val VISIBILIDADE_PRIVADA = "PRIVADO"
    }
}

private suspend fun <T> Task<T>.aguardarCatalogo(): T =
    suspendCancellableCoroutine { continuacao ->
        addOnCompleteListener { tarefa ->
            when {
                tarefa.isSuccessful -> continuacao.resume(tarefa.result)
                tarefa.exception != null -> continuacao.resumeWithException(tarefa.exception!!)
                else -> continuacao.resumeWithException(
                    IllegalStateException("Operação Firebase terminou sem resultado."),
                )
            }
        }
    }
