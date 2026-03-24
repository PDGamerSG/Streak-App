package com.example.wallpaperapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallpaperapp.ui.theme.DotStreakAccent
import kotlinx.coroutines.delay
import kotlin.random.Random

private data class Particle(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val color: Color,
    val radius: Float
)

private val CONFETTI_COLORS = listOf(
    Color(0xFFE74C3C), Color(0xFFF39C12), Color(0xFF27AE60),
    Color(0xFF4A90D9), Color(0xFF8E44AD), Color(0xFFFFFFFF)
)

@Composable
fun MilestoneOverlay(pct: Int, onDismiss: () -> Unit) {
    val milestoneMessage = when (pct) {
        25  -> "You're a quarter of the way there! \uD83D\uDCAA"
        50  -> "Halfway there! Keep it up! \uD83D\uDE80"
        75  -> "Three quarters done! Almost there! \uD83C\uDF1F"
        100 -> "You did it! Habit complete! \uD83C\uDF89"
        else -> "Milestone reached! $pct%"
    }

    val particles = remember {
        List(80) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.4f,
                vx = (Random.nextFloat() - 0.5f) * 0.3f,
                vy = Random.nextFloat() * 0.4f + 0.1f,
                color = CONFETTI_COLORS[Random.nextInt(CONFETTI_COLORS.size)],
                radius = Random.nextFloat() * 8f + 4f
            )
        }
    }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(2500, easing = LinearEasing))
        delay(800)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        // Confetti canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val t = progress.value
                val cx = (p.x + p.vx * t) * size.width
                val cy = (p.y + p.vy * t) * size.height
                drawCircle(
                    color = p.color.copy(alpha = (1f - t * 0.7f).coerceAtLeast(0f)),
                    radius = p.radius,
                    center = Offset(cx, cy)
                )
            }
        }

        // Message card
        Column(
            modifier = Modifier
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$pct%",
                color = DotStreakAccent,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = milestoneMessage,
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss) {
                Text("Continue", color = DotStreakAccent)
            }
        }
    }
}
