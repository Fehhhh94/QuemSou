package com.quemsou.app.domain.model

/**
 * Ciclo de vida de um [Baralho] no catálogo.
 *
 * O estado é governança de conteúdo (o catálogo e a UI o sinalizam com um
 * selo); nenhuma regra de partida muda com ele.
 */
enum class EstadoDoBaralho {
    /**
     * Versões novas podem adicionar, remover ou melhorar cards; o app
     * atualiza pelo versionamento do baralho. Selo "em evolução" na UI.
     */
    EM_DESENVOLVIMENTO,

    /**
     * Imutável para sempre: evolução só via novo baralho ou extensão
     * (ex.: "Harry Potter 2"). Selo "edição final" na UI.
     */
    FINALIZADO,
}
