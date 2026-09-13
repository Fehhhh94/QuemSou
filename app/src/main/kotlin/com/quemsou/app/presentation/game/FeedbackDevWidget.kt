package com.quemsou.app.presentation.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quemsou.app.R
import com.quemsou.app.data.feedback.VotoDeCard

/** Avaliação opcional após a resposta; o nome interno é mantido por compatibilidade. */
@Composable
internal fun FeedbackDevWidget(
    estado: FeedbackDevUiState,
    onVotar: (VotoDeCard) -> Unit,
    onComentar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var comentarioAberto by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.partida_feedback_pergunta), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = estado.voto == VotoDeCard.BOM, onClick = { onVotar(VotoDeCard.BOM) },
                    label = { Text(stringResource(R.string.partida_feedback_bom)) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                FilterChip(selected = estado.voto == VotoDeCard.FRACO, onClick = { onVotar(VotoDeCard.FRACO) },
                    label = { Text(stringResource(R.string.partida_feedback_fraco)) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
            }
            if (estado.voto != null) {
                if (comentarioAberto) {
                    OutlinedTextField(value = estado.comentario, onValueChange = { onComentar(it.take(1500)) },
                        label = { Text(stringResource(R.string.fabrica_comentario)) },
                        supportingText = { Text(stringResource(R.string.partida_feedback_comentario_placeholder)) },
                        modifier = Modifier.fillMaxWidth())
                } else {
                    TextButton(onClick = { comentarioAberto = true }) { Text(stringResource(R.string.fabrica_comentar)) }
                }
            }
        }
    }
}
