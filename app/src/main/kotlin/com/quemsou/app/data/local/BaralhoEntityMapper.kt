package com.quemsou.app.data.local

import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.EstadoDoBaralho

/**
 * Converte a entidade persistida no modelo de domínio, montando o baralho
 * com os seus [cards]. Lança [IllegalArgumentException] se [BaralhoEntity.categoria]
 * ou [BaralhoEntity.estado] não corresponderem a um enum conhecido.
 */
fun BaralhoEntity.paraDominio(cards: List<Card>): Baralho = Baralho(
    id = id,
    nome = nome,
    categoria = CardCategory.valueOf(categoria),
    versao = versao,
    estado = EstadoDoBaralho.valueOf(estado),
    cards = cards,
)

/** Converte o modelo de domínio na entidade persistida (metadados; os cards vão para a tabela `cards`). */
fun Baralho.paraEntidade(): BaralhoEntity = BaralhoEntity(
    id = id,
    nome = nome,
    categoria = categoria.name,
    versao = versao,
    estado = estado.name,
)
