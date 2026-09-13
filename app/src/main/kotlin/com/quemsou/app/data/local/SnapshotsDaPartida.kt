package com.quemsou.app.data.local

import com.quemsou.app.data.catalogo.*
import com.quemsou.app.domain.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Snapshot interno tem contrato próprio: um acervo reunido pode exceder o teto de um download. */
@Serializable
private data class ConteudoSalvo(val baralhos: List<BaralhoJson>, val chaves: Map<String, Set<String>>)

@Serializable
private data class CartaSalva(val card: CardDoBaralhoJson, val categoria: String, val chaves: Set<String>)

@Serializable
private data class ProgressoSalvo(val rodada: Int, val fase: String, val pontos: List<Int>,
    val posicoes: List<Int>, val acertador: String?, val shotPendente: Int?)

object SnapshotsDaPartida {
    fun salvar(baralhos: List<Baralho>): String = Json.encodeToString(ConteudoSalvo(
        baralhos.map { it.paraJsonModelo() },
        baralhos.flatMap { it.cards }.associate { it.id to it.chavesDaResposta },
    ))

    fun baralhos(json: String): List<Baralho> {
        val salvo = if (json.trimStart().startsWith("["))
            ConteudoSalvo(Json.decodeFromString(json), emptyMap()) // v5
        else Json.decodeFromString<ConteudoSalvo>(json)
        return salvo.baralhos.map { b -> Baralho(b.id, b.nome, CardCategory.valueOf(b.categoria),
            Colecao(b.colecao.id, b.colecao.nome, b.colecao.icone), b.versao, EstadoDoBaralho.valueOf(b.estado),
            b.cards.map { it.dominio(CardCategory.valueOf(b.categoria), salvo.chaves[it.id].orEmpty()) }) }
    }

    fun salvar(card: Card): String = Json.encodeToString(CartaSalva(card.paraJsonModelo(), card.category.name, card.chavesDaResposta))
    fun card(json: String, categoriaLegada: CardCategory): Card =
        if ("\"categoria\"" in json) Json.decodeFromString<CartaSalva>(json).let { it.card.dominio(CardCategory.valueOf(it.categoria), it.chaves) }
        else Json.decodeFromString<CardDoBaralhoJson>(json).dominio(categoriaLegada, emptySet())

    fun salvar(p: ProgressoDaPartida): String = Json.encodeToString(ProgressoSalvo(p.rodada, p.fase, p.pontos, p.posicoes, p.acertador, p.shotPendente))
    fun progresso(json: String): ProgressoDaPartida? = json.takeIf { it.isNotBlank() }?.let {
        Json.decodeFromString<ProgressoSalvo>(it).let { p -> ProgressoDaPartida(p.rodada, p.fase, p.pontos, p.posicoes, p.acertador, p.shotPendente) }
    }

    private fun CardDoBaralhoJson.dominio(categoria: CardCategory, chaves: Set<String>) =
        Card(id, CardType.valueOf(type), categoria, answer, clues, respostaId, bancoDeDicas.map { DicaDoBanco(it.id, it.texto) }, chaves)
}
