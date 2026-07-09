package com.quemsou.app.domain.validacao

/**
 * Resultado da validação editorial de um card
 * ([ValidadorEditorial.validar]): aprovado, ou reprovado com a lista
 * completa de violações — todas as regras são checadas, nada de parar na
 * primeira, para a tela de revisão da Fase 5 mostrar tudo de uma vez.
 */
sealed interface ResultadoValidacao {

    /** O card passou em todas as regras mecânicas da régua editorial. */
    data object Aprovado : ResultadoValidacao

    /** O card violou ao menos uma regra; [violacoes] nunca é vazia. */
    data class Reprovado(
        val violacoes: List<ViolacaoEditorial>,
    ) : ResultadoValidacao {
        init {
            require(violacoes.isNotEmpty()) { "Reprovado exige ao menos uma violação." }
        }
    }
}
