package com.quemsou.app.data.catalogo

import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.CardCategory
import com.quemsou.app.domain.model.CardType
import com.quemsou.app.domain.model.Colecao
import com.quemsou.app.domain.model.EstadoDoBaralho
import com.quemsou.app.domain.validacao.ResultadoValidacaoDeBaralho
import com.quemsou.app.domain.validacao.ValidadorDeBaralho
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Parser dos JSONs do catálogo com **validação estrutural antes de construir
 * qualquer entidade de domínio**: campo faltando, enum desconhecido, card com
 * 8 dicas etc. viram [ViolacaoDeFormato] legível em português — nunca exceção
 * crua de desserialização ou de construtor. Todas as violações são acumuladas.
 *
 * Depois da estrutura, o baralho ainda passa pelo
 * [ValidadorDeBaralho] (teto de 100, ids únicos, categoria real) — as
 * violações dele entram no mesmo resultado.
 */
class ParserDoCatalogo @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    private val validadorDeBaralho = ValidadorDeBaralho()

    /** Faz o parse e valida o índice do catálogo. */
    fun parseIndice(conteudo: String): ResultadoDoParse<List<EntradaDoCatalogo>> {
        val indice = try {
            json.decodeFromString<IndiceDoCatalogoJson>(conteudo)
        } catch (excecao: SerializationException) {
            return falhaDeJson(excecao)
        } catch (excecao: IllegalArgumentException) {
            return falhaDeJson(excecao)
        }
        val violacoes = mutableListOf<ViolacaoDeFormato>()
        val entradas = indice.baralhos.mapIndexedNotNull { indiceDaEntrada, entrada ->
            validarEntrada(entrada, caminho = "baralhos[$indiceDaEntrada]", violacoes)
        }
        return if (violacoes.isEmpty()) {
            ResultadoDoParse.Sucesso(entradas)
        } else {
            ResultadoDoParse.Falha(violacoes)
        }
    }

    /** Faz o parse e valida um baralho completo do catálogo. */
    fun parseBaralho(conteudo: String): ResultadoDoParse<Baralho> {
        val baralhoJson = try {
            json.decodeFromString<BaralhoJson>(conteudo)
        } catch (excecao: SerializationException) {
            return falhaDeJson(excecao)
        } catch (excecao: IllegalArgumentException) {
            return falhaDeJson(excecao)
        }
        return validarBaralho(baralhoJson)
    }

    /**
     * Valida um [BaralhoJson] já desserializado e o converte no domínio —
     * também é o caminho do importador para os baralhos embarcados em
     * `assets/cards.json` (mesmo formato do catálogo).
     */
    fun validarBaralho(baralhoJson: BaralhoJson): ResultadoDoParse<Baralho> {
        val violacoes = mutableListOf<ViolacaoDeFormato>()
        val id = baralhoJson.id.ifBlank {
            violacoes += ViolacaoDeFormato("id", "O id do baralho está vazio.")
            "?"
        }
        if (baralhoJson.nome.isBlank()) {
            violacoes += ViolacaoDeFormato("nome", "O nome do baralho '$id' está vazio.")
        }
        if (baralhoJson.versao < 1) {
            violacoes += ViolacaoDeFormato(
                "versao",
                "O baralho '$id' tem versão ${baralhoJson.versao}; a versão mínima é 1.",
            )
        }
        val categoria = categoriaDe(baralhoJson.categoria, "categoria", id, violacoes)
        val estado = estadoDe(baralhoJson.estado, "estado", id, violacoes)
        val colecao = colecaoDe(baralhoJson.colecao, "colecao", id, violacoes)
        val cards = baralhoJson.cards.mapIndexedNotNull { indiceDoCard, cardJson ->
            validarCard(cardJson, categoria, caminho = "cards[$indiceDoCard]", violacoes)
        }
        if (violacoes.isNotEmpty()) return ResultadoDoParse.Falha(violacoes)

        val baralho = Baralho(
            id = id,
            nome = baralhoJson.nome,
            categoria = checkNotNull(categoria),
            colecao = checkNotNull(colecao),
            versao = baralhoJson.versao,
            estado = checkNotNull(estado),
            cards = cards,
        )
        return when (val resultado = validadorDeBaralho.validar(baralho)) {
            is ResultadoValidacaoDeBaralho.Aprovado -> ResultadoDoParse.Sucesso(baralho)
            is ResultadoValidacaoDeBaralho.Reprovado -> ResultadoDoParse.Falha(
                resultado.violacoes.map { ViolacaoDeFormato(caminho = "baralho", mensagem = it.mensagem) },
            )
        }
    }

    private fun validarEntrada(
        entrada: EntradaDoIndiceJson,
        caminho: String,
        violacoes: MutableList<ViolacaoDeFormato>,
    ): EntradaDoCatalogo? {
        val antes = violacoes.size
        val id = entrada.id.ifBlank {
            violacoes += ViolacaoDeFormato("$caminho.id", "A entrada $caminho tem id vazio.")
            "?"
        }
        if (entrada.nome.isBlank()) {
            violacoes += ViolacaoDeFormato("$caminho.nome", "O baralho '$id' tem nome vazio no índice.")
        }
        if (entrada.versao < 1) {
            violacoes += ViolacaoDeFormato(
                "$caminho.versao",
                "O baralho '$id' tem versão ${entrada.versao} no índice; a versão mínima é 1.",
            )
        }
        if (entrada.quantidadeDeCards < 1) {
            violacoes += ViolacaoDeFormato(
                "$caminho.quantidadeDeCards",
                "O baralho '$id' declara ${entrada.quantidadeDeCards} cards no índice.",
            )
        }
        if (entrada.url.isBlank()) {
            violacoes += ViolacaoDeFormato("$caminho.url", "O baralho '$id' está sem url no índice.")
        }
        val categoria = categoriaDe(entrada.categoria, "$caminho.categoria", id, violacoes)
        val estado = estadoDe(entrada.estado, "$caminho.estado", id, violacoes)
        val colecao = colecaoDe(entrada.colecao, "$caminho.colecao", id, violacoes)
        if (violacoes.size > antes) return null
        return EntradaDoCatalogo(
            id = id,
            nome = entrada.nome,
            categoria = checkNotNull(categoria),
            colecao = checkNotNull(colecao),
            versao = entrada.versao,
            estado = checkNotNull(estado),
            quantidadeDeCards = entrada.quantidadeDeCards,
            url = entrada.url,
            descricao = entrada.descricao,
            tamanhoEmBytes = entrada.tamanhoEmBytes,
        )
    }

    private fun colecaoDe(
        colecaoJson: ColecaoJson,
        caminho: String,
        id: String,
        violacoes: MutableList<ViolacaoDeFormato>,
    ): Colecao? {
        val antes = violacoes.size
        if (colecaoJson.id.isBlank()) {
            violacoes += ViolacaoDeFormato("$caminho.id", "O baralho '$id' tem coleção com id vazio.")
        }
        if (colecaoJson.nome.isBlank()) {
            violacoes += ViolacaoDeFormato("$caminho.nome", "O baralho '$id' tem coleção com nome vazio.")
        }
        if (colecaoJson.icone.isBlank()) {
            violacoes += ViolacaoDeFormato("$caminho.icone", "O baralho '$id' tem coleção sem ícone (emoji).")
        }
        if (violacoes.size > antes) return null
        return Colecao(id = colecaoJson.id, nome = colecaoJson.nome, icone = colecaoJson.icone)
    }

    private fun validarCard(
        cardJson: CardDoBaralhoJson,
        categoria: CardCategory?,
        caminho: String,
        violacoes: MutableList<ViolacaoDeFormato>,
    ): Card? {
        val antes = violacoes.size
        val id = cardJson.id.ifBlank {
            violacoes += ViolacaoDeFormato("$caminho.id", "O card $caminho tem id vazio.")
            "?"
        }
        val type = CardType.entries.firstOrNull { it.name == cardJson.type }
        if (type == null) {
            violacoes += ViolacaoDeFormato(
                "$caminho.type",
                "O card '$id' tem tipo desconhecido \"${cardJson.type}\"; " +
                    "os tipos válidos são ${CardType.entries.joinToString(", ")}.",
            )
        }
        if (cardJson.answer.isBlank()) {
            violacoes += ViolacaoDeFormato("$caminho.answer", "O card '$id' está com a resposta vazia.")
        }
        if (cardJson.clues.size != Card.QUANTIDADE_DE_DICAS) {
            violacoes += ViolacaoDeFormato(
                "$caminho.clues",
                "O card '$id' tem ${cardJson.clues.size} dicas; todo card tem exatamente " +
                    "${Card.QUANTIDADE_DE_DICAS}.",
            )
        }
        cardJson.clues.forEachIndexed { indiceDaDica, dica ->
            if (dica.isBlank()) {
                violacoes += ViolacaoDeFormato(
                    "$caminho.clues[$indiceDaDica]",
                    "A dica ${indiceDaDica + 1} do card '$id' está vazia.",
                )
            }
        }
        if (violacoes.size > antes || categoria == null) return null
        return Card(
            id = id,
            type = checkNotNull(type),
            category = categoria,
            answer = cardJson.answer,
            clues = cardJson.clues,
        )
    }

    private fun categoriaDe(
        valor: String,
        caminho: String,
        id: String,
        violacoes: MutableList<ViolacaoDeFormato>,
    ): CardCategory? {
        val categoria = CardCategory.entries.firstOrNull { it.name == valor }
        if (categoria == null) {
            violacoes += ViolacaoDeFormato(
                caminho,
                "O baralho '$id' tem categoria desconhecida \"$valor\"; " +
                    "as categorias válidas são ${CardCategory.entries.filter { it != CardCategory.LIVRE }.joinToString(", ")}.",
            )
        }
        return categoria
    }

    private fun estadoDe(
        valor: String,
        caminho: String,
        id: String,
        violacoes: MutableList<ViolacaoDeFormato>,
    ): EstadoDoBaralho? {
        val estado = EstadoDoBaralho.entries.firstOrNull { it.name == valor }
        if (estado == null) {
            violacoes += ViolacaoDeFormato(
                caminho,
                "O baralho '$id' tem estado desconhecido \"$valor\"; " +
                    "os estados válidos são ${EstadoDoBaralho.entries.joinToString(", ")}.",
            )
        }
        return estado
    }

    private fun falhaDeJson(excecao: Exception): ResultadoDoParse.Falha = ResultadoDoParse.Falha(
        listOf(
            ViolacaoDeFormato(
                caminho = "$",
                mensagem = "JSON inválido: ${excecao.message?.lineSequence()?.first() ?: "estrutura irreconhecível"}.",
            ),
        ),
    )
}

/**
 * Resultado de um parse do catálogo: sucesso com o valor convertido, ou falha
 * com todas as [ViolacaoDeFormato] acumuladas — legíveis, prontas para tela.
 */
sealed interface ResultadoDoParse<out T> {

    /** Estrutura válida; [valor] pronto para uso. */
    data class Sucesso<T>(val valor: T) : ResultadoDoParse<T>

    /** Estrutura inválida; [violacoes] nunca é vazia. */
    data class Falha(val violacoes: List<ViolacaoDeFormato>) : ResultadoDoParse<Nothing> {
        init {
            require(violacoes.isNotEmpty()) { "Falha exige ao menos uma violação." }
        }
    }
}

/**
 * Uma violação de formato encontrada num JSON do catálogo.
 *
 * @property caminho onde no documento (ex.: "cards[3].clues"); "$" = raiz.
 * @property mensagem descrição em português, legível para humanos.
 */
data class ViolacaoDeFormato(
    val caminho: String,
    val mensagem: String,
)
