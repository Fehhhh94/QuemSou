package com.quemsou.app.domain.repository

import com.quemsou.app.domain.model.Baralho

/**
 * Fonte dos baralhos disponíveis no aparelho para montar o monte de uma
 * partida. A implementação real lê do banco Room; testes usam fakes em
 * memória.
 */
interface RepositorioDeCards {

    /**
     * Baralhos dos [ids] informados, cada um com os seus cards. Ids
     * desconhecidos são ignorados; a ordem do retorno não é garantida — quem
     * monta o monte é [Baralho.uniaoDeterministica], que ordena por chave
     * estável.
     */
    suspend fun buscarPorIds(ids: List<String>): List<Baralho>
}
