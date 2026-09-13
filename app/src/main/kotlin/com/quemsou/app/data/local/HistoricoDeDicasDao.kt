package com.quemsou.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.ColumnInfo

/** Sem FK: atualizar ou remover conteúdo nunca apaga o que este celular já utilizou. */
@Entity(tableName = "dicas_utilizadas")
data class DicaUtilizadaEntity(@PrimaryKey val chave: String)

@Entity(tableName = "sessoes_de_dicas")
data class SessaoDeDicasEntity(@PrimaryKey val id: String, val baralhosJson: String,
    @ColumnInfo(defaultValue = "'{}'") val historicoJson: String = "{}",
    @ColumnInfo(defaultValue = "''") val progressoJson: String = "",
    @ColumnInfo(defaultValue = "0") val encerrada: Boolean = false)

@Entity(tableName = "turnos_de_dicas", primaryKeys = ["sessao", "rodada"])
data class TurnoDeDicasEntity(val sessao: String, val rodada: Int, val cardJson: String)

@Entity(tableName = "respostas_jogadas")
data class RespostaJogadaEntity(@PrimaryKey val chave: String, val ordem: Long)

/** Reserva temporária; liberar nunca apaga o histórico das dicas já reveladas. */
@Entity(tableName = "dicas_reservadas")
data class DicaReservadaEntity(@PrimaryKey val chave: String, val sessao: String, val rodada: Int)

@Dao
interface HistoricoDeDicasDao {
    @Query("SELECT * FROM respostas_jogadas")
    suspend fun respostas(): List<RespostaJogadaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarRespostas(itens: List<RespostaJogadaEntity>)

    @Query("SELECT chave FROM dicas_reservadas")
    suspend fun reservadas(): List<String>

    @Insert
    suspend fun reservar(itens: List<DicaReservadaEntity>)

    @Query("DELETE FROM dicas_reservadas WHERE sessao = :sessao AND rodada = :rodada")
    suspend fun liberarTurno(sessao: String, rodada: Int)

    @Query("DELETE FROM dicas_reservadas WHERE sessao = :sessao")
    suspend fun liberarSessao(sessao: String)

    @Query("UPDATE sessoes_de_dicas SET encerrada = 1 WHERE id = :sessao")
    suspend fun encerrar(sessao: String)

    @Query("SELECT id FROM sessoes_de_dicas WHERE encerrada = 0")
    suspend fun sessoesAtivas(): List<String>

    @Query("UPDATE sessoes_de_dicas SET progressoJson = :progresso WHERE id = :sessao")
    suspend fun salvarProgresso(sessao: String, progresso: String)

    @Query("SELECT chave FROM dicas_utilizadas")
    suspend fun usadas(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun registrar(itens: List<DicaUtilizadaEntity>)

    @Query("SELECT * FROM sessoes_de_dicas WHERE id = :id")
    suspend fun sessao(id: String): SessaoDeDicasEntity?

    @Insert
    suspend fun inserirSessao(sessao: SessaoDeDicasEntity)

    @Query("SELECT * FROM turnos_de_dicas WHERE sessao = :sessao AND rodada = :rodada")
    suspend fun turno(sessao: String, rodada: Int): TurnoDeDicasEntity?

    @Insert
    suspend fun inserirTurno(turno: TurnoDeDicasEntity)
}
