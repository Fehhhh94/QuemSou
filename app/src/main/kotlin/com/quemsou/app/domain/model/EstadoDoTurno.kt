package com.quemsou.app.domain.model

/**
 * Máquina de estados de um [Turno]:
 *
 * ```
 * EscolhendoDica --revelarDica(posicao)--> DicaRevelada
 * DicaRevelada --outraDica()------------> EscolhendoDica (próximo escolhedor)
 * DicaRevelada --registrarAcerto(id)----> TurnoEncerrado.Acerto
 * qualquer ativo --queimarCard()--------> TurnoEncerrado.Queimado
 * DicaRevelada com 10 dicas --outraDica()-> TurnoEncerrado.Queimado
 * ```
 *
 * As transições vivem em [Turno] e lançam exceção quando inválidas.
 */
sealed interface EstadoDoTurno {

    /** O escolhedor da vez está escolhendo uma posição no grid 1–10. */
    data object EscolhendoDica : EstadoDoTurno

    /**
     * A dica da [posicao] acabou de ser revelada e o leitor a leu em voz alta;
     * os adivinhadores podem tentar acertar ou pedir outra dica.
     */
    data class DicaRevelada(val posicao: Int) : EstadoDoTurno

    /**
     * Fim de turno, anunciado com o resultado completo: a [resposta] é
     * revelada a todos e as [dicasUsadas] entram na conta dos pontos.
     */
    sealed interface TurnoEncerrado : EstadoDoTurno {
        val resposta: String
        val dicasUsadas: Int

        /**
         * Alguém acertou: [acertadorId] ganha [pontosAcertador] e o leitor
         * ganha [pontosLeitor] (0 quando [RegrasPartida.leitorPontua] está
         * desligado).
         */
        data class Acerto(
            override val resposta: String,
            override val dicasUsadas: Int,
            val acertadorId: String,
            val pontosAcertador: Int,
            val pontosLeitor: Int,
        ) : TurnoEncerrado

        /**
         * Card queimado (desistência ou 10 dicas sem acerto): o acertador não
         * pontua e o leitor ganha [pontosLeitor] — 10 pontos (0 quando
         * [RegrasPartida.leitorPontua] está desligado).
         */
        data class Queimado(
            override val resposta: String,
            override val dicasUsadas: Int,
            val pontosLeitor: Int,
        ) : TurnoEncerrado
    }
}
