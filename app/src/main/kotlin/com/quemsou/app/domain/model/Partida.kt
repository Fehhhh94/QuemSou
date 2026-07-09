package com.quemsou.app.domain.model

/**
 * Uma partida do QuemSou: jogadores, grupos, regras, baralho já embaralhado e
 * o andamento (rodada atual, leitor da vez, pontos por grupo).
 *
 * Imutável: [encerrarTurno] retorna uma nova partida com os pontos
 * atualizados, a próxima rodada e o próximo leitor (rodízio circular). Após
 * [totalDeRodadas] rodadas, a partida fica [encerrada] com o placar final.
 *
 * ## Grupos (especificação v4)
 * Todo jogador pertence a exatamente um [Grupo]; o padrão é cada um em seu
 * grupo próprio de tamanho 1 ([Grupo.individuais]). Os pontos de acertador e
 * leitor são creditados ao grupo do jogador que pontuou ([grupoDe]) — mas o
 * rodízio de leitor e de escolhedor continua por **jogador individual**.
 *
 * @property jogadores de [MINIMO_DE_JOGADORES] a [MAXIMO_DE_JOGADORES] jogadores, ids únicos,
 *   na ordem de assento — o rodízio de leitor segue essa ordem.
 * @property regras regras configuradas antes da partida.
 * @property baralho cards já embaralhados pela seed; um card por rodada.
 * @property seed seed da partida (gerada do código); também deriva o
 *   embaralhamento das dicas de cada turno.
 * @property rodadaAtual rodada em jogo, de 1 a [totalDeRodadas].
 * @property indiceDoLeitor índice em [jogadores] do leitor da vez.
 * @property grupos agrupamento dos jogadores, com os pontos acumulados; todo
 *   jogador precisa pertencer a exatamente um grupo. Grupos de tamanho 1 e 2+
 *   convivem na mesma partida, sem limite de quantidade de grupos.
 * @property encerrada `true` após a última rodada; nenhuma ação é permitida.
 */
