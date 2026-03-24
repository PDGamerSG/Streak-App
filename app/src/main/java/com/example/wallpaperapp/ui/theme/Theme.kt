package com.example.wallpaperapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DotStreakColorScheme = darkColorScheme(
    primary = DotStreakAccent,
    background = DotStreakBackground,
    surface = DotStreakSurface,
    onBackground = DotStreakOnBackground,
    onSurface = DotStreakOnBackground,
    secondary = DotStreakSecondaryText
)

@Composable
fun WallpaperappTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DotStreakColorScheme,
        typography = Typography,
        content = content
    )
}
