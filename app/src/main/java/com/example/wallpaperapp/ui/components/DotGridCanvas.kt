package com.example.wallpaperapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.wallpaperapp.domain.DotGridGenerator
import com.example.wallpaperapp.domain.DotState
import kotlin.math.ceil

@Composable
fun DotGridCanvas(
    dots: List<DotState>,
    modifier: Modifier = Modifier,
    dotsPerRow: Int = DotGridGenerator.DOTS_PER_ROW
) {
    Canvas(modifier = modifier) {
        if (dots.isEmpty()) return@Canvas

        val spacing = size.width / (dotsPerRow + (dotsPerRow - 1) * 0.5f) * 1.5f
        val dotRadius = spacing / 3f

        dots.forEachIndexed { index, dot ->
            val col = index % dotsPerRow
            val row = index / dotsPerRow
            val cx = col * spacing + dotRadius
            val cy = row * spacing + dotRadius
            val center = Offset(cx, cy)
            val color = parseColor(dot.colorHex)

            if (dot.isVisible) {
                // Glow rings for today's dot
                if (dot.isToday) {
                    drawCircle(color = color.copy(alpha = 0.07f), radius = dotRadius * 3.2f, center = center)
                    drawCircle(color = color.copy(alpha = 0.18f), radius = dotRadius * 1.9f, center = center)
                }
                drawCircle(color = color, radius = dotRadius, center = center)
            }
        }
    }
}

fun parseColor(hex: String): Color {
    return try {
        val clean = hex.trimStart('#')
        val value = clean.toLong(16)
        val argb = if (clean.length == 6) (0xFF000000L or value) else value
        Color(argb.toInt())
    } catch (e: Exception) {
        Color(0xFF3A3A3A)
    }
}

/** Calculates the required height in px for the canvas given dot count. */
fun dotGridHeightPx(dotCount: Int, widthPx: Float, dotsPerRow: Int = DotGridGenerator.DOTS_PER_ROW): Float {
    if (dotCount == 0 || widthPx == 0f) return 0f
    val spacing = widthPx / (dotsPerRow + (dotsPerRow - 1) * 0.5f) * 1.5f
    val dotRadius = spacing / 3f
    val rows = ceil(dotCount.toDouble() / dotsPerRow).toInt()
    return rows * spacing + dotRadius
}
