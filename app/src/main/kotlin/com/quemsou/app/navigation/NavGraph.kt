package com.quemsou.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quemsou.app.presentation.game.PartidaScreen
import com.quemsou.app.presentation.setup.SetupScreen
import com.quemsou.app.presentation.ui.home.HomeScreen

/**
 * Grafo de navegação: 3 rotas tipadas (Home, Setup, Partida). As fases do
 * jogo são estados da rota Partida — o `PartidaViewModel` (escopado à rota)
 * dirige toda a partida com um único `StateFlow`.
 */
@Composable
fun QuemSouNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onCreateMatch = { navController.navigate(SetupRoute) },
                // Entrar com código é multiplayer (Fase 4); por ora leva ao Setup.
                onJoinWithCode = { navController.navigate(SetupRoute) },
            )
        }
        composable<SetupRoute> {
            SetupScreen(
                onComecarPartida = { configuracao ->
                    navController.navigate(PartidaRoute(configuracao.paraJson()))
                },
            )
        }
        composable<PartidaRoute> {
            PartidaScreen()
        }
    }
}
