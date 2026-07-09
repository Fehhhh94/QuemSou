package com.quemsou.app.domain.validacao

import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.CardCategory

/**
 * Regras de baralho inteiro, na mesma filosofia do [ValidadorEditorial]:
 * função pura, todas as regras checadas, todas as violações acumuladas com
 * mensagem legível em português — nunca exceção de construtor. Serve ao
 * importador de assets, ao parser do catálogo e à fábrica interna.
 *
 * As regras por card (resposta/dica vazias, dica que nomeia a resposta)
 * continuam no [ValidadorEditorial]; aqui ficam só as do conjunto.
 */
class ValidadorDeBaralho {

    /**
     * Valida o [baralho] contra todas as regras de [RegraDeBaralho]:
     * não vazio, teto de [Baralho.MAXIMO_DE_CARDS] cards, ids de card únicos,
     * categoria real (nunca `LIVRE` — "Livre" é seleção de todos os baralhos,
     * não categoria de conteúdo) e todo card com a categoria do baralho.
     */
    fun validar(baralho: Baralho): ResultadoValidacaoDeBaralho {
        val violacoes = buildList {
            if (baralho.cards.isEmpty()) {
                add(
                    ViolacaoDeBaralho(
                        regra = RegraDeBaralho.BARALHO_VAZIO,
                        mensagem = "O baralho '${baralho.id}' não tem nenhum card.",
                    ),
                )
            }
            if (baralho.quantidadeDeCards > Baralho.MAXIMO_DE_CARDS) {
                add(
                    ViolacaoDeBaralho(
                        regra = RegraDeBaralho.TETO_DE_CARDS_EXCEDIDO,
                        mensagem = "O baralho '${baralho.id}' tem ${baralho.quantidadeDeCards} cards; " +
                            "o máximo é ${Baralho.MAXIMO_DE_CARDS} — crescimento além disso vira um baralho novo.",
                    ),
                )
            }
            val idsRepetidos = baralho.cards.groupBy { it.id }.filterValues { it.size > 1 }.keys
            if (idsRepetidos.isNotEmpty()) {
                add(
                    ViolacaoDeBaralho(
                        regra = RegraDeBaralho.IDS_DE_CARDS_REPETIDOS,
                        mensagem = "O baralho '${baralho.id}' tem ids de card repetidos: " +
                            "${idsRepetidos.sorted().joinToString(", ")}.",
                    ),
                )
            }
            if (baralho.categoria == CardCategory.LIVRE) {
                add(
                    ViolacaoDeBaralho(
                        regra = RegraDeBaralho.CATEGORIA_LIVRE,
                        mensagem = "O baralho '${baralho.id}' usa a categoria LIVRE; " +
                            "\"Livre\" é a seleção de todos os baralhos, não uma categoria de conteúdo.",
                    ),
                )
            }
            baralho.cards
                .filter { it.category != baralho.categoria }
                .forEach { card ->
                    add(
                        ViolacaoDeBaralho(
                            regra = RegraDeBaralho.CARD_DE_CATEGORIA_DIVERGENTE,
                            mensagem = "O card '${card.id}' tem categoria ${card.category}, " +
                                "mas o baralho '${baralho.id}' é de ${baralho.categoria} — " +
                                "a categoria do card é herdada do baralho.",
                        ),
                    )
                }
        }
        return if (violacoes.isEmpty()) {
            ResultadoValidacaoDeBaralho.Aprovado
        } else {
            ResultadoValidacaoDeBaralho.Reprovado(violacoes)
        }
    }
}

/** As regras de conjunto de um baralho ([ValidadorDeBaralho]). */
enum class RegraDeBaralho {
    /** O baralho precisa ter ao menos um card. */
    BARALHO_VAZIO,

    /** O baralho não pode passar de [Baralho.MAXIMO_DE_CARDS] cards. */
    TETO_DE_CARDS_EXCEDIDO,

    /** Os ids de card precisam ser únicos dentro do baralho (chave da união determinística). */
    IDS_DE_CARDS_REPETIDOS,

    /** A categoria do baralho precisa ser real — `LIVRE` é seleção, não conteúdo. */
    CATEGORIA_LIVRE,

    /** Todo card do baralho precisa ter a categoria do baralho (herdada). */
    CARD_DE_CATEGORIA_DIVERGENTE,
}

/**
 * Uma violação de regra de baralho, com mensagem em português legível para
 * humanos — aparece no log do importador, no erro da tela de catálogo e na
 * revisão da fábrica interna.
 */
data class ViolacaoDeBaralho(
    val regra: RegraDeBaralho,
    val mensagem: String,
)

/**
 * Resultado da validação de um baralho ([ValidadorDeBaralho.validar]):
 * aprovado, ou reprovado com a lista completa de violações — todas as regras
 * são checadas, nada de parar na primeira.
 */
sealed interface ResultadoValidacaoDeBaralho {

    /** O baralho passou em todas as regras de conjunto. */
    data object Aprovado : ResultadoValidacaoDeBaralho

    /** O baralho violou ao menos uma regra; [violacoes] nunca é vazia. */
    data class Reprovado(
        val violacoes: List<ViolacaoDeBaralho>,
    ) : ResultadoValidacaoDeBaralho {
        init {
            require(violacoes.isNotEmpty()) { "Reprovado exige ao menos uma violação." }
        }
    }
}
