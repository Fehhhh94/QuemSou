package com.quemsou.app.domain.model

/**
 * Coleção de baralhos: **metadado de agrupamento** do catálogo (ex.:
 * "Cinema Clássico" 🎬 reúne "Edição 1", "Edição 2"…). Não é entidade de
 * domínio com regras — quem agrupa, filtra e exibe é a UI do catálogo; o
 * conteúdo é validado no nível do [Baralho].
 *
 * @property id identificador estável da coleção (slug).
 * @property nome nome de exibição.
 * @property icone emoji da coleção (String, ex.: "🎬").
 */
data class Colecao(
    val id: String,
    val nome: String,
    val icone: String,
)
