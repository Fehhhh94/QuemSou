package com.quemsou.app.data.catalogo

import androidx.room.withTransaction
import com.quemsou.app.data.local.AppDatabase
import com.quemsou.app.data.local.paraEntidade
import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.EstadoDoBaralho
import com.quemsou.app.domain.validacao.ResultadoValidacao
import com.quemsou.app.domain.validacao.ValidadorEditorial
import javax.inject.Inject

/** Instala o resultado da fábrica numa única transação, preservando identidades e histórico. */
interface DestinoDeBaralhos {
    suspend fun instalado(id: String, versao: Int): Boolean
    suspend fun instalar(baralho: Baralho)
}

class InstaladorDeBaralhos @Inject constructor(private val db: AppDatabase) : DestinoDeBaralhos {
    override suspend fun instalado(id: String, versao: Int): Boolean =
        (db.baralhoDao().buscarPorIds(listOf(id)).singleOrNull()?.versao ?: 0) >= versao

    override suspend fun instalar(baralho: Baralho) = db.withTransaction {
        require(baralho.cards.all { ValidadorEditorial().validar(it) is ResultadoValidacao.Aprovado })
        val anterior = db.baralhoDao().buscarPorIds(listOf(baralho.id)).singleOrNull()
        if (anterior != null && anterior.versao >= baralho.versao) return@withTransaction
        require(anterior?.estado != EstadoDoBaralho.FINALIZADO.name)
        val outros = db.baralhoDao().buscarTodos().filter { it.id != baralho.id }.map { it.id }
        val idsDeOutros = db.cardDao().buscarPorBaralhos(outros).map { it.id }.toSet()
        require(baralho.cards.none { it.id in idsDeOutros })
        db.baralhoDao().inserirTodos(listOf(baralho.paraEntidade()))
        db.cardDao().removerPorBaralho(baralho.id)
        db.cardDao().inserirTodos(baralho.cards.map { it.paraEntidade(baralho.id) })
    }
}
