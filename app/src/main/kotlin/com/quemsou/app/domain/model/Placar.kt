package com.quemsou.app.domain.model

/**
 * Placar da partida: pontos acumulados por jogador ao fim de cada turno.
 *
 * Imutável — [adicionar] retorna um novo placar. Empate na v1 é **declarado**:
 * [vencedores] retorna todos os empatados na maior pontuação, sem desempate.
 *
 * @property pontosPorJogador pontos acumulados, indexados pelo id do jogador.
 */
data class Placar(
    val pontosPorJogador: Map<String, Int>,
) {
    init {
        require(pontosPorJogador.isNotEmpty()) { "Placar sem jogadores." }
        require(pontosPorJogador.values.all { it >= 0 }) { "Placar com pontos negativos." }
    }

    /** Soma [pontos] ao jogador [jogadorId], que precisa existir no placar. */
    fun adicionar(jogadorId: String, pontos: Int): Placar {
        require(jogadorId in pontosPorJogador) { "Jogador '$jogadorId' não está no placar." }
        require(pontos >= 0) { "Pontos não podem ser negativos: $pontos." }
        return Placar(pontosPorJogador + (jogadorId to pontosDe(jogadorId) + pontos))
    }

    /** Pontos atuais do jogador [jogadorId], que precisa existir no placar. */
    fun pontosDe(jogadorId: String): Int =
        requireNotNull(pontosPorJogador[jogadorId]) { "Jogador '$jogadorId' não está no placar." }

    /** Ranking individual: pares (jogadorId, pontos) do maior para o menor. */
    fun ranking(): List<Pair<String, Int>> =
        pontosPorJogador.entries.sortedByDescending { it.value }.map { it.toPair() }

    /** Ids dos jogadores com a maior pontuação (mais de um em caso de empate). */
    fun vencedores(): List<String> {
        val maiorPontuacao = pontosPorJogador.values.max()
        return ranking().filter { it.second == maiorPontuacao }.map { it.first }
    }

    /**
     * Pontos por time no modo [ModoDeJogo.TIMES]: a soma dos pontos dos
     * jogadores de cada time. Todos os [jogadores] precisam ter [Jogador.timeId].
     */
    fun pontosPorTime(jogadores: List<Jogador>): Map<String, Int> = jogadores
        .groupBy { jogador ->
            requireNotNull(jogador.timeId) { "Jogador '${jogador.id}' sem timeId." }
        }
        .mapValues { (_, membros) -> membros.sumOf { pontosDe(it.id) } }

    /** Ids dos times com a maior soma de pontos (mais de um em caso de empate). */
    fun vencedoresPorTime(jogadores: List<Jogador>): List<String> {
        val pontos = pontosPorTime(jogadores)
        val maiorPontuacao = pontos.values.max()
        return pontos.filterValues { it == maiorPontuacao }.keys.toList()
    }

    companion object {
        /** Placar inicial: todos os [jogadores] com 0 pontos. */
        fun inicial(jogadores: List<Jogador>): Placar =
            Placar(jogadores.associate { it.id to 0 })
    }
}
