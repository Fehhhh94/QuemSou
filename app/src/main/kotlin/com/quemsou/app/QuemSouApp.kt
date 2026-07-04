package com.quemsou.app

import android.app.Application
import android.util.Log
import com.quemsou.app.data.importer.CardsImporter
import com.quemsou.app.data.importer.ResultadoImportacao
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Classe [Application] do QuemSou, ponto de entrada do grafo de injeção de dependências do Hilt.
 *
 * Na inicialização dispara o [CardsImporter] fora da main thread. Uma falha de
 * importação (JSON malformado ou card inválido) derruba o app de propósito —
 * falha ruidosa: melhor quebrar no debug do que jogar com card capenga.
 */
@HiltAndroidApp
class QuemSouApp : Application() {

    @Inject
    lateinit var cardsImporter: CardsImporter

    private val escopoDaAplicacao = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        importarCards()
    }

    private fun importarCards() {
        escopoDaAplicacao.launch {
            when (val resultado = cardsImporter.importarSeNecessario()) {
                is ResultadoImportacao.Importado ->
                    Log.i(TAG_IMPORTACAO, "Cards importados: ${resultado.quantidade} (versão ${resultado.versao})")

                is ResultadoImportacao.NadaAFazer ->
                    Log.i(TAG_IMPORTACAO, "Banco já na versão ${resultado.versao}, nada a fazer")
            }
        }
    }

    private companion object {
        const val TAG_IMPORTACAO = "CardsImporter"
    }
}
