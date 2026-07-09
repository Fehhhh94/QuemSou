package com.quemsou.app.domain.model

/**
 * Um grupo de jogadores da partida — o único conceito de disputa desde a
 * especificação v4 (não existe mais bifurcação entre "modo individual" e
 * "modo times").
 *
 * Todo jogador pertence a exatamente um grupo. Por padrão, cada jogador nasce
 * em um grupo próprio de tamanho 1 ([individuais]) — o antigo "individual" é
 * apenas esse estado padrão, não um caso especial. Um "time" é simplesmente
 * um grupo com 2 ou mais jogadores, e grupos de tamanhos diferentes convivem
 * na mesma partida sem validação especial. Não há limite de quantidade de
 * grupos: o teto natural é o número de jogadores da partida.
 *
 * Imutável — [adicionarPontos] retorna um novo grupo.
 *
 * @property id identificador único do grupo na partida.
 * @property nome nome de exibição: o nome do jogador quando o grupo tem 1
 *   membro, ou os nomes concatenados (ex.: "Ana & Bruno") quando tem 2+.
 * @property jogadores ids dos jogadores do grupo, na ordem de assento.
 * @property pontos pontos acumulados pelo grupo ao fim de cada turno.
 */
data class Grupo(
    val id: String,
    val nome: String,
    val jogadores: List<String>,
    val pontos: Int = 0,
) {
    init {
        require(id.isNotBlank()) { "Grupo com id vazio." }
        require(nome.isNotBlank()) { "Grupo '$id' com nome vazio." }
        require(jogadores.isNotEmpty()) { "Grupo '$id' sem jogadores." }
        require(jogadores.toSet().size == jogadores.size) { "Grupo '$id' com jogadores repetidos." }
        require(pontos >= 0) { "Grupo '$id' com pontos negativos: $pontos." }
    }

    /** Soma [pontos] ao grupo, retornando uma cópia. */
    fun adicionarPontos(pontos: Int): Grupo {
        require(pontos >= 0) { "Pontos não podem ser negativos: $pontos." }
        return copy(pontos = this.pontos + pontos)
    }

    companion object {
        /** Cria um grupo com os [membros], com o nome de exibição derivado dos nomes deles. */
        fun criar(id: String, membros: List<Jogador>): Grupo = Grupo(
            id = id,
            nome = membros.joinToString(" & ") { it.nome },
            jogadores = membros.map { it.id },
        )

        /**
         * O agrupamento padrão do modelo v4: cada jogador em um grupo próprio
         * de tamanho 1, com o id e o nome do próprio jogador.
         */
        fun individuais(jogadores: List<Jogador>): List<Grupo> =
            jogadores.map { criar(id = it.id, membros = listOf(it)) }
    }
}
