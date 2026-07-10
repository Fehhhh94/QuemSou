package com.quemsou.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migração 1 → 2 (Fase 5A): cria a tabela `baralhos` e adiciona `baralhoId`
 * (FK) em `cards`, preservando os dados existentes — os 60 cards da versão 1
 * são atribuídos aos dois baralhos embarcados pela categoria que já tinham.
 *
 * O SQLite não adiciona FK em tabela existente, então `cards` é recriada e
 * copiada. Logo após a migração o `CardsImporter` reimporta tudo do asset
 * (version 3 > 2), mas a cópia garante um banco íntegro mesmo se o processo
 * morrer entre a migração e a importação.
 */
val MIGRACAO_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `baralhos` (" +
                "`id` TEXT NOT NULL, `nome` TEXT NOT NULL, `categoria` TEXT NOT NULL, " +
                "`versao` INTEGER NOT NULL, `estado` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `baralhos` (`id`, `nome`, `categoria`, `versao`, `estado`) VALUES " +
                "('cinema-classico-1', 'Cinema Clássico — Edição 1', 'PERSONAGEM_FILME', 1, 'FINALIZADO'), " +
                "('mundo-da-musica-1', 'Mundo da Música — Edição 1', 'MUNDO_DA_MUSICA', 1, 'FINALIZADO')",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cards_novo` (" +
                "`id` TEXT NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                "`answer` TEXT NOT NULL, `clues` TEXT NOT NULL, `baralhoId` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`baralhoId`) REFERENCES `baralhos`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL(
            "INSERT INTO `cards_novo` (`id`, `type`, `category`, `answer`, `clues`, `baralhoId`) " +
                "SELECT `id`, `type`, `category`, `answer`, `clues`, " +
                "CASE `category` WHEN 'PERSONAGEM_FILME' THEN 'cinema-classico-1' " +
                "ELSE 'mundo-da-musica-1' END FROM `cards`",
        )
        db.execSQL("DROP TABLE `cards`")
        db.execSQL("ALTER TABLE `cards_novo` RENAME TO `cards`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_baralhoId` ON `cards` (`baralhoId`)")
    }
}

/**
 * Migração 2 → 3 (5A parte 2): a coleção (metadado de agrupamento) entra em
 * `baralhos` como três colunas achatadas. A tabela é recriada (em vez de
 * `ALTER TABLE ... ADD COLUMN`) para o schema final ficar idêntico ao que o
 * Room valida; os dois baralhos embarcados da v2 ganham as coleções próprias
 * pela categoria. Logo após, o `CardsImporter` reimporta o asset (version 4).
 */
val MIGRACAO_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `baralhos_novo` (" +
                "`id` TEXT NOT NULL, `nome` TEXT NOT NULL, `categoria` TEXT NOT NULL, " +
                "`versao` INTEGER NOT NULL, `estado` TEXT NOT NULL, " +
                "`colecaoId` TEXT NOT NULL, `colecaoNome` TEXT NOT NULL, `colecaoIcone` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `baralhos_novo` " +
                "SELECT `id`, `nome`, `categoria`, `versao`, `estado`, " +
                "CASE `categoria` WHEN 'PERSONAGEM_FILME' THEN 'cinema-classico' ELSE 'mundo-da-musica' END, " +
                "CASE `categoria` WHEN 'PERSONAGEM_FILME' THEN 'Cinema Clássico' ELSE 'Mundo da Música' END, " +
                "CASE `categoria` WHEN 'PERSONAGEM_FILME' THEN '🎬' ELSE '🎸' END " +
                "FROM `baralhos`",
        )
        db.execSQL("DROP TABLE `baralhos`")
        db.execSQL("ALTER TABLE `baralhos_novo` RENAME TO `baralhos`")
    }
}
