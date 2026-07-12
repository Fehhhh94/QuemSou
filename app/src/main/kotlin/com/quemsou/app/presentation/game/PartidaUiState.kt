package com.quemsou.app.presentation.game

import com.quemsou.app.data.feedback.VotoDeCard
import com.quemsou.app.domain.model.CardType

/**
 * Estado único que dirige toda a UI da rota Partida. As fases do jogo são
 * estados deste sealed — **não** rotas de navegação (decisão da 3.2).
 */
sealed interface PartidaUiState {

    /** A partida ainda está sendo montada (cards vindo do banco). */
    data object Carregando : PartidaUiState

    /** Anúncio de início de rodada: passar o celular ao leitor. */
    data class VezDeJogar(
        val rodada: Int,
        val totalDeRodadas: Int,
        val nomeDoLeitor: String,
        val nomesDosAdivinhadores: List<String>,
    ) : PartidaUiState

    /**
     * Grid 1–10 às cegas: o escolhedor da vez toca uma posição livre.
     *
     * @property respostaParaOLeitor a resposta secreta, visível só na área do leitor.
     * @property pontosEmJogo pontos que a próxima dica revelada valerá se alguém acertar.
     * @property tipo tipo do card (para o chip "Sou um LUGAR" etc.).
     */
    data class Grid(
        val rodada: Int,
        val nomeDoLeitor: String,
        val posicoesReveladas: List<Int>,
        val nomeDoEscolhedor: String,
        val respostaParaOLeitor: String,
        val pontosEmJogo: Int,
        val tipo: CardType,
    ) : PartidaUiState

    /**
     * Modo Shot: o escolhedor da vez tocou uma posição com shot — bebe, toca
     * "Bebi!" e só então a dica é revelada, normalmente. A pontuação não muda
     * em nada; o overlay não é dispensável por toque no scrim.
     *
     * @property posicao posição tocada, pendente de revelação.
     * @property nomeDoBebedor quem escolheu o número — é sempre quem bebe.
     * @property grid o grid da fase anterior, como pano de fundo sob o overlay.
     */
    data class Shot(
        val posicao: Int,
        val nomeDoBebedor: String,
        val grid: Grid,
    ) : PartidaUiState

    /** Dica revelada, lida em voz alta; vale [valor] pontos se alguém acertar agora. */
    data class DicaRevelada(
        val posicao: Int,
        val texto: String,
        val valor: Int,
        val tipo: CardType,
    ) : PartidaUiState

    /**
     * Escolha de quem acertou (pulada quando há um único adivinhador).
     * [adivinhadores] traz o escolhedor da vez primeiro na lista.
     *
     * @property pontosEmJogo pontos que quem acertar agora vai ganhar.
     * @property pontosDoLeitor pontos que o leitor ganha junto (0 quando a regra está desligada).
     */
    data class QuemAcertou(
        val adivinhadores: List<AdivinhadorUi>,
        val nomeDoLeitor: String,
        val pontosEmJogo: Int,
        val pontosDoLeitor: Int,
    ) : PartidaUiState

    /**
     * Fim de turno, sempre com a resposta revelada.
     *
     * @property ultimaRodada `true` quando fechar este anúncio leva ao
     *   [PlacarFinal] — a UI troca o texto do botão de avançar por "Ver placar".
     * @property pontosDoLeitor pontos que o leitor ganha neste turno (0
     *   quando a regra `leitorPontua` está desligada) — especificação v3:
     *   1 ponto por dica revelada sem acerto, 10 quando o card queima.
     */
    sealed interface Anuncio : PartidaUiState {
        val resposta: String
        val dicasUsadas: Int
        val ultimaRodada: Boolean
        val nomeDoLeitor: String
        val pontosDoLeitor: Int

        data class Acerto(
            override val resposta: String,
            override val dicasUsadas: Int,
            override val ultimaRodada: Boolean,
            override val nomeDoLeitor: String,
            override val pontosDoLeitor: Int,
            val nomeDoAcertador: String,
            val pontosDoAcertador: Int,
        ) : Anuncio

        data class Queimado(
            override val resposta: String,
            override val dicasUsadas: Int,
            override val ultimaRodada: Boolean,
            override val nomeDoLeitor: String,
            override val pontosDoLeitor: Int,
        ) : Anuncio
    }

    /**
     * Placar final da partida encerrada, agregado por grupo (especificação
     * v4): cada linha usa o nome de exibição do grupo — o nome do jogador se
     * o grupo tem 1 membro, os nomes concatenados (ex.: "Ana & Bruno") se tem
     * 2+. Empate na v1 é declarado: [vencedores] traz os nomes de todos os
     * grupos empatados.
     */
    data class PlacarFinal(
        val ranking: List<LinhaDoPlacar>,
        val vencedores: List<String>,
        val empate: Boolean,
    ) : PartidaUiState
}

/**
 * Estado do widget dev de feedback no Anúncio (modo dev de feedback, 5B
 * parte 2). Existe (não nulo) **apenas** durante um Anúncio com o modo dev
 * ligado — com o modo desligado, nenhum composable do widget entra na
 * composição. Avaliar é opcional: sem [voto], nada é gravado no "Continuar".
 *
 * @property voto veredito atual; `null` = nenhum chip selecionado.
 * @property comentario texto do comentário opcional (só gravado com voto).
 */
data class FeedbackDevUiState(
    val voto: VotoDeCard? = null,
    val comentario: String = "",
)

/** Um adivinhador listado na fase QuemAcertou. */
data class AdivinhadorUi(val id: String, val nome: String)

/** Uma linha do ranking do placar final. */
data class LinhaDoPlacar(val nome: String, val pontos: Int)
