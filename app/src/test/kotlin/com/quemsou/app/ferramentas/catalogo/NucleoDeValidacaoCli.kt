package com.quemsou.app.ferramentas.catalogo

import com.quemsou.app.data.catalogo.EntradaDoCatalogo
import com.quemsou.app.data.catalogo.ParserDoCatalogo
import com.quemsou.app.data.catalogo.ResultadoDoParse
import com.quemsou.app.domain.model.Baralho
import com.quemsou.app.domain.validacao.ResultadoValidacao
import com.quemsou.app.domain.validacao.ValidadorEditorial
import java.io.File
import kotlin.system.exitProcess

/**
 * Núcleo reusado pelas duas ferramentas de linha de comando da fábrica (5B
 * parte 1: `validarBaralho` e `validarCatalogo`). Vive no sourceSet de teste
 * (não em `main`) de propósito: reusa o classpath de `testDebugUnitTest`
 * (JVM pura — domínio + kotlinx.serialization, sem Android em tempo de
 * execução) sem nunca ir parar no APK. Nenhuma regra é duplicada — tudo
 * delega a [ParserDoCatalogo] (estrutura + [com.quemsou.app.domain.validacao.ValidadorDeBaralho],
 * já fundidos) e a [ValidadorEditorial], os mesmos usados pelo app e pelos
 * testes existentes.
 */

/** Resultado da validação de um único arquivo de baralho. */
data class ResultadoDoArquivoDeBaralho(
    val caminho: File,
    val baralho: Baralho?,
    val violacoes: List<String>,
) {
    val aprovado: Boolean get() = violacoes.isEmpty()
}

/**
 * Valida [caminho] como um JSON de baralho do catálogo: estrutura + regras
 * de conjunto (via [ParserDoCatalogo.parseBaralho]) e, só se a estrutura for
 * válida, as regras editoriais por card ([ValidadorEditorial]) — a única
 * regra que o parser não cobre (resposta/dica vazias já são violação
 * estrutural antes de chegar aqui).
 */
fun validarArquivoDeBaralho(caminho: File): ResultadoDoArquivoDeBaralho {
    if (!caminho.isFile) {
        return ResultadoDoArquivoDeBaralho(
            caminho = caminho,
            baralho = null,
            violacoes = listOf("Arquivo não encontrado: ${caminho.path}"),
        )
    }
    return when (val resultado = ParserDoCatalogo().parseBaralho(caminho.readText())) {
        is ResultadoDoParse.Falha -> ResultadoDoArquivoDeBaralho(
            caminho = caminho,
            baralho = null,
            violacoes = resultado.violacoes.map { "${it.caminho}: ${it.mensagem}" },
        )
        is ResultadoDoParse.Sucesso -> ResultadoDoArquivoDeBaralho(
            caminho = caminho,
            baralho = resultado.valor,
            violacoes = resultado.valor.cards.flatMap { card ->
                when (val editorial = ValidadorEditorial().validar(card)) {
                    is ResultadoValidacao.Aprovado -> emptyList()
                    is ResultadoValidacao.Reprovado -> editorial.violacoes.map {
                        "card '${card.id}': ${it.mensagem}"
                    }
                }
            },
        )
    }
}

/** Resultado da validação do catálogo inteiro (índice + baralhos + cruzamento). */
data class ResultadoDoCatalogo(
    val violacoesDoIndice: List<String>,
    val arquivos: List<ResultadoDoArquivoDeBaralho>,
    val violacoesCruzadas: List<String>,
) {
    val totalDeViolacoes: Int
        get() = violacoesDoIndice.size + arquivos.sumOf { it.violacoes.size } + violacoesCruzadas.size
}

/**
 * Valida a pasta [raiz] de um checkout do catálogo (`indice.json` na raiz +
 * arquivos `.json` dentro de `baralhos`, formato de `docs/CATALOG_FORMAT.md`):
 * o índice e cada baralho isoladamente (mesma disciplina de [validarArquivoDeBaralho]) e a
 * consistência cruzada entre os dois — versão do índice == versão do
 * arquivo, quantidade declarada == quantidade real, todo id do índice tem
 * arquivo `<id>.json` (convenção já usada no repositório real) e vice-versa.
 * Esta última checagem é a que teria pego o índice bumpado sem o arquivo do
 * baralho acompanhar (achado da validação física da 5A).
 */
fun validarPastaDoCatalogo(raiz: File): ResultadoDoCatalogo {
    val arquivoDoIndice = File(raiz, "indice.json")
    val violacoesDoIndice = mutableListOf<String>()
    val entradas: List<EntradaDoCatalogo> = if (!arquivoDoIndice.isFile) {
        violacoesDoIndice += "Arquivo não encontrado: ${arquivoDoIndice.path}"
        emptyList()
    } else {
        when (val resultado = ParserDoCatalogo().parseIndice(arquivoDoIndice.readText())) {
            is ResultadoDoParse.Falha -> {
                violacoesDoIndice += resultado.violacoes.map { "${it.caminho}: ${it.mensagem}" }
                emptyList()
            }
            is ResultadoDoParse.Sucesso -> resultado.valor
        }
    }

    val pastaDeBaralhos = File(raiz, "baralhos")
    val arquivosDeBaralho = pastaDeBaralhos.listFiles { arquivo -> arquivo.extension == "json" }
        ?.sortedBy { it.name }
        .orEmpty()
    val resultados = arquivosDeBaralho.map { validarArquivoDeBaralho(it) }

    val violacoesCruzadas = mutableListOf<String>()
    val resultadosPorNomeBase = resultados.associateBy { it.caminho.nameWithoutExtension }
    entradas.forEach { entrada ->
        val resultado = resultadosPorNomeBase[entrada.id]
        if (resultado == null) {
            violacoesCruzadas += "baralho '${entrada.id}': o índice aponta para " +
                "'${entrada.id}.json', mas o arquivo não existe em baralhos/."
            return@forEach
        }
        val baralho = resultado.baralho ?: return@forEach // violação estrutural já reportada no arquivo
        if (baralho.versao != entrada.versao) {
            violacoesCruzadas += "baralho '${entrada.id}': versão ${entrada.versao} no índice, " +
                "mas ${baralho.versao} no arquivo."
        }
        if (baralho.quantidadeDeCards != entrada.quantidadeDeCards) {
            violacoesCruzadas += "baralho '${entrada.id}': índice declara " +
                "${entrada.quantidadeDeCards} card(s), mas o arquivo tem ${baralho.quantidadeDeCards}."
        }
        if (baralho.id != entrada.id) {
            violacoesCruzadas += "arquivo '${entrada.id}.json': declara id '${baralho.id}', " +
                "divergente do id '${entrada.id}' no índice."
        }
    }
    val idsDoIndice = entradas.map { it.id }.toSet()
    resultados.forEach { resultado ->
        val nomeBase = resultado.caminho.nameWithoutExtension
        if (nomeBase !in idsDoIndice) {
            violacoesCruzadas += "arquivo '${resultado.caminho.name}': não tem entrada " +
                "correspondente no índice (esperado id '$nomeBase')."
        }
    }

    return ResultadoDoCatalogo(violacoesDoIndice, resultados, violacoesCruzadas)
}

/** Imprime [mensagem] em stderr e encerra o processo com exit code 2 (uso incorreto da ferramenta). */
fun falharComUso(mensagem: String): Nothing {
    System.err.println(mensagem)
    exitProcess(2)
}
