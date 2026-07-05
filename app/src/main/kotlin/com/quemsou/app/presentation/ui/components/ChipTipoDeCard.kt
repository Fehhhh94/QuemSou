package com.quemsou.app.presentation.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quemsou.app.R
import com.quemsou.app.domain.model.CardType

/** Chip "Sou um LUGAR" / "Sou uma PESSOA" / "Sou uma COISA", conforme [tipo]. */
@Composable
fun ChipTipoDeCard(tipo: CardType, modifier: Modifier = Modifier) {
    val textoId = when (tipo) {
        CardType.PESSOA -> R.string.partida_tipo_pessoa
        CardType.LUGAR -> R.string.partida_tipo_lugar
        CardType.COISA -> R.string.partida_tipo_coisa
    }
    AssistChip(
        onClick = {},
        label = { Text(stringResource(textoId)) },
        modifier = modifier,
    )
}
