package com.quemsou.app.domain.validacao

import com.quemsou.app.domain.model.Card

/**
 * Régua editorial mecânica dos cards, no domínio puro (sem Android).
 *
 * Extraída do antigo `BaralhoDeAssetsTest` na sub-fase 5.1 para servir a dois
 * consumidores com as **mesmas regras**: o teste do baralho real de
 * `assets/cards.json` e, na Fase 5, a validação de cards gerados por IA
 * (Gemini) antes da revisão humana. As regras aqui são só as mecanizáveis;
 * a régua editorial completa (curadoria) vive em `docs/CARDS_GUIDE.md`.
 *
 * Sem estado — [validar] é uma função pura. Todas as regras são checadas e
 * todas as violações são acumuladas (não para na primeira), para a tela de
 * revisão da Fase 5 exibir o card inteiro de uma vez.
 */
class ValidadorEditorial {

    /**
     * Valida o [card] contra todas as regras de [RegraEditorial]:
     * resposta não vazia, nenhuma dica vazia e nenhuma dica contendo a
     * resposta (sem diferenciar maiúsculas). Uma dica vazia não é checada
     * contra a resposta — a violação de [RegraEditorial.DICA_VAZIA] já a
     * reprova, e "conter a resposta" não faz sentido para texto em branco.
     */
    fun validar(card: Card): ResultadoValidacao {
        val violacoes = buildList {
            if (card.answer.isBlank()) {
                add(
                    ViolacaoEditorial(
                        regra = RegraEditorial.RESPOSTA_VAZIA,
                        mensagem = "A resposta do card está vazia.",
                    ),
                )
            }
            card.clues.forEachIndexed { indice, dica ->
                if (dica.isBlank()) {
                    add(
                        ViolacaoEditorial(
                            regra = RegraEditorial.DICA_VAZIA,
                            indiceDaDica = indice,
                            mensagem = "A dica ${indice + 1} está vazia.",
                        ),
                    )
                } else if (card.answer.isNotBlank() && dica.contains(card.answer, ignoreCase = true)) {
                    add(
                        ViolacaoEditorial(
                            regra = RegraEditorial.DICA_NOMEIA_RESPOSTA,
                            indiceDaDica = indice,
                            mensagem = "A dica ${indice + 1} nomeia a resposta \"${card.answer}\".",
                        ),
                    )
                }
            }
        }
        return if (violacoes.isEmpty()) {
            ResultadoValidacao.Aprovado
        } else {
            ResultadoValidacao.Reprovado(violacoes)
        }
    }
}
