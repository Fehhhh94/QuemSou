package com.quemsou.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.quemsou.app.data.local.*
import com.quemsou.app.domain.model.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MigracaoEHistoricoTest {
    @Test fun upgradePreservaConteudoEFeedbackERestauraCartaSemConsumirNovamente() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val nome = "teste-migracao-dicas.db"
        contexto.deleteDatabase(nome)
        val caminho = contexto.getDatabasePath(nome)
        caminho.parentFile!!.mkdirs()
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.quemsou.app.data.local.AppDatabase/4.json").bufferedReader().use { it.readText() })
            .getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(caminho, null).use { banco ->
            val entidades = schema.getJSONArray("entities")
            for (i in 0 until entidades.length()) {
                val entidade = entidades.getJSONObject(i)
                banco.execSQL(entidade.getString("createSql").replace("\${TABLE_NAME}", entidade.getString("tableName")))
                val indices = entidade.getJSONArray("indices")
                for (j in 0 until indices.length()) banco.execSQL(indices.getJSONObject(j).getString("createSql")
                    .replace("\${TABLE_NAME}", entidade.getString("tableName")))
            }
            banco.execSQL("INSERT INTO baralhos VALUES ('b','Cinema','PERSONAGEM_FILME',1,'EM_DESENVOLVIMENTO','c','Cinema','C')")
            banco.execSQL("INSERT INTO cards VALUES ('hp','PESSOA','PERSONAGEM_FILME','Harry Potter','[\"0\",\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\"]','b')")
            banco.execSQL("INSERT INTO feedback_de_cards VALUES (1,'b','hp','BOM','Preservar',1,'ACERTO',2,123)")
            banco.version = 4
        }
        val db = Room.databaseBuilder(contexto, AppDatabase::class.java, nome)
            .addMigrations(MIGRACAO_4_5, MIGRACAO_5_6, MIGRACAO_6_7)
            .build()
        try {
            assertEquals("Cinema", db.baralhoDao().buscarTodos().single().nome)
            assertEquals("Preservar", db.feedbackDeCardDao().buscarTodosComResposta().single().feedback.comentario)
            val feedbackMigrado = db.feedbackDeCardDao().buscarTodosComResposta().single().feedback
            assertEquals(1, feedbackMigrado.revisaoLocal)
            assertNull(feedbackMigrado.sincronizadoEm)
            val legado = db.cardDao().buscarPorBaralhos(listOf("b")).single().paraDominio()
            assertTrue(legado.bancoDeDicas.isEmpty())
            val expandido = legado.copy(respostaId = "harry-potter",
                bancoDeDicas = List(60) { DicaDoBanco("d$it", "Fato $it") })
            db.cardDao().inserirTodos(listOf(expandido.paraEntidade("b")))
            val repositorio = RepositorioDeCardsLocal(db.baralhoDao(), db.cardDao(), db)
            repositorio.prepararSessao("s1", listOf("b"))
            val primeira = repositorio.prepararTurno("s1", 1, expandido, 5)
            assertTrue(db.historicoDeDicasDao().usadas().isEmpty())
            repositorio.salvarProgresso("s1", ProgressoDaPartida(1, "ANUNCIO", listOf(0, 0), listOf(1, 2, 3)), primeira.clues.take(3), true)
            val antes = db.historicoDeDicasDao().usadas()
            val restaurada = repositorio.prepararTurno("s1", 1, expandido, 99)
            assertEquals(primeira, restaurada)
            assertEquals(antes, db.historicoDeDicasDao().usadas())
            repositorio.prepararSessao("s2", listOf("b"))
            val seguinte = repositorio.prepararTurno("s2", 1, expandido, 5)
            assertTrue(primeira.clues.take(3).intersect(seguinte.clues.toSet()).isEmpty())
            db.cardDao().removerPorBaralho("b")
            assertTrue(db.historicoDeDicasDao().usadas().isNotEmpty())
        } finally {
            db.close()
            contexto.deleteDatabase(nome)
        }
    }
}
