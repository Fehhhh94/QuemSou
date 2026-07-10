package com.quemsou.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Linha da tabela `baralhos` — metadados persistidos de um baralho do
 * catálogo. A contagem de cards não é armazenada: deriva da tabela `cards`
 * (via [com.quemsou.app.domain.model.Baralho.quantidadeDeCards] no domínio).
 *
 * [categoria] e [estado] guardam o `name` dos enums de domínio, como
 * [CardEntity] já faz. A coleção (metadado de agrupamento) é achatada em
 * três colunas. A conversão vive em `BaralhoEntityMapper`.
 */
@Entity(tableName = "baralhos")
data class BaralhoEntity(
    @PrimaryKey val id: String,
    val nome: String,
    val categoria: String,
    val versao: Int,
    val estado: String,
    val colecaoId: String,
    val colecaoNome: String,
    val colecaoIcone: String,
)
