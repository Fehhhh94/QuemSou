package com.quemsou.app.domain.rules

import com.quemsou.app.domain.model.Card
import com.quemsou.app.domain.model.DicaDoBanco
import java.text.Normalizer
import java.util.Locale

/** Identidade textual complementar ao id editorial: pontuação não torna uma dica inédita. */
object SelecionadorDeDicas {
    private val marcas = Regex("\\p{M}+")
    private val separadores = Regex("[^\\p{L}\\p{N}]+")
    fun normalizar(texto: String): String = Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(marcas, "").lowercase(Locale.ROOT)
        .replace(separadores, " ").trim()

    fun resposta(card: Card): String = card.respostaId.ifBlank { normalizar(card.answer) }

    fun banco(card: Card): List<DicaDoBanco> = card.bancoDeDicas.ifEmpty {
        card.clues.map { DicaDoBanco("legado:${normalizar(it)}", it) }
    }

    /** As duas chaves impedem repetir o id com texto editado ou o texto com id trocado. */
    fun chaves(card: Card, dica: DicaDoBanco): Set<String> {
        val texto = normalizar(dica.texto)
        return AcervoDeRespostas.chaves(card).flatMap { chave ->
            if (chave.startsWith("id:")) listOf(
                "$chave:${dica.id}", "canonico:${chave.removePrefix("id:")}:$texto",
            ) else listOf("texto:${chave.removePrefix("nome:")}:$texto")
        }.toSet()
    }

    fun disponiveis(card: Card, usadas: Set<String>): List<DicaDoBanco> = banco(card)
        .distinctBy { normalizar(it.texto) }
        .filter { chaves(card, it).none(usadas::contains) }

    fun preparar(card: Card, usadas: Set<String>, seed: Long): Card? {
        val livres = disponiveis(card, usadas).sortedBy { it.id }
        if (livres.size < Card.QUANTIDADE_DE_DICAS) return null
        val escolhidas = EmbaralhadorDeCards.embaralhar(livres, seed).take(Card.QUANTIDADE_DE_DICAS)
        return card.copy(clues = escolhidas.map { it.texto }, bancoDeDicas = escolhidas)
    }
}
