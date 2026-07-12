package com.quemsou.app.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta provisória da Fase 0 — cores definitivas do jogo virão numa fase futura.
val QuemSouPrimary = Color(0xFFBB86FC)
val QuemSouOnPrimary = Color(0xFF000000)
val QuemSouSecondary = Color(0xFF03DAC6)
val QuemSouBackground = Color(0xFF121212)
val QuemSouSurface = Color(0xFF1E1E1E)
val QuemSouOnBackground = Color(0xFFE1E1E1)
val QuemSouOnSurface = Color(0xFFE1E1E1)

// Contrapartida clara da mesma paleta provisória (3.3 — suporte a tema claro).
val QuemSouPrimaryLight = Color(0xFF6650A4)
val QuemSouOnPrimaryLight = Color(0xFFFFFFFF)
val QuemSouBackgroundLight = Color(0xFFFFFBFE)
val QuemSouSurfaceLight = Color(0xFFF3EDF7)
val QuemSouOnBackgroundLight = Color(0xFF1C1B1F)
val QuemSouOnSurfaceLight = Color(0xFF1C1B1F)

// Paleta âmbar/dourada do Modo Shot (overlay e card do Setup) — dentro da
// PARTIDA é exclusiva do modo: nada de âmbar no grid nem nas demais fases.
val ShotAmbar = Color(0xFFFFB300)
val ShotAmbarEscuro = Color(0xFF8F6400) // variante do tema claro (contraste sobre fundo claro)
val ShotOnAmbar = Color(0xFF261A00) // texto sobre o botão âmbar, em ambos os temas

// Catálogo (mockup v2 da 5A): âmbar sinaliza NOVIDADE (pontinho e botão
// Atualizar); verde/ciano são os selos do ciclo de vida do baralho.
val NovidadeAmbar = Color(0xFFFFB300)
val NovidadeAmbarEscuro = Color(0xFF8F6400) // variante do tema claro
val SeloVerde = Color(0xFF2E7D32)
val SeloVerdeClaro = Color(0xFF81C784) // variante do tema escuro
val SeloCiano = Color(0xFF00838F)
val SeloCianoClaro = Color(0xFF4DD0E1) // variante do tema escuro

// Modo dev de feedback (5B parte 2): identidade "andaime" violeta do widget
// no Anúncio e do item de export na Home — NUNCA âmbar (exclusivo do Modo
// Shot dentro da partida). Borda tracejada e fundo derivam do acento com
// alpha (0.45/0.10, mockup-feedback-anuncio-v1).
val DevVioleta = Color(0xFF9A86E8)
val DevVioletaEscuro = Color(0xFF5F4BB6) // variante do tema claro (contraste sobre fundo claro)
