package com.quemsou.app.data.importer

import com.quemsou.app.data.local.CardDao
import com.quemsou.app.data.local.paraEntidade
import javax.inject.Inject

/**
 * Importa os cards de `assets/cards.json` para o banco Room, com versionamento:
 * só recarrega quando a versão do asset é maior que a última importada.
 *
 * Cards inválidos (answer vazio, dicas faltando ou vazias) interrompem a
 * importação com [IllegalArgumentException] identificando o card — falha
 * ruidosa por design.
 */
class CardsImporter @Inject constructor(
    private val fonte: FonteDeCardsJson,
    private val cardDao: CardDao,
    private val versionStore: CardsVersionStore,
) {

    /**
     * Compara a versão do asset com a última importada e recarrega a tabela
     * `cards` se o asset for mais novo; caso contrário não toca no banco.
     */
    suspend fun importarSeNecessario(): ResultadoImportacao {
        val cardsJson = CardsJson.deJson(fonte.ler())
        val versaoImportada = versionStore.versaoImportada()
        if (cardsJson.version <= versaoImportada) {
            return ResultadoImportacao.NadaAFazer(versaoImportada)
        }

        val entidades = cardsJson.cards.map { it.paraDominio().paraEntidade() }
        cardDao.limparTabela()
        cardDao.inserirTodos(entidades)
        versionStore.salvarVersaoImportada(cardsJson.version)
        return ResultadoImportacao.Importado(quantidade = entidades.size, versao = cardsJson.version)
    }
}

/** Desfecho de uma chamada a [CardsImporter.importarSeNecessario]. */
sealed interface ResultadoImportacao {

    /** A tabela foi recarregada com [quantidade] cards da versão [versao]. */
    data class Importado(val quantidade: Int, val versao: Int) : ResultadoImportacao

    /** O banco já estava na versão [versao] (ou mais nova); nada foi alterado. */
    data class NadaAFazer(val versao: Int) : ResultadoImportacao
}
