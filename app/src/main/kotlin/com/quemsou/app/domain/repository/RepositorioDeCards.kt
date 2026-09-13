package com.quemsou.app.domain.repository

import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.ProgressoDaPartida

/**
 * Fonte dos baralhos disponíveis no aparelho para montar o monte de uma
 * partida. A implementação real lê do banco Room; testes usam fakes em
 * memória.
 */
interface RepositorioDeCards {
    suspend fun historicoDaSessao(sessao: String): Map<String, Long> = emptyMap()
    suspend fun progressoDaSessao(sessao: String): ProgressoDaPartida? = null
    /** Consumo e checkpoint atômicos; somente textos efetivamente revelados na carta salva. */
    suspend fun salvarProgresso(sessao: String, progresso: ProgressoDaPartida,
        dicasReveladas: List<String>, encerrarTurno: Boolean) {}
    suspend fun encerrarSessao(sessao: String) {}
    /** Congela conteúdo e rotação; montar uma partida não consome dicas nem registra aparições. */
    suspend fun prepararSessao(sessao: String, ids: List<String>): List<Baralho> = buscarPorIds(ids)

    /** Reserva dez dicas antes de exibi-las. Reabrir a mesma rodada devolve a mesma carta. */
    suspend fun prepararTurno(sessao: String, rodada: Int, card: com.quemsou.app.domain.model.Card,
        seed: Long): com.quemsou.app.domain.model.Card = card

    /**
     * Baralhos dos [ids] informados, cada um com os seus cards. Ids
     * desconhecidos são ignorados; a ordem do retorno não é garantida — quem
     * monta o monte é [Baralho.uniaoDeterministica], que ordena por chave
     * estável.
     */
    suspend fun buscarPorIds(ids: List<String>): List<Baralho>

    /**
     * Todos os baralhos do aparelho (embarcados + baixados do catálogo), com
     * os seus cards — a lista que o Setup oferece para a seleção da partida.
     */
    suspend fun buscarTodos(): List<Baralho>
}
