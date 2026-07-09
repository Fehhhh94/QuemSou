package com.quemsou.app.domain.usecase

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.Grupo
import com.quemsou.app.domain.model.Jogador
import com.quemsou.app.domain.model.Partida
import com.quemsou.app.domain.model.RegrasPartida
import com.quemsou.app.domain.rules.EmbaralhadorDeCards
import com.quemsou.app.domain.rules.SeedDeCodigo

/**
 * Monta uma [Partida] a partir do código informado: gera a seed determinística
 * do código ([SeedDeCodigo]) e embaralha o baralho com ela
 * ([EmbaralhadorDeCards]) — o mesmo código gera sempre a mesma partida.
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
     * @param cardsDisponiveis baralho completo disponível (ex.: vindos do banco).
     * @param grupos agrupamento dos jogadores; por padrão, cada jogador em um
     *   grupo próprio de tamanho 1 ([Grupo.individuais]).
     */
    fun executar(
        codigo: String,
        jogadores: List<Jogador>,
        regras: RegrasPartida,
        cardsDisponiveis: List<Card>,
        grupos: List<Grupo> = Grupo.individuais(jogadores),
    ): Partida {
        val seed = SeedDeCodigo.gerar(codigo)
        return Partida(
            jogadores = jogadores,
            regras = regras,
            baralho = EmbaralhadorDeCards.embaralhar(cardsDisponiveis, seed),
            seed = seed,
            grupos = grupos,
        )
    }
}
