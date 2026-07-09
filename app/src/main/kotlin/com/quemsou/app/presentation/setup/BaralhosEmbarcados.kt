package com.quemsou.app.presentation.setup

import com.quemsou.app.domain.model.CardCategory

/**
 * Ponte **transitória** da 5A parte 1: o Setup ainda expõe a escolha por
 * categoria (chips), mas a partida já é montada por **baralhos** — este
 * objeto traduz a categoria escolhida nos ids dos dois baralhos embarcados,
 * reproduzindo o comportamento antigo (LIVRE = todos). A parte 2 da 5A
 * substitui isto pela seleção real de baralhos vindos do catálogo.
 *
 * Os ids precisam bater com `assets/cards.json`; o `BaralhoDeAssetsTest`
 * garante a sincronia.
 */
object BaralhosEmbarcados {

    const val CINEMA_CLASSICO_1 = "cinema-classico-1"

    const val MUNDO_DA_MUSICA_1 = "mundo-da-musica-1"

    /** Ids de baralho equivalentes à [categoria] no comportamento antigo. */
    fun idsPara(categoria: CardCategory): List<String> = when (categoria) {
        CardCategory.PERSONAGEM_FILME -> listOf(CINEMA_CLASSICO_1)
        CardCategory.MUNDO_DA_MUSICA -> listOf(MUNDO_DA_MUSICA_1)
        CardCategory.LIVRE -> listOf(CINEMA_CLASSICO_1, MUNDO_DA_MUSICA_1)
    }
}
