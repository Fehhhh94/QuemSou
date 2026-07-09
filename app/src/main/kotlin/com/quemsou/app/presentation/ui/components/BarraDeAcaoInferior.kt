package com.quemsou.app.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Container padrão para uma barra de ação inferior fora do `content` de um
 * [androidx.compose.material3.Scaffold] (ex.: `bottomBar`). Diferente do
 * `content`, o slot `bottomBar` do Scaffold não recebe padding de
 * `WindowInsets.systemBars` automaticamente — em telas edge-to-edge
 * (`enableEdgeToEdge` na `MainActivity`) isso deixa o conteúdo por baixo da
 * barra de navegação do sistema. Este componente aplica
 * [Modifier.navigationBarsPadding] antes do padding visual para evitar isso.
 */
@Composable
fun BarraDeAcaoInferior(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(24.dp),
        content = content,
    )
}
