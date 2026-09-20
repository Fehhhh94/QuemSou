package com.quemsou.app.domain.model

/**
 * Valor legado de transporte de um [Baralho] no catálogo.
 *
 * Os dois valores continuam aceitos para que JSONs e bancos existentes não
 * quebrem. Desde 2026-09-19 eles não representam ciclo de vida, não aparecem
 * na UI e não limitam atualização ou edição. A versão técnica continua sendo
 * o único controle de atualização de conteúdo.
 */
enum class EstadoDoBaralho {
    /**
     * Valor usado pelos baralhos criados depois da simplificação editorial.
     */
    EM_DESENVOLVIMENTO,

    /**
     * Valor legado. Baralhos antigos com este valor também são atualizáveis.
     */
    FINALIZADO,
}
