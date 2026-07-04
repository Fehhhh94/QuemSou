package com.quemsou.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Classe [Application] do QuemSou, ponto de entrada do grafo de injeção de dependências do Hilt.
 */
@HiltAndroidApp
class QuemSouApp : Application()
