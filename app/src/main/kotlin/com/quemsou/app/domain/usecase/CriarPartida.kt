package com.quemsou.app.domain.usecase

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.Jogador
import com.quemsou.app.domain.model.ModoDeJogo
import com.quemsou.app.domain.model.Partida
import com.quemsou.app.domain.model.RegrasPartida
import com.quemsou.app.domain.rules.EmbaralhadorDeCards
import com.quemsou.app.domain.rules.SeedDeCodigo

/**
 * Monta uma [Partida] a partir do código informado: gera a seed determinística
 * do código ([SeedDeCodigo]) e embaralha o baralho com ela
 * ([EmbaralhadorDeCards]) — o mesmo código gera sempre a mesma partida.
 *
 * As validações (2–4 jogadores, cards suficientes, times etc.) acontecem na
 * construção da [Partida] e lançam [IllegalArgumentException].
 */
object CriarPartida {

    /**
     * @param codigo código da partida (ex.: "LOBO"); normalizado pela seed.
     * @param jogadores jogadores na ordem de assento — o rodízio de leitor segue essa ordem.
     * @param modoDeJogo individual ou times.
     * @param regras regras configuradas antes da partida.
     * @param cardsDisponiveis baralho completo disponível (ex.: vindos do banco).
     */
    fun executar(
        codigo: String,
        jogadores: List<Jogador>,
        modoDeJogo: ModoDeJogo,
        regras: RegrasPartida,
        cardsDisponiveis: List<Card>,
    ): Partida {
        val seed = SeedDeCodigo.gerar(codigo)
        return Partida(
            jogadores = jogadores,
            modoDeJogo = modoDeJogo,
            regras = regras,
            baralho = EmbaralhadorDeCards.embaralhar(cardsDisponiveis, seed),
            seed = seed,
        )
    }
}
