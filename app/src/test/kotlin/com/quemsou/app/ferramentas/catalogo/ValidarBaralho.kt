package com.quemsou.app.ferramentas.catalogo

import java.io.File
import kotlin.system.exitProcess

/**
 * Ponto de entrada da task Gradle `validarBaralho` (5B parte 1): valida um
 * único arquivo JSON de baralho do catálogo antes da revisão humana.
 *
 * Uso: `./gradlew validarBaralho -Parquivo=<caminho do JSON>`
 */
fun main(args: Array<String>) {
    val caminho = args.getOrNull(0)
        ?: falharComUso("Uso: ./gradlew validarBaralho -Parquivo=<caminho do JSON>")
    val resultado = validarArquivoDeBaralho(File(caminho))

    println("Validando baralho: ${resultado.caminho.path}")
    println()
    if (resultado.aprovado) {
        println("✓ ${resultado.baralho?.quantidadeDeCards ?: 0} card(s) válido(s). Baralho aprovado.")
        exitProcess(0)
    } else {
        println("✗ ${resultado.violacoes.size} violação(ões) encontrada(s):")
        resultado.violacoes.forEach { println("  - $it") }
        println()
        println("Baralho reprovado.")
        exitProcess(1)
    }
}
