package com.quemsou.app.data.espelho

import kotlinx.serialization.Serializable

/** Fase da partida que o servidor local pode refletir aos navegadores. */
enum class FaseDoEspelho(val codigo: String) {
    AGUARDANDO("aguardando"),
    VEZ_DE_JOGAR("vez_de_jogar"),
    GRID("grid"),
    SHOT("shot"),
    DICA_REVELADA("dica_revelada"),
    QUEM_ACERTOU("quem_acertou"),
    ANUNCIO("anuncio"),
    PLACAR_FINAL("placar_final"),
    INDISPONIVEL("indisponivel"),
}

/** Uma linha do placar final publicada pelo espelho. */
data class LinhaDoPlacarNoEspelho(
    val nome: String,
    val pontos: Int,
)

/**
 * Estado interno publicado pelo jogo no servidor local.
 *
 * [resposta] e [dica] ficam apenas na memória do aparelho anfitrião. Antes de
 * serializar, [paraJogador] remove os segredos de qualquer sessão que não seja
 * a do leitor. A resposta só se torna pública no anúncio do fim do turno.
 */
data class EstadoDaPartidaNoEspelho(
    val fase: FaseDoEspelho,
    val rodada: Int? = null,
    val totalDeRodadas: Int? = null,
    val leitorId: String? = null,
    val leitorNome: String? = null,
    val escolhedorNome: String? = null,
    val resposta: String? = null,
    val dica: String? = null,
    val valor: Int? = null,
    val mensagem: String? = null,
    val ranking: List<LinhaDoPlacarNoEspelho> = emptyList(),
) {
    companion object {
        val Aguardando = EstadoDaPartidaNoEspelho(FaseDoEspelho.AGUARDANDO)
    }
}

/** Contrato JSON entregue a uma sessão autenticada do navegador. */
@Serializable
internal data class EstadoEntregueAoJogador(
    val fase: String,
    val rodada: Int? = null,
    val totalDeRodadas: Int? = null,
    val leitor: String? = null,
    val escolhedor: String? = null,
    val suaVez: Boolean = false,
    val resposta: String? = null,
    val dica: String? = null,
    val valor: Int? = null,
    val mensagem: String? = null,
    val ranking: List<LinhaDoPlacarEntregue> = emptyList(),
)

@Serializable
internal data class LinhaDoPlacarEntregue(
    val nome: String,
    val pontos: Int,
)

/** Aplica o limite de informação antes de qualquer byte sair pela rede. */
internal fun EstadoDaPartidaNoEspelho.paraJogador(jogadorId: String): EstadoEntregueAoJogador {
    val eLeitor = leitorId != null && leitorId == jogadorId
    val respostaPublica = fase == FaseDoEspelho.ANUNCIO
    return EstadoEntregueAoJogador(
        fase = fase.codigo,
        rodada = rodada,
        totalDeRodadas = totalDeRodadas,
        leitor = leitorNome,
        escolhedor = escolhedorNome,
        suaVez = eLeitor,
        resposta = resposta.takeIf { eLeitor || respostaPublica },
        dica = dica.takeIf { eLeitor },
        valor = valor,
        mensagem = mensagem,
        ranking = ranking.map { LinhaDoPlacarEntregue(it.nome, it.pontos) },
    )
}
