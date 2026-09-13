package com.quemsou.app.presentation.ui.components

import androidx.compose.foundation.BorderStroke
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
import com.quemsou.app.domain.model.CardType

/**
 * Chip "Sou um LUGAR" / "Sou uma PESSOA" / "Sou uma COISA", conforme [tipo].
 *
 * É etiqueta, não ação: um `AssistChip` com `onClick` vazio se anunciava como
 * botão para a acessibilidade e convidava a um toque que não faz nada.
 */
@Composable
fun ChipTipoDeCard(tipo: CardType, modifier: Modifier = Modifier) {
    val textoId = when (tipo) {
        CardType.PESSOA -> R.string.partida_tipo_pessoa
        CardType.LUGAR -> R.string.partida_tipo_lugar
        CardType.COISA -> R.string.partida_tipo_coisa
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = stringResource(textoId),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
