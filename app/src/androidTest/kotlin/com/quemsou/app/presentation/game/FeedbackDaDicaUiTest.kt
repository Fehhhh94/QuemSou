package com.quemsou.app.presentation.game

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.quemsou.app.data.feedback.VotoDeCard
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.presentation.ui.theme.QuemSouTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FeedbackDaDicaUiTest {
    @get:Rule val compose = createComposeRule()

    private fun capturar(nome: String) {
        compose.waitForIdle()
        val inst = InstrumentationRegistry.getInstrumentation()
        val pasta = File(inst.targetContext.getExternalFilesDir(null), "evidencias-feedback").apply { mkdirs() }
        val imagem = inst.uiAutomation.takeScreenshot()
        File(pasta, "$nome.png").outputStream().use { imagem.compress(Bitmap.CompressFormat.PNG, 100, it) }
        imagem.recycle()
    }

    @Test fun avaliarDicaComComentarioSemExporRespostaNemBloquearOutraDica() {
        val feedback = mutableStateOf(FeedbackDaDicaUiState("contexto-com-resposta-secreta", carregando = false))
        var continuou = 0
        compose.setContent {
            QuemSouTheme(darkTheme = false) {
                Surface(Modifier.fillMaxSize().systemBarsPadding()) {
                DicaReveladaContent(PartidaUiState.DicaRevelada(1, "Esta é uma pista fictícia para o teste de avaliação.", 10, CardType.COISA),
                    {}, { continuou++ }, {}, feedback = {
                        FeedbackDaDicaWidget(feedback.value) { chave, voto, comentario ->
                            feedback.value = FeedbackDaDicaUiState(chave, voto, comentario, carregando = false, salvo = true)
                        }
                    })
                }
            }
        }
        capturar("dica-light")
        compose.onNodeWithText("Como foi esta dica? Avaliar (opcional)").performScrollTo().performClick()
        compose.onNodeWithText("contexto-com-resposta-secreta").assertDoesNotExist()
        compose.onNodeWithText("Comentário opcional").performTextInput("Clara e específica")
        compose.onNodeWithText("Boa dica").performScrollTo().performClick()
        assertEquals(VotoDeCard.BOM, feedback.value.voto)
        assertEquals("Clara e específica", feedback.value.comentario)
        compose.onNodeWithText("Avaliação salva neste celular.").performScrollTo().assertIsDisplayed()
        capturar("avaliacao-salva-light")
        compose.onNodeWithText("Voltar à dica").performClick()
        compose.onNodeWithText("Outra dica").performClick()
        assertEquals(1, continuou)
    }

    @Test fun conviteEVoltarAlcancaveisEm320dpDarkFonte150() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                QuemSouTheme(darkTheme = true) {
                    Surface(Modifier.fillMaxSize().systemBarsPadding()) {
                    Box(Modifier.fillMaxSize()) {
                    Box(Modifier.size(320.dp, 600.dp)) {
                        DicaReveladaContent(PartidaUiState.DicaRevelada(2, "Esta é outra pista sintética para o teste de apresentação.", 9, CardType.PESSOA),
                            {}, {}, {}, feedback = { FeedbackDaDicaWidget(FeedbackDaDicaUiState("dica", carregando = false)) { _, _, _ -> } })
                    }
                    }
                    }
                }
            }
        }
        compose.onNodeWithText("Como foi esta dica? Avaliar (opcional)").performScrollTo().assertIsDisplayed()
        capturar("dica-dark-320dp-fonte150")
        compose.onNodeWithText("Como foi esta dica? Avaliar (opcional)").performClick()
        compose.onNodeWithText("Precisa melhorar").performScrollTo().assertIsDisplayed()
        capturar("avaliacao-dark-320dp-fonte150")
        compose.onNodeWithText("Voltar à dica").performClick()
        compose.onNodeWithText("Outra dica").assertIsDisplayed()
    }
}
