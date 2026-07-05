package com.quemsou.app.domain.repository

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory

/**
 * Fonte dos cards disponíveis para montar o baralho de uma partida.
 * A implementação real lê do banco Room; testes usam fakes em memória.
 */
interface RepositorioDeCards {

    /**
     * Cards da [categoria] informada. [CardCategory.LIVRE] é um filtro-união:
     * retorna os cards de todas as categorias (decisão registrada em
     * docs/GAME_RULES.md — não existem cards exclusivos de LIVRE).
     */
    suspend fun buscarPorCategoria(categoria: CardCategory): List<Card>
}
