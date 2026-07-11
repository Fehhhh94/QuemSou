package com.quemsou.app.ferramentas.catalogo

import java.io.File
import kotlin.system.exitProcess

/**
 * Ponto de entrada da task Gradle `validarCatalogo` (5B parte 1): valida o
 * `indice.json`, todos os baralhos de `baralhos/` e a consistência cruzada
 * entre os dois — o check que teria pego o índice bumpado sem o arquivo do
 * baralho acompanhar (achado da validação física da 5A, ver `docs/BUGS.md`).
 *
 * Uso: `./gradlew validarCatalogo -Ppasta=<raiz do checkout do catálogo>`
 */
fun main(args: Array<String>) {
    val pasta = args.getOrNull(0)
        ?: falharComUso("Uso: ./gradlew validarCatalogo -Ppasta=<raiz do catálogo>")
    val raiz = File(pasta)
    if (!raiz.isDirectory) {
        falharComUso("Pasta não encontrada: ${raiz.path}")
    }

    val resultado = validarPastaDoCatalogo(raiz)
    println("Validando catálogo: ${raiz.path}")
    println()

    println("## Índice (indice.json)")
    if (resultado.violacoesDoIndice.isEmpty()) {
        println("✓ índice válido.")
    } else {
        println("✗ ${resultado.violacoesDoIndice.size} violação(ões):")
        resultado.violacoesDoIndice.forEach { println("  - $it") }
    }
    println()

    println("## Baralhos (baralhos/)")
    if (resultado.arquivos.isEmpty()) {
        println("(nenhum arquivo encontrado)")
    }
    resultado.arquivos.forEach { arquivo ->
        if (arquivo.aprovado) {
            println("✓ ${arquivo.caminho.name} — ${arquivo.baralho?.quantidadeDeCards ?: 0} card(s) válido(s).")
        } else {
            println("✗ ${arquivo.caminho.name} — ${arquivo.violacoes.size} violação(ões):")
            arquivo.violacoes.forEach { println("    - $it") }
        }
    }
    println()

    println("## Consistência cruzada índice ↔ baralhos")
    if (resultado.violacoesCruzadas.isEmpty()) {
        println("✓ nenhuma divergência.")
    } else {
        println("✗ ${resultado.violacoesCruzadas.size} violação(ões):")
        resultado.violacoesCruzadas.forEach { println("  - $it") }
    }
    println()

    val total = resultado.totalDeViolacoes
    if (total == 0) {
        println("RESUMO: catálogo aprovado.")
        exitProcess(0)
    } else {
        println("RESUMO: $total violação(ões) no catálogo.")
        exitProcess(1)
    }
}
