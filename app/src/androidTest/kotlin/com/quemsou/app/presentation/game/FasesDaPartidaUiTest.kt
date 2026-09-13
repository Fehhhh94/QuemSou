package com.quemsou.app.presentation.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.presentation.ui.theme.QuemSouTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fases da partida que não dão para alcançar à mão num emulador: o estado
 * indisponível (depende de acabar o conteúdo elegível no meio da partida), a
 * semântica das posições do grid e o sigilo da resposta do leitor.
 *
 * São testes de **apresentação**: nenhuma regra do jogo é avaliada aqui — o
 * domínio já tem os seus testes JVM.
 */
@RunWith(AndroidJUnit4::class)
class FasesDaPartidaUiTest {

    @get:Rule
    val compose = createComposeRule()

    private fun grid(reveladas: List<Int> = emptyList()) = PartidaUiState.Grid(
        rodada = 1,
        nomeDoLeitor = "Ana",
        posicoesReveladas = reveladas,
        nomeDoEscolhedor = "Bruno",
        respostaParaOLeitor = "Harry Potter",
        pontosEmJogo = 10,
        tipo = CardType.PESSOA,
    )

    @Test
    fun estadoIndisponivelOfereceAsTresSaidasEExplicaAFalta() {
        var voltouAoSetup = 0
        compose.setContent {
            QuemSouTheme {
                IndisponivelContent(
                    onTentarNovamente = {},
                    onVoltarAoSetup = { voltouAoSetup++ },
                    onVoltarAoInicio = {},
                )
            }
        }

        compose.onNodeWithText("Não deu para preparar esta partida").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Voltar ao início").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("Mudar rodadas ou baralhos")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        assertEquals(1, voltouAoSetup)
    }

    @Test
    fun posicaoDoGridAnunciaSeEstaDisponivelOuJaRevelada() {
        compose.setContent { QuemSouTheme { GridContent(estado = grid(reveladas = listOf(3)), onRevelarDica = {}) } }

        compose.onNodeWithContentDescription("Posição 7, disponível")
            .performScrollTo()
            .assertHasClickAction()
        compose.onNodeWithContentDescription("Posição 3, dica já revelada")
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun tocarNumaPosicaoJaReveladaNaoRevelaNada() {
        val tocadas = mutableListOf<Int>()
        compose.setContent {
            QuemSouTheme { GridContent(estado = grid(reveladas = listOf(3)), onRevelarDica = { tocadas += it }) }
        }

        compose.onNodeWithContentDescription("Posição 3, dica já revelada").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Posição 7, disponível").performScrollTo().performClick()

        assertEquals(listOf(7), tocadas)
    }

    /**
     * A resposta do leitor não existe na árvore enquanto ninguém segura a
     * área — é o contrato do sigilo: um print, um leitor de tela ou um olhar
     * de canto não podem entregar o card.
     */
    @Test
    fun aRespostaSoExisteEnquantoAAreaEstaPressionada() {
        compose.setContent { QuemSouTheme { GridContent(estado = grid(), onRevelarDica = {}) } }

        compose.onNodeWithText("Harry Potter").assertDoesNotExist()

        // O alvo é procurado pelo rótulo fixo da área, não pelo texto que ela
        // mostra: o texto muda ao pressionar e a busca perderia o nó no meio
        // do gesto.
        val area = compose.onNodeWithContentDescription("Área da resposta do leitor. Segure para ver.")
        area.performTouchInput { down(center) }
        compose.onNodeWithText("Harry Potter").assertIsDisplayed()

        area.performTouchInput { up() }
        compose.onNodeWithText("Harry Potter").assertDoesNotExist()
    }

    // region Área de leitura da dica (R1)

    /** A dica exata da reprodução da revisão: curta, mas de duas a três linhas. */
    private val dicaCurta = "Tenho uma nave como marca registrada dos meus shows."

    private val dicaLonga =
        "Comecei tocando em bares da minha cidade natal antes de assinar com uma " +
            "gravadora internacional, e só então o mundo inteiro passou a cantar " +
            "junto os refrões que eu escrevia no fundo de um ônibus em turnê."

    /**
     * Desenha a fase da dica dentro de uma janela de tamanho fixo e com a
     * escala de fonte do sistema simulada. É o que permite cobrir "paisagem
     * com fonte a 150%" sem depender de girar o aparelho no meio do teste.
     */
    private fun dicaEmJanela(
        texto: String,
        largura: Dp,
        altura: Dp,
        escalaDaFonte: Float,
        onAlguemAcertou: () -> Unit = {},
    ) {
        compose.setContent {
            val densidade = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(densidade.density, escalaDaFonte),
            ) {
                QuemSouTheme {
                    Box(Modifier.size(largura, altura)) {
                        DicaReveladaContent(
                            estado = PartidaUiState.DicaRevelada(
                                posicao = 1,
                                texto = texto,
                                valor = 10,
                                tipo = CardType.PESSOA,
                            ),
                            onAlguemAcertou = onAlguemAcertou,
                            onOutraDica = {},
                            onPedirQueimar = {},
                        )
                    }
                }
            }
        }
    }

