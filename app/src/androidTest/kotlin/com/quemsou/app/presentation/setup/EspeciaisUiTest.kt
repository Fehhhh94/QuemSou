package com.quemsou.app.presentation.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quemsou.app.domain.model.EstadoDoBaralho
import com.quemsou.app.presentation.ui.theme.QuemSouTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EspeciaisUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun especialPodeSerSelecionadoSemMarcarTemaComumEmJanelaCompacta() {
        val comum = BaralhoParaSelecao("musica", "Mundo da Música", EstadoDoBaralho.FINALIZADO,
            "musica", "Música", "🎵", 30)
        val especial = BaralhoParaSelecao("kimberly-clark-financas", "Kimberly-Clark — Finanças",
            EstadoDoBaralho.EM_DESENVOLVIMENTO, "especiais", "Especiais", "⭐", 30)
        var estado by mutableStateOf(SetupUiState(
            baralhosDisponiveis = listOf(comum, especial), baralhosCarregados = true,
        ))
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                QuemSouTheme {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        SecaoBaralhos(
                            uiState = estado,
                            onAlternarBaralho = { id ->
                                estado = estado.copy(baralhosSelecionados =
                                    if (id in estado.baralhosSelecionados) estado.baralhosSelecionados - id
                                    else estado.baralhosSelecionados + id)
                            },
                            onSelecionarTodos = {}, onAbrirCatalogo = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Temas da partida").assertIsDisplayed()
        compose.onNodeWithText("⭐ Especiais").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(especial.nome).performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText(comum.nome).performScrollTo().assertIsOff()
        compose.onNodeWithText(especial.nome).performScrollTo().performClick().assertIsOff()
    }
}
