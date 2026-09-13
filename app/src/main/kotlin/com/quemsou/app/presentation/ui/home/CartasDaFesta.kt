package com.quemsou.app.presentation.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.quemsou.app.presentation.ui.theme.FestaCoral
import com.quemsou.app.presentation.ui.theme.FestaLima
import com.quemsou.app.presentation.ui.theme.FestaTinta

/** Arte vetorial decorativa: acompanha a densidade da tela e funciona offline. */
@Composable
internal fun CartasDaFesta(modifier: Modifier = Modifier) {
    Box(modifier.height(124.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(FestaLima, 5.dp.toPx(), Offset(size.width * .14f, size.height * .25f))
            drawCircle(FestaCoral, 4.dp.toPx(), Offset(size.width * .88f, size.height * .75f))
            drawLine(Color.White.copy(alpha = .65f), Offset(size.width * .12f, size.height * .70f), Offset(size.width * .17f, size.height * .79f), 3.dp.toPx())
            drawLine(FestaLima, Offset(size.width * .81f, size.height * .16f), Offset(size.width * .85f, size.height * .08f), 3.dp.toPx())
        }
        Surface(Modifier.size(84.dp, 100.dp).rotate(-22f), shape = RoundedCornerShape(18.dp), color = FestaCoral) { }
        Surface(Modifier.size(84.dp, 100.dp).rotate(19f), shape = RoundedCornerShape(18.dp), color = FestaLima) { }
        Surface(Modifier.size(78.dp, 104.dp).rotate(-5f), shape = RoundedCornerShape(18.dp), color = Color(0xFFFFFDF7), shadowElevation = 4.dp) {
            Box(contentAlignment = Alignment.Center) {
                Text("?", style = MaterialTheme.typography.displaySmall, color = FestaTinta)
            }
        }
    }
}
