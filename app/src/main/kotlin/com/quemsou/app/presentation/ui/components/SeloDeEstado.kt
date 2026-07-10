package com.quemsou.app.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quemsou.app.R
import com.quemsou.app.domain.model.EstadoDoBaralho
import com.quemsou.app.presentation.ui.theme.SeloCiano
import com.quemsou.app.presentation.ui.theme.SeloCianoClaro
import com.quemsou.app.presentation.ui.theme.SeloVerde
import com.quemsou.app.presentation.ui.theme.SeloVerdeClaro

/**
 * Selo do ciclo de vida de um baralho: "✓ EDIÇÃO FINAL" (verde) para
 * [EstadoDoBaralho.FINALIZADO] e "🧪 EM EVOLUÇÃO" (ciano) para
 * [EstadoDoBaralho.EM_DESENVOLVIMENTO]. Usado nos cards do catálogo e, em
 * [compacto], na seleção de baralhos do Setup.
 */
@Composable
fun SeloDeEstado(
    estado: EstadoDoBaralho,
    modifier: Modifier = Modifier,
    compacto: Boolean = false,
) {
    val escuro = isSystemInDarkTheme()
    val cor = when (estado) {
        EstadoDoBaralho.FINALIZADO -> if (escuro) SeloVerdeClaro else SeloVerde
        EstadoDoBaralho.EM_DESENVOLVIMENTO -> if (escuro) SeloCianoClaro else SeloCiano
    }
    val texto = when (estado) {
        EstadoDoBaralho.FINALIZADO -> stringResource(R.string.catalogo_selo_edicao_final)
        EstadoDoBaralho.EM_DESENVOLVIMENTO -> stringResource(R.string.catalogo_selo_em_evolucao)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = cor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, cor.copy(alpha = 0.6f)),
        modifier = modifier,
    ) {
        Text(
            text = texto,
            style = if (compacto) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = cor,
            modifier = Modifier.padding(
                horizontal = if (compacto) 6.dp else 10.dp,
                vertical = if (compacto) 2.dp else 4.dp,
            ),
        )
    }
}
