package com.quemsou.app.domain.model

/**
 * Um baralho do catálogo: conjunto temático de cards de uma categoria (ex.:
 * `PERSONAGEM_FILME` → "Cinema Clássico — Edição 1"). Todo card pertence a
 * exatamente um baralho — a categoria é metadado do baralho, herdada pelos
 * seus cards.
 *
 * O teto de [MAXIMO_DE_CARDS] cards por baralho **não** é validado aqui: é
 * regra de conteúdo com violação legível, checada pelo
 * [com.quemsou.app.domain.validacao.ValidadorDeBaralho] (mesma filosofia do
 * `ValidadorEditorial`) — crescimento além do teto vira um baralho novo.
 *
 * @property id identificador estável do baralho no catálogo (ex.:
 *   "cinema-classico-1"); participa da chave de ordenação da união.
 * @property nome nome de exibição (ex.: "Cinema Clássico — Edição 1").
 * @property categoria categoria temática do baralho inteiro.
 * @property colecao coleção a que o baralho pertence (metadado de
 *   agrupamento do catálogo).
 * @property versao versão inteira crescente do conteúdo; dirige a
 *   atualização por download no catálogo.
 * @property estado ciclo de vida ([EstadoDoBaralho]).
 * @property cards os cards do baralho, na ordem do JSON de origem — a ordem
 *   NÃO importa para a partida (ver [uniaoDeterministica]).
 */
data class Baralho(
    val id: String,
    val nome: String,
    val categoria: CardCategory,
    val colecao: Colecao,
    val versao: Int,
    val estado: EstadoDoBaralho,
    val cards: List<Card>,
) {
    /** Contagem de cards, derivada — nunca armazenada em separado. */
    val quantidadeDeCards: Int
        get() = cards.size

    init {
        require(id.isNotBlank()) { "Baralho com id vazio." }
        require(nome.isNotBlank()) { "Baralho '$id' com nome vazio." }
        require(versao >= 1) { "Baralho '$id' com versão $versao; a versão mínima é 1." }
    }

    companion object {
        /**
         * Teto de cards por baralho. Crescimento além disso vira um baralho
         * novo (ex.: "Harry Potter 2"), preferindo subtítulos temáticos.
         */
        const val MAXIMO_DE_CARDS = 100

        /**
         * Monte da partida a partir dos [baralhos] selecionados: a união dos
         * cards **ordenada por chave estável** — (id do baralho, id do card)
         * — ANTES do embaralhamento por seed. Assim, mesma seleção + mesma
         * seed → mesmo monte, independentemente da ordem de seleção, de
         * download ou de inserção no Room. Baralhos repetidos na seleção
         * contam uma única vez.
         */
        fun uniaoDeterministica(baralhos: List<Baralho>): List<Card> = baralhos
            .distinctBy { it.id }
            .sortedBy { it.id }
            .flatMap { baralho -> baralho.cards.sortedBy { it.id } }
    }
}
