package com.quemsou.app.presentation.game

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
     */
    data class Grid(
        val posicoesReveladas: List<Int>,
        val nomeDoEscolhedor: String,
        val respostaParaOLeitor: String,
        val pontosEmJogo: Int,
    ) : PartidaUiState

    /** Dica revelada, lida em voz alta; vale [valor] pontos se alguém acertar agora. */
    data class DicaRevelada(
        val posicao: Int,
        val texto: String,
        val valor: Int,
    ) : PartidaUiState

    /** Escolha de quem acertou (pulada quando há um único adivinhador). */
    data class QuemAcertou(
        val adivinhadores: List<AdivinhadorUi>,
    ) : PartidaUiState

    /** Fim de turno, sempre com a resposta revelada. */
    sealed interface Anuncio : PartidaUiState {
        val resposta: String
        val dicasUsadas: Int

        data class Acerto(
            override val resposta: String,
            override val dicasUsadas: Int,
            val nomeDoAcertador: String,
            val pontosDoAcertador: Int,
            val pontosDoLeitor: Int,
        ) : Anuncio

        data class Queimado(
            override val resposta: String,
            override val dicasUsadas: Int,
        ) : Anuncio
    }

    /**
     * Placar final da partida encerrada. Empate na v1 é declarado:
     * [vencedores] traz todos os empatados (nomes de jogadores no modo
     * individual, ids de times no modo times).
     */
    data class PlacarFinal(
        val ranking: List<LinhaDoPlacar>,
        val vencedores: List<String>,
        val empate: Boolean,
    ) : PartidaUiState
}

/** Um adivinhador listado na fase QuemAcertou. */
data class AdivinhadorUi(val id: String, val nome: String)

/** Uma linha do ranking do placar final. */
data class LinhaDoPlacar(val nome: String, val pontos: Int)
