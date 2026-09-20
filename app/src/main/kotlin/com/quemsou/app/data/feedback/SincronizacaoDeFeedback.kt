package com.quemsou.app.data.feedback

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.quemsou.app.data.local.FeedbackDeCardDao
import com.quemsou.app.data.local.FeedbackDeCardEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json

/** Agenda o envio sem transformar a gravação local em uma operação de rede. */
interface AgendadorDaSincronizacaoDeFeedback {
    fun agendar()
}

/** Usado por testes que constroem o registro local sem o grafo Android. */
object AgendadorDaSincronizacaoNulo : AgendadorDaSincronizacaoDeFeedback {
    override fun agendar() = Unit
}

@Singleton
class AgendadorDaSincronizacaoWorkManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : AgendadorDaSincronizacaoDeFeedback {

    override fun agendar() {
        val pedido = OneTimeWorkRequestBuilder<SincronizarFeedbackWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            TRABALHO_UNICO,
            ExistingWorkPolicy.KEEP,
            pedido,
        )
    }

    private companion object {
        const val TRABALHO_UNICO = "sincronizar-feedback-de-dicas"
    }
}

interface DestinoDeFeedbackNaNuvem {
    val configurado: Boolean
    suspend fun enviar(feedback: FeedbackDeCardEntity)
}

/**
 * Destino mínimo do Firestore. Envia somente o snapshot da dica avaliada;
 * resposta correta, sessão e as outras nove dicas nunca saem do aparelho.
 */
@Singleton
class DestinoDeFeedbackNoFirestore @Inject constructor(
    @ApplicationContext private val context: Context,
) : DestinoDeFeedbackNaNuvem {

    private val firebaseApp: FirebaseApp?
        get() = FirebaseApp.getApps(context).firstOrNull()

    override val configurado: Boolean
        get() = firebaseApp != null

    override suspend fun enviar(feedback: FeedbackDeCardEntity) {
        require(feedback.id > 0) { "Feedback sem id local não pode ser sincronizado." }
        require(feedback.resultadoDoTurno == ResultadoDoTurnoRegistrado.DICA_REVELADA.name) {
            "Somente avaliações de dica são sincronizadas."
        }
        val app = checkNotNull(firebaseApp) { "Firebase não configurado neste build." }
        val usuario = autenticar(FirebaseAuth.getInstance(app))
        val contextoDaDica = JSON.decodeFromString<ContextoDeFeedbackDaDica>(
            feedback.contextoJson,
        )
        val dados = mapOf(
            "schemaVersion" to 1,
            "autorUid" to usuario.uid,
            "baralhoId" to feedback.baralhoId,
            "cardId" to feedback.cardId,
            "dicaId" to contextoDaDica.dicaId,
            "textoAvaliado" to contextoDaDica.texto,
            "voto" to feedback.voto,
            "comentario" to (feedback.comentario ?: ""),
            "rodada" to feedback.rodada,
            "posicao" to contextoDaDica.posicao,
            "versaoDoBaralho" to contextoDaDica.versaoDoBaralho,
            "criadoEm" to Timestamp(Date(feedback.criadoEm)),
            "atualizadoEm" to FieldValue.serverTimestamp(),
        )
        FirebaseFirestore.getInstance(app)
            .collection(COLECAO)
            .document("${usuario.uid}_${feedback.id}")
            .set(dados)
            .aguardar()
    }

    private suspend fun autenticar(auth: FirebaseAuth): FirebaseUser {
        auth.currentUser?.let { return it }
        return checkNotNull(auth.signInAnonymously().aguardar().user) {
            "O Firebase não devolveu o usuário anônimo."
        }
    }

    private companion object {
        const val COLECAO = "feedbacks"
        val JSON = Json { ignoreUnknownKeys = true }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DependenciasDaSincronizacaoDeFeedback {
    fun feedbackDeCardDao(): FeedbackDeCardDao
    fun destinoDeFeedbackNaNuvem(): DestinoDeFeedbackNaNuvem
}

class SincronizarFeedbackWorker(
    context: Context,
    parametros: WorkerParameters,
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val dependencias = EntryPointAccessors.fromApplication(
            applicationContext,
            DependenciasDaSincronizacaoDeFeedback::class.java,
        )
        val destino = dependencias.destinoDeFeedbackNaNuvem()
        if (!destino.configurado) return Result.success()
        val dao = dependencias.feedbackDeCardDao()
        return try {
            while (true) {
                val lote = dao.buscarDicasPendentes(LIMITE_DO_LOTE)
                if (lote.isEmpty()) break
                lote.forEach { feedback ->
                    destino.enviar(feedback)
                    dao.marcarSincronizado(
                        feedback.id,
                        feedback.revisaoLocal,
                        System.currentTimeMillis(),
                    )
                }
            }
            Result.success()
        } catch (falha: Exception) {
            Log.w(TAG, "Feedback de dica permaneceu na fila local.", falha)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "FeedbackFirestore"
        const val LIMITE_DO_LOTE = 100
    }
}

private suspend fun <T> Task<T>.aguardar(): T = suspendCancellableCoroutine { continuacao ->
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
