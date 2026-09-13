package com.quemsou.app.domain.model

/**
 * Um card do jogo: uma resposta secreta acompanhada de exatamente
 * [QUANTIDADE_DE_DICAS] dicas, reveladas uma a uma durante o turno.
 *
 * @property id identificador único do card.
 * @property type tipo da resposta (pessoa, lugar ou coisa).
 * @property category categoria temática do card.
 * @property answer resposta secreta que os jogadores tentam adivinhar.
 * @property clues dez dicas de compatibilidade no catálogo ou dez selecionadas na rodada;
 *   as posições no grid são embaralhadas antes da revelação.
 */
data class Card(
    val id: String,
    val type: CardType,
    val category: CardCategory,
    val answer: String,
    val clues: List<String>,
    /** Identidade editorial compartilhada entre baralhos; vazio nos cards legados. */
    val respostaId: String = "",
    /** Acervo editorial. A carta da rodada continua contendo exatamente dez [clues]. */
    val bancoDeDicas: List<DicaDoBanco> = emptyList(),
    /** Aliases reunidos no acervo local; não fazem parte do contrato editorial de download. */
    val chavesDaResposta: Set<String> = emptySet(),
) {
    init {
        require(clues.size == QUANTIDADE_DE_DICAS) {
            "Card '$id' deve ter exatamente $QUANTIDADE_DE_DICAS dicas, mas tem ${clues.size}."
        }
    }

    companion object {
        /** Número fixo de dicas de todo card. */
        const val QUANTIDADE_DE_DICAS = 10
    }
}

data class DicaDoBanco(val id: String, val texto: String)
