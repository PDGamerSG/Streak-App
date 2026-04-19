package com.example.wallpaperapp.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallpaperapp.domain.DotGridGenerator
import com.example.wallpaperapp.ui.components.DotGridCanvas
import com.example.wallpaperapp.ui.theme.DotStreakAccent
import com.example.wallpaperapp.ui.theme.DotStreakBackground
import com.example.wallpaperapp.ui.theme.DotStreakSecondaryText
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPreviewScreen(
    viewModel: WallpaperPreviewViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadWallpaperState(context) }

    LaunchedEffect(uiState.exportSuccess, uiState.exportError) {
        when {
            uiState.exportSuccess -> {
                val msg = if (uiState.isWallpaperEnabled) "Wallpaper enabled ✓" else "Wallpaper disabled ✓"
                snackbarHostState.showSnackbar(msg)
                viewModel.clearExportStatus()
            }
            uiState.exportError != null -> {
                snackbarHostState.showSnackbar("Error: ${uiState.exportError}")
                viewModel.clearExportStatus()
            }
        }
    }

    Scaffold(
        containerColor = DotStreakBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "PREVIEW",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 2.8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Lock screen",
                            color = Color(0xFF555555),
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF888888))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DotStreakBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DotStreakBackground)
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Phone frame ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                PhoneMockup(uiState = uiState)
            }

            // ── Action buttons ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A0A))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val enabled = uiState.isWallpaperEnabled
                Button(
                    onClick = {
                        if (enabled) viewModel.disableWallpaper(context)
                        else viewModel.setAsWallpaper(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isExporting && (enabled || uiState.habits.isNotEmpty()),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (enabled) Color(0xFF2A2A2A) else DotStreakAccent
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (enabled) "DISABLE WALLPAPER" else "ENABLE WALLPAPER",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 13.sp
                        )
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.saveToGallery(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isExporting && uiState.habits.isNotEmpty(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF888888)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save to Gallery", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PhoneMockup(uiState: PreviewUiState) {
    val today = remember { LocalDate.now() }
    val mockTime = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
    val mockDate = remember { today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 19.5f)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF0A0A0A))
            .border(2.dp, Color(0xFF2A2A2A), RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status bar + notch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 7.dp)
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(Color(0xFF252525))
                )
            }

            Spacer(Modifier.height(14.dp))

            // Time
            Text(
                text = mockTime,
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Thin,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            // Date
            Text(
                text = mockDate,
                color = Color(0xFF999999),
                fontSize = 10.sp,
                letterSpacing = 0.3.sp
            )

            Spacer(Modifier.height(12.dp))

            // Thin divider
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(0.5.dp)
                    .background(Color(0xFF2A2A2A))
            )

            Spacer(Modifier.height(10.dp))

            // Habits content — scrollable inside the phone
            if (uiState.isLoading) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = DotStreakAccent,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (uiState.habits.isEmpty()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "Add habits to\npreview wallpaper",
                        color = Color(0xFF444444),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    uiState.habits.forEach { habitWithDots ->
                        val habit = habitWithDots.habit
                        val dotsPerRow = DotGridGenerator.WALLPAPER_DOTS_PER_ROW

                        val daysLeft = YearMonth.of(today.year, today.month).lengthOfMonth() - today.dayOfMonth

                        // Habit name
                        Text(
                            text = habit.name.uppercase(),
                            color = Color(0xFFCCCCCC),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.4.sp
                        )
                        Spacer(Modifier.height(3.dp))

                        // Streak number
                        Text(
                            text = "${habitWithDots.streak}",
                            color = DotStreakAccent,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )

                        // Streak label
                        Text(
                            text = when {
                                habit.isMonthly -> "DAYS THIS MONTH"
                                habit.isWeekly  -> "WEEK STREAK"
                                else            -> "DAY STREAK"
                            },
                            color = Color(0xFF555555),
                            fontSize = 7.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        // Dot grid — height derived from spacing formula so dots aren't clipped
                        val rows = kotlin.math.ceil(
                            habitWithDots.dots.size.toDouble() / dotsPerRow
                        ).toInt().coerceAtLeast(1)
                        // width/height = (dotsPerRow + (dotsPerRow-1)*0.5) / (1.5 * (rows + 1/3))
                        val canvasRatio = (dotsPerRow + (dotsPerRow - 1) * 0.5f) /
                                          (1.5f * (rows + 1f / 3f))
                        DotGridCanvas(
                            dots = habitWithDots.dots,
                            dotsPerRow = dotsPerRow,
                            showGlow = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(canvasRatio)
                        )
                        Spacer(Modifier.height(6.dp))

                        // Footer: days left or ongoing
                        val footerText = when {
                            habit.isMonthly  -> "COUNT"
                            habit.isWeekly   -> "×${habit.weeklyTarget}/WEEK"
                            habit.isInfinite -> "ONGOING"
                            else             -> "$daysLeft DAYS LEFT"
                        }
                        val footerColor = when {
                            habit.isMonthly  -> Color(0xFF4ECDC4)
                            habit.isWeekly   -> Color(0xFF9B7FE8)
                            habit.isInfinite -> Color(0xFF3DAA55)
                            else             -> Color(0xFF555555)
                        }
                        Text(
                            text = footerText,
                            color = footerColor,
                            fontSize = 7.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )

                        Spacer(Modifier.height(14.dp))
                    }
                }
            }

            // Home indicator
            Box(
                modifier = Modifier.padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFF333333))
                )
            }
        }
    }
}
