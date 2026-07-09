package com.quemsou.app.data.importer

import com.quemsou.app.data.catalogo.BaralhoJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Estrutura de `assets/cards.json`: uma versão inteira crescente e os
 * baralhos embarcados no APK, cada um no **mesmo formato do catálogo**
 * ([BaralhoJson], documentado em `docs/CATALOG_FORMAT.md`). O importador só
 * recarrega o banco quando [version] é maior que a última versão importada.
 */
@Serializable
data class CardsJson(
    val version: Int,
    val baralhos: List<BaralhoJson>,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Faz o parse do conteúdo de `cards.json`. Lança
         * [kotlinx.serialization.SerializationException] se a estrutura for
         * inválida ou faltar campo obrigatório — falha ruidosa por design: o
         * asset embarcado quebrado é bug de release, não entrada do usuário
         * (o `BaralhoDeAssetsTest` o valida antes de qualquer release).
         */
        fun deJson(conteudo: String): CardsJson = json.decodeFromString(conteudo)
    }
}
