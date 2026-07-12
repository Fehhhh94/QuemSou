package com.quemsou.app.presentation.game

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quemsou.app.R
import com.quemsou.app.data.feedback.VotoDeCard
import com.quemsou.app.presentation.ui.theme.DevVioleta
import com.quemsou.app.presentation.ui.theme.DevVioletaEscuro
import com.quemsou.app.presentation.ui.theme.SeloVerde

/**
 * Widget dev de feedback do Anúncio (modo dev de feedback, 5B parte 2) —
 * mockup-feedback-anuncio-v1. Identidade "andaime": borda TRACEJADA violeta
 * com fundo translúcido e selo "DEV" — deliberadamente diferente de tudo que
 * o jogador vê, e **nunca âmbar** (exclusivo do Modo Shot na partida).
 *
 * Avaliar é opcional e nada aqui bloqueia o ritmo da mesa: os chips apenas
 * marcam o estado no ViewModel; quem grava (com voto) e avança é o
 * "Continuar" do Anúncio. O link de comentar só existe após um voto —
 * comentário sem veredito não existe.
 */
@Composable
internal fun FeedbackDevWidget(
    estado: FeedbackDevUiState,
    onVotar: (VotoDeCard) -> Unit,
    onComentar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val acento = if (isSystemInDarkTheme()) DevVioleta else DevVioletaEscuro
    var comentarioAberto by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DevVioleta.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    color = DevVioleta.copy(alpha = 0.45f),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                        ),
                    ),
                )
            }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.partida_feedback_selo),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = acento,
                modifier = Modifier
                    .background(DevVioleta.copy(alpha = 0.20f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Text(
                text = stringResource(R.string.partida_feedback_pergunta),
                style = MaterialTheme.typography.labelLarge,
                color = acento,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = estado.voto == VotoDeCard.BOM,
                onClick = { onVotar(VotoDeCard.BOM) },
                label = { Text(stringResource(R.string.partida_feedback_bom)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SeloVerde,
                    selectedLabelColor = Color.White,
                ),
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = estado.voto == VotoDeCard.FRACO,
                onClick = { onVotar(VotoDeCard.FRACO) },
                label = { Text(stringResource(R.string.partida_feedback_fraco)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.weight(1f),
            )
        }
        if (estado.voto != null) {
            if (comentarioAberto) {
                OutlinedTextField(
                    value = estado.comentario,
                    onValueChange = onComentar,
                    placeholder = {
                        Text(stringResource(R.string.partida_feedback_comentario_placeholder))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TextButton(onClick = { comentarioAberto = true }) {
                    Text(
                        text = stringResource(R.string.partida_feedback_comentar),
                        color = acento,
                    )
                }
            }
        }
    }
}
