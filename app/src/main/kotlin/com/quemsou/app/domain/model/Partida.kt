package com.quemsou.app.domain.model

/**
 * Uma partida do QuemSou: jogadores, regras, baralho já embaralhado e o
 * andamento (rodada atual, leitor da vez, placar).
 *
 * Imutável: [encerrarTurno] retorna uma nova partida com o placar atualizado,
 * a próxima rodada e o próximo leitor (rodízio circular). Após
 * [totalDeRodadas] rodadas, a partida fica [encerrada] com o placar final.
 *
 * @property jogadores de [MINIMO_DE_JOGADORES] a [MAXIMO_DE_JOGADORES] jogadores, ids únicos.
 * @property modoDeJogo individual ou times; em [ModoDeJogo.TIMES] todo jogador
 *   precisa de [Jogador.timeId] e deve haver pelo menos 2 times.
 * @property regras regras configuradas antes da partida.
 * @property baralho cards já embaralhados pela seed; um card por rodada.
 * @property seed seed da partida (gerada do código); também deriva o
 *   embaralhamento das dicas de cada turno.
 * @property rodadaAtual rodada em jogo, de 1 a [totalDeRodadas].
 * @property indiceDoLeitor índice em [jogadores] do leitor da vez.
 * @property placar pontos acumulados por jogador.
 * @property encerrada `true` após a última rodada; nenhuma ação é permitida.
 */
data class Partida(
    val jogadores: List<Jogador>,
    val modoDeJogo: ModoDeJogo,
    val regras: RegrasPartida,
    val baralho: List<Card>,
    val seed: Long,
    val rodadaAtual: Int = 1,
    val indiceDoLeitor: Int = 0,
    val placar: Placar = Placar.inicial(jogadores),
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
        if (modoDeJogo == ModoDeJogo.TIMES) {
            require(jogadores.all { it.timeId != null }) {
                "No modo TIMES todo jogador precisa de timeId."
            }
            require(jogadores.mapNotNull { it.timeId }.toSet().size >= 2) {
                "No modo TIMES são necessários pelo menos 2 times."
            }
        }
    }

    /** Leitor da rodada atual. */
    val leitorDaVez: Jogador
        get() = jogadores[indiceDoLeitor]

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
     * Aplica o resultado de um turno encerrado: soma os pontos ao placar e
     * avança para a próxima rodada com o próximo leitor (rodízio circular).
     * Se esta era a última rodada, a partida é encerrada com o placar final.
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
        val novoPlacar = when (fim) {
            is EstadoDoTurno.TurnoEncerrado.Acerto -> placar
                .adicionar(fim.acertadorId, fim.pontosAcertador)
                .adicionar(leitorDaVez.id, fim.pontosLeitor)

            is EstadoDoTurno.TurnoEncerrado.Queimado -> placar
        }
        return if (rodadaAtual == totalDeRodadas) {
            copy(placar = novoPlacar, encerrada = true)
        } else {
            copy(
                placar = novoPlacar,
                rodadaAtual = rodadaAtual + 1,
                indiceDoLeitor = (indiceDoLeitor + 1) % jogadores.size,
            )
        }
    }

    /**
     * Vencedor(es) segundo o modo de jogo: ids de jogadores no
     * [ModoDeJogo.INDIVIDUAL], ids de times no [ModoDeJogo.TIMES]. Empate na
     * v1 é declarado — a lista traz todos os empatados, sem desempate.
     */
    fun vencedores(): List<String> = when (modoDeJogo) {
        ModoDeJogo.INDIVIDUAL -> placar.vencedores()
        ModoDeJogo.TIMES -> placar.vencedoresPorTime(jogadores)
    }

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
