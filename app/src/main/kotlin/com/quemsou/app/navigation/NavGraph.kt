package com.quemsou.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quemsou.app.presentation.ui.game.GameScreen
import com.quemsou.app.presentation.ui.home.HomeScreen
import com.quemsou.app.presentation.ui.score.ScoreScreen
import com.quemsou.app.presentation.ui.setup.SetupScreen

/**
 * Grafo de navegação principal do app, com rotas tipadas via [kotlinx.serialization].
 * Nesta fase as telas são apenas placeholders — nenhuma regra de jogo foi implementada.
 */
@Composable
fun QuemSouNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onCreateMatch = { navController.navigate(SetupRoute) },
                onJoinWithCode = { navController.navigate(GameRoute) },
            )
        }
        composable<SetupRoute> {
            SetupScreen()
        }
        composable<GameRoute> {
            GameScreen()
        }
        composable<ScoreRoute> {
            ScoreScreen()
        }
    }
}
