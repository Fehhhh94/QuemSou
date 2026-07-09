package com.quemsou.app.data

import com.quemsou.app.data.local.BaralhoDao
import com.quemsou.app.data.local.CardDao
import com.quemsou.app.data.local.paraDominio
import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.repository.RepositorioDeCards
import javax.inject.Inject

/**
 * Implementação real do [RepositorioDeCards]: lê das tabelas `baralhos` e
 * `cards` do Room (importadas de `assets/cards.json`) e monta os baralhos de
 * domínio com os seus cards.
 */
class RepositorioDeCardsLocal @Inject constructor(
    private val baralhoDao: BaralhoDao,
    private val cardDao: CardDao,
) : RepositorioDeCards {

    override suspend fun buscarPorIds(ids: List<String>): List<Baralho> {
        val cardsPorBaralho = cardDao.buscarPorBaralhos(ids).groupBy { it.baralhoId }
        return baralhoDao.buscarPorIds(ids).map { entidade ->
            entidade.paraDominio(cards = cardsPorBaralho[entidade.id].orEmpty().map { it.paraDominio() })
        }
    }
}
