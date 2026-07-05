package com.quemsou.app.data

import com.quemsou.app.data.local.CardDao
import com.quemsou.app.data.local.paraDominio
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.repository.RepositorioDeCards
import javax.inject.Inject

/**
 * Implementação real do [RepositorioDeCards]: lê da tabela `cards` do Room
 * (importada de `assets/cards.json`) e mapeia para o modelo de domínio.
 */
class RepositorioDeCardsLocal @Inject constructor(
    private val cardDao: CardDao,
) : RepositorioDeCards {

    override suspend fun buscarPorCategoria(categoria: CardCategory): List<Card> =
        when (categoria) {
            CardCategory.LIVRE -> cardDao.buscarTodas()
            else -> cardDao.buscarPorCategoria(categoria.name)
        }.map { it.paraDominio() }
}
