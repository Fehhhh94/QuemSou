package com.quemsou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.quemsou.app.navigation.QuemSouNavGraph
import com.quemsou.app.presentation.ui.theme.QuemSouTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity única do app, hospedando todo o grafo de navegação do Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuemSouTheme {
                QuemSouNavGraph()
            }
        }
    }
}
