package com.quemsou.app.domain.validacao

/**
 * As regras mecânicas da régua editorial, extraídas do antigo
 * `BaralhoDeAssetsTest` (sub-fase 5.1). A régua completa — incluindo a parte
 * de curadoria humana, que não é mecanizável — vive em `docs/CARDS_GUIDE.md`.
 *
 * A regra estrutural de **exatamente 10 dicas** não aparece aqui: ela é
 * garantida pelo construtor de [com.quemsou.app.domain.model.Card] (e pela
 * conversão do importador), então nenhum `Card` existente pode violá-la.
 */
enum class RegraEditorial {
    /** A resposta do card não pode ser vazia ou só espaços. */
    RESPOSTA_VAZIA,

    /** Nenhuma dica pode ser vazia ou só espaços. */
    DICA_VAZIA,

    /** Nenhuma dica pode conter a resposta do card (sem diferenciar maiúsculas). */
    DICA_NOMEIA_RESPOSTA,
}

/**
 * Uma violação da régua editorial encontrada num card.
 *
 * @property regra a regra violada.
 * @property indiceDaDica índice (base 0) da dica problemática em
 *   [com.quemsou.app.domain.model.Card.clues]; `null` quando a regra não diz
 *   respeito a uma dica específica (ex.: [RegraEditorial.RESPOSTA_VAZIA]).
 * @property mensagem descrição em português, legível para humanos — vai
 *   aparecer na tela de revisão de cards gerados por IA (Fase 5). Numera as
 *   dicas de 1 a 10, como o jogo faz.
 */
data class ViolacaoEditorial(
    val regra: RegraEditorial,
    val indiceDaDica: Int? = null,
    val mensagem: String,
)
