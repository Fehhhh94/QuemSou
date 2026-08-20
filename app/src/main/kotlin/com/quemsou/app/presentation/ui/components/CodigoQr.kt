package com.quemsou.app.presentation.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR gerado no aparelho, sem rede e sem serviço de imagem — o bitmap nasce
 * e morre na memória.
 *
 * Preto sobre branco **sempre**, mesmo no tema escuro: o QR é lido pela
 * câmera de outro celular, e contraste invertido ou de baixa razão derruba a
 * taxa de leitura. Por isso a moldura branca também é obrigatória (a "zona
 * silenciosa" do padrão).
 *
 * @param conteudo texto codificado — aqui, a URL do espelho de leitura.
 */
@Composable
fun CodigoQr(
    conteudo: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tamanho: Dp = 200.dp,
) {
    val lado = with(LocalDensity.current) { tamanho.roundToPx() }
    val bitmap = remember(conteudo, lado) { gerarQr(conteudo, lado) } ?: return

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(tamanho + MOLDURA * 2)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(MOLDURA),
    )
}

private val MOLDURA = 12.dp

/**
 * Codifica [conteudo] em um bitmap de [lado] px. Devolve `null` se o ZXing
 * recusar o conteúdo — a UI simplesmente não desenha o QR e o endereço em
 * texto continua servindo.
 */
private fun gerarQr(conteudo: String, lado: Int): ImageBitmap? {
    if (conteudo.isBlank() || lado <= 0) return null
    return runCatching {
        val matriz = QRCodeWriter().encode(
            conteudo,
            BarcodeFormat.QR_CODE,
            lado,
            lado,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                // A moldura branca é desenhada pelo Compose; sem isto o ZXing
                // gastaria pixels do bitmap com a borda e o QR sairia menor.
                EncodeHintType.MARGIN to 0,
                EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
            ),
        )
        val pixels = IntArray(matriz.width * matriz.height) { indice ->
            val x = indice % matriz.width
            val y = indice / matriz.width
            if (matriz.get(x, y)) PRETO else BRANCO
        }
        Bitmap.createBitmap(pixels, matriz.width, matriz.height, Bitmap.Config.ARGB_8888)
            .asImageBitmap()
    }.getOrNull()
}

private const val PRETO = 0xFF000000.toInt()
private const val BRANCO = 0xFFFFFFFF.toInt()
