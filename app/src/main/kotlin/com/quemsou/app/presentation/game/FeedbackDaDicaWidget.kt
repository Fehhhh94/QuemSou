package com.quemsou.app.presentation.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quemsou.app.R
import com.quemsou.app.data.feedback.VotoDeCard

/** Convite após cada dica, sem abrir diálogo automaticamente nem exigir avaliação. */
@Composable
internal fun FeedbackDaDicaWidget(
    estado: FeedbackDaDicaUiState,
    onAvaliar: (String, VotoDeCard, String) -> Unit,
) {
    var aberto by rememberSaveable(estado.chave) { mutableStateOf(false) }
    var comentario by rememberSaveable(estado.chave, estado.comentario) { mutableStateOf(estado.comentario) }
    TextButton(onClick = { aberto = true }, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(if (estado.salvo) R.string.dica_feedback_editar else R.string.dica_feedback_convite))
    }
    if (estado.erro) Text(stringResource(R.string.dica_feedback_erro), style = MaterialTheme.typography.bodySmall)
    if (aberto) AlertDialog(
        onDismissRequest = { aberto = false },
        title = { Text(stringResource(R.string.dica_feedback_titulo)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.dica_feedback_explicacao))
                for (voto in VotoDeCard.entries) {
                    FilterChip(
                        selected = estado.voto == voto,
                        onClick = { onAvaliar(estado.chave, voto, comentario) },
                        enabled = !estado.carregando && !estado.salvando,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        label = { Text(stringResource(if (voto == VotoDeCard.BOM) R.string.dica_feedback_boa else R.string.dica_feedback_melhorar)) },
                    )
                }
                OutlinedTextField(
                    value = comentario, onValueChange = { comentario = it.take(1000) },
                    label = { Text(stringResource(R.string.dica_feedback_comentario)) },
                    modifier = Modifier.fillMaxWidth(), maxLines = 4,
                )
                if (estado.carregando || estado.salvando) Text(stringResource(R.string.dica_feedback_aguarde))
                if (estado.salvo) Text(stringResource(R.string.dica_feedback_salvo))
                if (estado.erro) Text(stringResource(R.string.dica_feedback_erro), color = MaterialTheme.colorScheme.error)
                if (estado.voto != null && comentario != estado.comentario) Text(stringResource(R.string.dica_feedback_reenviar))
            }
        },
        confirmButton = { TextButton(onClick = { aberto = false }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.dica_feedback_voltar))
        } },
    )
}