    /** Todo o texto do nó cabe na sua caixa visível — nada foi cortado. */
    private fun assertDicaInteiraVisivel(texto: String) {
        val no = compose.onNodeWithText(texto)
        val visivel = no.getBoundsInRoot().height
        val completa = no.getUnclippedBoundsInRoot().height
        if (completa.value > visivel.value + TOLERANCIA_DE_CORTE) {
            throw AssertionError(
                "Dica cortada: a caixa visível tem $visivel e o texto inteiro precisa de $completa.",
            )
        }
    }

    private fun assertAcoesAlcancaveis() {
        compose.onNodeWithText("Alguém acertou").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Outra dica").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Ninguém vai acertar · queimar card")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    /**
     * Regressão direta do defeito relatado na revisão: em janela de paisagem
     * com fonte ampliada, as três ações empilhadas deixavam para a dica uma
     * faixa de uma linha e o texto era cortado.
     */
    @Test
    fun dicaCurtaCabeInteiraEmJanelaDePaisagemComFonteA150() {
        dicaEmJanela(dicaCurta, largura = 400.dp, altura = 380.dp, escalaDaFonte = 1.5f)

        assertDicaInteiraVisivel(dicaCurta)
        assertAcoesAlcancaveis()
    }

    @Test
    fun dicaCurtaCabeInteiraEmJanelaDePaisagemComFonteA130() {
        dicaEmJanela(dicaCurta, largura = 400.dp, altura = 320.dp, escalaDaFonte = 1.3f)

        assertDicaInteiraVisivel(dicaCurta)
        assertAcoesAlcancaveis()
    }

    @Test
    fun dicaCurtaCabeInteiraEmRetratoComFonteA150() {
        dicaEmJanela(dicaCurta, largura = 360.dp, altura = 700.dp, escalaDaFonte = 1.5f)

        assertDicaInteiraVisivel(dicaCurta)
        assertAcoesAlcancaveis()
    }

    /**
     * A dica longa pode precisar de rolagem, mas a janela de leitura tem de
     * continuar sendo uma janela: [ALTURA_MINIMA_DE_LEITURA] é mais de duas
     * linhas do corpo usado em janela baixa, contra a faixa de uma linha do
     * defeito relatado.
     */
    @Test
    fun dicaLongaEmJanelaBaixaMantemJanelaDeLeituraUtilEAcoesAlcancaveis() {
        dicaEmJanela(dicaLonga, largura = 400.dp, altura = 380.dp, escalaDaFonte = 1.5f)

        compose.onNodeWithText(dicaLonga).assertIsDisplayed()
        val visivel = compose.onNodeWithText(dicaLonga).getBoundsInRoot().height
        if (visivel.value < ALTURA_MINIMA_DE_LEITURA.value) {
            throw AssertionError("Janela de leitura de apenas $visivel; mínimo $ALTURA_MINIMA_DE_LEITURA.")
        }
        assertAcoesAlcancaveis()
    }

    /** Em janela baixa as ações mudam de forma, mas continuam funcionando. */
    @Test
    fun acaoDeAcertoContinuaClicavelNaJanelaBaixa() {
        var acertos = 0
        dicaEmJanela(
            dicaCurta,
            largura = 400.dp,
            altura = 380.dp,
            escalaDaFonte = 1.5f,
            onAlguemAcertou = { acertos++ },
        )

        compose.onNodeWithText("Alguém acertou").performClick()

        assertEquals(1, acertos)
    }

    // endregion

    @Test
    fun placarFinalMantemJogarDeNovoEVoltarAlcancaveisComQuatroGrupos() {
        compose.setContent {
            QuemSouTheme {
                PlacarFinalContent(
                    estado = PartidaUiState.PlacarFinal(
                        ranking = listOf(
                            LinhaDoPlacar("Ana & Bruno", 24),
                            LinhaDoPlacar("Caio", 18),
                            LinhaDoPlacar("Dani", 8),
                        ),
                        vencedores = listOf("Ana & Bruno"),
                        empate = false,
                    ),
                    onJogarDeNovo = {},
                    onVoltarAoInicio = {},
                )
            }
        }

        compose.onNodeWithText("Jogar de novo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Voltar ao início").performScrollTo().assertIsDisplayed()
    }

    private companion object {
        /** Arredondamento de layout; acima disso é corte de verdade. */
        const val TOLERANCIA_DE_CORTE = 0.5f

        /** Piso da janela de leitura em janela baixa: mais de duas linhas. */
        val ALTURA_MINIMA_DE_LEITURA = 96.dp
    }
}
