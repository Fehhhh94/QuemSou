package com.quemsou.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Linha da tabela `feedback_de_cards` — um veredito do **modo dev de
 * feedback** (5B parte 2) sobre um card jogado, dado no Anúncio da partida.
 *
 * Histórico completo: o mesmo card pode receber feedback em partidas
 * diferentes — cada avaliação é uma **inserção nova**, nunca sobrescreve.
 * Deliberadamente **sem FK** para `cards`/`baralhos`: reimportação ou remoção
 * de baralho não pode apagar o histórico da fábrica; o export junta a
 * resposta por `cardId` quando o card ainda existe.
 *
 * [voto] e [resultadoDoTurno] guardam o `name` dos enums de
 * `data/feedback` (`VotoDeCard`, `ResultadoDoTurnoRegistrado`), no mesmo
 * padrão dos enums de domínio nas outras entidades.
 *
 * @property id chave autogerada — cada feedback é uma linha própria.
 * @property baralhoId id do baralho dono do card na hora do voto.
 * @property cardId id do card avaliado.
 * @property voto `BOM` ou `FRACO`.
 * @property comentario comentário opcional; `null` se não houver.
 * @property rodada rodada da partida em que o card foi jogado.
 * @property resultadoDoTurno `ACERTO` ou `QUEIMADO`.
 * @property numeroDaDicaDoAcerto dica em que houve o acerto; `null` se queimado.
 * @property criadoEm timestamp (epoch millis) da gravação.
 */
@Entity(tableName = "feedback_de_cards")
data class FeedbackDeCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val baralhoId: String,
    val cardId: String,
    val voto: String,
    val comentario: String?,
    val rodada: Int,
    val resultadoDoTurno: String,
    val numeroDaDicaDoAcerto: Int?,
    val criadoEm: Long,
)
