package com.quemsou.app.domain.usecase

import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.Grupo
import com.quemsou.app.domain.model.Jogador
import com.quemsou.app.domain.model.Partida
import com.quemsou.app.domain.model.RegrasPartida
import com.quemsou.app.domain.rules.EmbaralhadorDeCards
import com.quemsou.app.domain.rules.SeedDeCodigo

/**
 * Monta uma [Partida] a partir do código informado: gera a seed determinística
 * do código ([SeedDeCodigo]), monta o monte com a **união determinística** dos
 * baralhos selecionados ([Baralho.uniaoDeterministica] — ordenada por chave
 * estável antes de qualquer embaralhamento) e o embaralha com a seed
 * ([EmbaralhadorDeCards]) — a mesma seleção com o mesmo código gera sempre a
 * mesma partida, em qualquer aparelho.
 *
 * As validações (2–4 jogadores, cards suficientes, grupos cobrindo todos os
 * jogadores etc.) acontecem na construção da [Partida] e lançam
 * [IllegalArgumentException].
 */
object CriarPartida {

    /**
     * @param codigo código da partida (ex.: "LOBO"); normalizado pela seed.
     * @param jogadores jogadores na ordem de assento — o rodízio de leitor segue essa ordem.
     * @param regras regras configuradas antes da partida.
     * @param baralhosSelecionados baralhos escolhidos no Setup (1+); o monte é
     *   a união dos seus cards.
     * @param grupos agrupamento dos jogadores; por padrão, cada jogador em um
     *   grupo próprio de tamanho 1 ([Grupo.individuais]).
     */
    fun executar(
        codigo: String,
        jogadores: List<Jogador>,
        regras: RegrasPartida,
        baralhosSelecionados: List<Baralho>,
        grupos: List<Grupo> = Grupo.individuais(jogadores),
    ): Partida {
        val seed = SeedDeCodigo.gerar(codigo)
        return Partida(
            jogadores = jogadores,
            regras = regras,
            monte = EmbaralhadorDeCards.embaralhar(Baralho.uniaoDeterministica(baralhosSelecionados), seed),
            seed = seed,
            grupos = grupos,
        )
    }
}
