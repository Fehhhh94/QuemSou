package com.quemsou.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quemsou.app.data.feedback.*
import com.quemsou.app.data.local.AppDatabase
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class FeedbackDaDicaRoomTest {
    @Test fun votoRepetidoSobreviveReaberturaSemDuplicarENaoDependeDoCard() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val nome = "feedback-teste-${UUID.randomUUID()}"
        fun abrir() = Room.databaseBuilder(contexto, AppDatabase::class.java, nome).build()
        var db = abrir()
        try {
            var agendamentos = 0
            val agendador = object : AgendadorDaSincronizacaoDeFeedback {
                override fun agendar() { agendamentos++ }
            }
            var registro = RegistroDeFeedbackLocal(db.feedbackDeCardDao(), agendador)
            val voto = NovoFeedback("pacote", "card", VotoDeCard.BOM, null, 1,
                ResultadoDoTurnoRegistrado.DICA_REVELADA, null, "snapshot-dica-sessao-1")
            coroutineScope { repeat(8) { launch(Dispatchers.IO) { registro.registrar(voto) } } }
            assertEquals(1, registro.quantidade().first())
            assertTrue(agendamentos > 0)
            db.close()
            db = abrir()
            registro = RegistroDeFeedbackLocal(db.feedbackDeCardDao(), agendador)
            assertEquals("BOM", registro.buscarDica(voto.contextoJson)!!.voto)
            val revisaoAntesDaEdicao = registro.buscarDica(voto.contextoJson)!!.revisaoLocal
            registro.registrar(voto.copy(voto = VotoDeCard.FRACO, comentario = "Confusa"))
            assertEquals(1, registro.quantidade().first())
            val editado = registro.buscarDica(voto.contextoJson)!!
            assertEquals("Confusa", editado.comentario)
            assertTrue(editado.revisaoLocal > revisaoAntesDaEdicao)
            assertEquals(
                0,
                db.feedbackDeCardDao().marcarSincronizado(
                    editado.id,
                    revisaoAntesDaEdicao,
                    System.currentTimeMillis(),
                ),
            )
            assertNull(registro.buscarDica(voto.contextoJson)!!.sincronizadoEm)
            assertEquals(1, db.feedbackDeCardDao().buscarDicasPendentes(10).size)
            registro.registrar(voto.copy(contextoJson = "snapshot-dica-sessao-2"))
            registro.registrar(voto.copy(resultadoDoTurno = ResultadoDoTurnoRegistrado.QUEIMADO))
            assertEquals(3, registro.quantidade().first())
            assertTrue(ExportadorDeFeedback.montarJson(registro.buscarTodosComResposta(), "2026-09-16T00:00:00Z").contains("DICA_REVELADA"))
            registro.apagarTudo()
            assertEquals(0, registro.quantidade().first())
        } finally {
            db.close()
            contexto.deleteDatabase(nome)
        }
    }
}
