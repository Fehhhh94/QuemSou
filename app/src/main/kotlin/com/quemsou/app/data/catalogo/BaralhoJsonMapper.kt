package com.quemsou.app.data.catalogo

import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.Card

fun Card.paraJsonModelo() = CardDoBaralhoJson(id, type.name, answer, clues, respostaId,
    bancoDeDicas.map { DicaJson(it.id, it.texto) })

fun Baralho.paraJsonModelo() = BaralhoJson(id, nome, categoria.name,
    ColecaoJson(colecao.id, colecao.nome, colecao.icone), versao, estado.name,
    cards.map { it.paraJsonModelo() })
