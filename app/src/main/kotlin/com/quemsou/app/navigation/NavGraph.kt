package com.quemsou.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quemsou.app.presentation.catalogo.CatalogoScreen
import com.quemsou.app.presentation.catalogo.ColecaoScreen
import com.quemsou.app.presentation.game.PartidaScreen
import com.quemsou.app.presentation.setup.SetupScreen
import com.quemsou.app.presentation.ui.home.HomeScreen

/**
 * Grafo de navegação: 5 rotas tipadas (Home, Setup, Partida, Catalogo,
 * Colecao). As fases do jogo são estados da rota Partida — o
 * `PartidaViewModel` (escopado à rota) dirige toda a partida com um único
 * `StateFlow`.
 */
@Composable
fun QuemSouNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onCreateMatch = { navController.navigate(SetupRoute) },
                onAbrirCatalogo = { navController.navigate(CatalogoRoute) },
                // Entrar com código é multiplayer (Fase 4); por ora leva ao Setup.
                onJoinWithCode = { navController.navigate(SetupRoute) },
            )
        }
        composable<SetupRoute> {
            SetupScreen(
                onComecarPartida = { configuracao ->
                    navController.navigate(PartidaRoute(configuracao.paraJson()))
                },
                onAbrirCatalogo = { navController.navigate(CatalogoRoute) },
            )
        }
        composable<PartidaRoute> {
            PartidaScreen(
                onAbandonarPartida = { navController.popBackStack(HomeRoute, inclusive = false) },
                onVoltarAoInicio = { navController.popBackStack(HomeRoute, inclusive = false) },
            )
        }
        composable<CatalogoRoute> {
            CatalogoScreen(
                onAbrirColecao = { colecaoId -> navController.navigate(ColecaoRoute(colecaoId)) },
                onVoltar = { navController.popBackStack() },
            )
        }
        composable<ColecaoRoute> {
            ColecaoScreen(onVoltar = { navController.popBackStack() })
        }
    }
}
