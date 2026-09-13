package com.quemsou.app.domain.model

/** Uma resposta compartilhada por temas e baralhos, com seu acervo de fatos. */
data class Resposta(
    val id: String,
    val nome: String,
    val temas: Set<CardCategory>,
    val dicas: List<DicaDoBanco>,
    val chaves: Set<String>,
    val referencias: List<Card>,
)

/** Checkpoint local: salvo junto das revelações antes de publicar a nova fase na UI. */
data class ProgressoDaPartida(
    val rodada: Int,
    val fase: String,
    val pontos: List<Int>,
    val posicoes: List<Int>,
    val acertador: String? = null,
    val shotPendente: Int? = null,
)
