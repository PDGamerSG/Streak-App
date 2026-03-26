package com.example.wallpaperapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallpaperapp.domain.DotGridGenerator
import com.example.wallpaperapp.domain.DotState
import com.example.wallpaperapp.domain.StreakResult
import com.example.wallpaperapp.ui.theme.DotStreakAccent
import kotlin.math.ceil

@Composable
fun HabitCard(
    name: String,
    color: String,
    dots: List<DotState>,
    streakResult: StreakResult,
    milestoneBadge: Int?,
    isInfinite: Boolean = false,
    isWeekly: Boolean = false,
    weeklyTarget: Int = 1,
    onEditStreak: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val habitColor = parseColor(color)
    val dotsPerRow = if (isWeekly) DotGridGenerator.WEEKLY_DOTS_PER_ROW else DotGridGenerator.DOTS_PER_ROW
    val dotRows = ceil(dots.size.toDouble() / dotsPerRow).toInt().coerceAtLeast(1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141414))
            .border(1.dp, Color(0xFF252525), RoundedCornerShape(12.dp))
    ) {
        // Left accent bar — habit color
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(habitColor)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Header: name (uppercase, letter-spaced) + edit/more icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = name.uppercase(),
                        color = Color(0xFFBBBBBB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                        maxLines = 1
                    )
                    if (milestoneBadge != null) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(habitColor.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$milestoneBadge%",
                                color = habitColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditStreak,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit streak",
                            tint = Color(0xFF484848),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = Color(0xFF484848),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Streak hero number + days-left pill on same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${streakResult.currentStreak}",
                        color = DotStreakAccent,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 40.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isWeekly) "WEEK\nSTREAK" else "DAY\nSTREAK",
                        color = Color(0xFF4A4A4A),
                        fontSize = 8.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 10.sp,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                // Pill: weekly target / days left / ongoing
                val pillBg: Color
                val pillFg: Color
                val pillText: String
                when {
                    isWeekly -> {
                        pillBg = Color(0xFF1A1030)
                        pillFg = Color(0xFF9B7FE8)
                        pillText = "×$weeklyTarget/WK"
                    }
                    isInfinite -> {
                        pillBg = Color(0xFF0D200D)
                        pillFg = Color(0xFF3DAA55)
                        pillText = "ONGOING"
                    }
                    else -> {
                        pillBg = Color(0xFF0D1828)
                        pillFg = Color(0xFF4A90D9)
                        pillText = "${streakResult.daysLeft}D LEFT"
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .clip(RoundedCornerShape(5.dp))
                        .background(pillBg)
                        .border(1.dp, pillFg.copy(alpha = 0.35f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = pillText,
                        color = pillFg,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Dot grid — use BoxWithConstraints to derive height from actual canvas width,
            // matching the spacing formula in DotGridCanvas exactly.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val spacing = maxWidth / (dotsPerRow + (dotsPerRow - 1) * 0.5f) * 1.5f
                val dotRadiusDp = spacing / 3f
                val canvasHeight = spacing * dotRows + dotRadiusDp
                DotGridCanvas(
                    dots = dots,
                    dotsPerRow = dotsPerRow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(canvasHeight)
                )
            }
        }
    }
}
