package com.example.wallpaperapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallpaperapp.domain.DotState
import com.example.wallpaperapp.domain.StreakResult
import com.example.wallpaperapp.ui.theme.DotStreakCard
import com.example.wallpaperapp.ui.theme.DotStreakSecondaryText

@Composable
fun HabitCard(
    name: String,
    color: String,
    dots: List<DotState>,
    streakResult: StreakResult,
    milestoneBadge: Int?,
    onEditStreak: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val habitColor = parseColor(color)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DotStreakCard)
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            // Header row: color dot + name + milestone badge + streak
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(habitColor)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (milestoneBadge != null) {
                        Spacer(Modifier.size(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(habitColor.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$milestoneBadge%",
                                color = habitColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\uD83D\uDD25 ${streakResult.currentStreak} day streak",
                        color = DotStreakSecondaryText,
                        fontSize = 12.sp
                    )
                    IconButton(
                        onClick = onEditStreak,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit streak",
                            tint = DotStreakSecondaryText,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Dot grid preview — height grows with number of rows
            val dotRows = kotlin.math.ceil(dots.size.toDouble() / com.example.wallpaperapp.domain.DotGridGenerator.DOTS_PER_ROW)
                .toInt().coerceAtLeast(1)
            DotGridCanvas(
                dots = dots,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((dotRows * 22).dp)
            )

            Spacer(Modifier.height(6.dp))

            // Stats row
            Text(
                text = "${streakResult.daysLeft}d left",
                color = habitColor,
                fontSize = 12.sp
            )
        }
    }
}
