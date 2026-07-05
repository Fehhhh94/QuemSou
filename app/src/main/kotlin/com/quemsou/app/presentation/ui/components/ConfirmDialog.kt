package com.quemsou.app.presentation.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Diálogo de confirmação genérico para ações destrutivas (queimar card,
 * abandonar partida) — nenhuma ação assim acontece sem essa confirmação.
 */
@Composable
fun ConfirmDialog(
    titulo: String,
    texto: String,
    textoConfirmar: String,
    textoCancelar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(textoConfirmar) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(textoCancelar) }
        },
    )
}