data class Partida(
    val jogadores: List<Jogador>,
    val regras: RegrasPartida,
    val baralho: List<Card>,
    val seed: Long,
    val rodadaAtual: Int = 1,
    val indiceDoLeitor: Int = 0,
    val grupos: List<Grupo> = Grupo.individuais(jogadores),
    val encerrada: Boolean = false,
) {
    /** Total de rodadas da partida, definido nas regras. */
    val totalDeRodadas: Int
        get() = regras.numeroDeRodadas

    init {
        require(jogadores.size in MINIMO_DE_JOGADORES..MAXIMO_DE_JOGADORES) {
            "Partida exige de $MINIMO_DE_JOGADORES a $MAXIMO_DE_JOGADORES jogadores, mas tem ${jogadores.size}."
        }
        require(jogadores.map { it.id }.toSet().size == jogadores.size) {
            "Partida com ids de jogador repetidos."
        }
        require(baralho.size >= totalDeRodadas) {
            "Baralho tem ${baralho.size} cards para $totalDeRodadas rodadas."
        }
        require(rodadaAtual in 1..totalDeRodadas) {
            "rodadaAtual $rodadaAtual fora de 1..$totalDeRodadas."
        }
        require(indiceDoLeitor in jogadores.indices) {
            "indiceDoLeitor $indiceDoLeitor fora de ${jogadores.indices}."
        }
        require(grupos.map { it.id }.toSet().size == grupos.size) {
            "Partida com ids de grupo repetidos."
        }
        val idsNosGrupos = grupos.flatMap { it.jogadores }
        require(idsNosGrupos.size == idsNosGrupos.toSet().size) {
            "Partida com jogador em mais de um grupo."
        }
        require(idsNosGrupos.toSet() == jogadores.map { it.id }.toSet()) {
            "Todo jogador precisa pertencer a exatamente um grupo."
        }
    }

    /** Leitor da rodada atual. */
    val leitorDaVez: Jogador
        get() = jogadores[indiceDoLeitor]

    /** Grupo ao qual o jogador [jogadorId] pertence — é a ele que os pontos vão. */
    fun grupoDe(jogadorId: String): Grupo =
        requireNotNull(grupos.firstOrNull { jogadorId in it.jogadores }) {
            "Jogador '$jogadorId' não pertence a nenhum grupo."
        }

    /**
     * Monta o turno da rodada atual: card da vez do baralho, leitor da vez,
     * os demais jogadores como adivinhadores e o grid de dicas embaralhado
     * por uma seed derivada da seed da partida e da rodada — determinístico
     * em qualquer aparelho.
     */
    fun iniciarTurno(): Turno {
        check(!encerrada) { "A partida já foi encerrada." }
        return Turno.criar(
            card = baralho[rodadaAtual - 1],
            leitor = leitorDaVez,
            adivinhadores = jogadores.filter { it.id != leitorDaVez.id },
            regras = regras,
            seedDasDicas = seedDasDicas(rodadaAtual),
        )
    }

    /**
     * Aplica o resultado de um turno encerrado: credita os pontos ao grupo do
     * acertador e ao grupo do leitor (que podem ser o mesmo grupo) e avança
     * para a próxima rodada com o próximo leitor (rodízio circular). Se esta
     * era a última rodada, a partida é encerrada com o placar final.
     *
     * @throws IllegalArgumentException se o turno não estiver encerrado ou não
     *   pertencer à rodada atual (leitor diferente do leitor da vez).
     */
    fun encerrarTurno(turno: Turno): Partida {
        check(!encerrada) { "A partida já foi encerrada." }
        val fim = turno.estado
        require(fim is EstadoDoTurno.TurnoEncerrado) {
            "O turno ainda não foi encerrado (estado atual: ${turno.estado})."
        }
        require(turno.leitor.id == leitorDaVez.id) {
            "O turno não pertence à rodada atual: leitor '${turno.leitor.id}', esperado '${leitorDaVez.id}'."
        }
        val novosGrupos = when (fim) {
            is EstadoDoTurno.TurnoEncerrado.Acerto -> grupos
                .creditar(grupoDe(fim.acertadorId).id, fim.pontosAcertador)
                .creditar(grupoDe(leitorDaVez.id).id, fim.pontosLeitor)

            is EstadoDoTurno.TurnoEncerrado.Queimado -> grupos
                .creditar(grupoDe(leitorDaVez.id).id, fim.pontosLeitor)
        }
        return if (rodadaAtual == totalDeRodadas) {
            copy(grupos = novosGrupos, encerrada = true)
        } else {
            copy(
                grupos = novosGrupos,
                rodadaAtual = rodadaAtual + 1,
                indiceDoLeitor = (indiceDoLeitor + 1) % jogadores.size,
            )
        }
    }

    /** Ranking por grupo, do maior para o menor número de pontos. */
    fun ranking(): List<Grupo> = grupos.sortedByDescending { it.pontos }

    /**
     * Grupo(s) com a maior pontuação. Empate na v1 é declarado — a lista traz
     * todos os empatados, sem desempate.
     */
    fun vencedores(): List<Grupo> {
        val maiorPontuacao = grupos.maxOf { it.pontos }
        return ranking().filter { it.pontos == maiorPontuacao }
    }

    private fun List<Grupo>.creditar(grupoId: String, pontos: Int): List<Grupo> =
        map { grupo -> if (grupo.id == grupoId) grupo.adicionarPontos(pontos) else grupo }

    /**
     * Seed do grid de dicas da [rodada]: combinação polinomial (mesma base do
     * hash de [com.quemsou.app.domain.rules.SeedDeCodigo]) da seed da partida
     * com o número da rodada — turnos diferentes, grids diferentes, sempre
     * reproduzíveis.
     */
    private fun seedDasDicas(rodada: Int): Long = seed * BASE_DA_SEED_DE_DICAS + rodada

    companion object {
        /** Mínimo de jogadores por partida (1 leitor + 1 adivinhador). */
        const val MINIMO_DE_JOGADORES = 2

        /** Máximo de jogadores por partida (1 leitor + 3 adivinhadores). */
        const val MAXIMO_DE_JOGADORES = 4

        private const val BASE_DA_SEED_DE_DICAS = 31L
    }
}
