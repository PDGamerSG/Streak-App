package com.example.wallpaperapp.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallpaperapp.ui.components.DotGridCanvas
import com.example.wallpaperapp.ui.components.parseColor
import com.example.wallpaperapp.ui.theme.DotStreakAccent
import com.example.wallpaperapp.ui.theme.DotStreakBackground
import com.example.wallpaperapp.ui.theme.DotStreakSecondaryText
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPreviewScreen(
    viewModel: WallpaperPreviewViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar feedback
    LaunchedEffect(uiState.exportSuccess, uiState.exportError) {
        when {
            uiState.exportSuccess -> {
                snackbarHostState.showSnackbar("Wallpaper saved to gallery ✓")
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
                title = { Text("Wallpaper Preview", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
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
                .padding(innerPadding)
        ) {
            // Preview area (scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(160.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator(color = DotStreakAccent)
                } else if (uiState.habits.isEmpty()) {
                    Text(
                        "Add habits to preview\nyour wallpaper",
                        color = DotStreakSecondaryText,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                } else {
                    uiState.habits.forEach { habitWithDots ->
                        val habit = habitWithDots.habit
                        val habitColor = parseColor(habit.color)

                        // Habit header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = habit.name,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        // Dot grid
                        val rows = kotlin.math.ceil(
                            habitWithDots.dots.size.toDouble() / 20
                        ).toInt().coerceAtLeast(1)
                        DotGridCanvas(
                            dots = habitWithDots.dots,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((rows * 22).dp)
                        )

                        Spacer(Modifier.height(6.dp))

                        // Stats
                        val completedCount = habitWithDots.logs.count {
                            it.status == com.example.wallpaperapp.data.model.DayStatus.COMPLETED
                        }
                        val totalDays = java.time.temporal.ChronoUnit.DAYS
                            .between(habit.startDate, habit.endDate).toInt() + 1
                        val daysLeft = maxOf(
                            0,
                            java.time.temporal.ChronoUnit.DAYS
                                .between(java.time.LocalDate.now(), habit.endDate).toInt()
                        )
                        val pct = if (totalDays > 0) (completedCount * 100f / totalDays).roundToInt() else 0

                        Text(
                            text = "${daysLeft}d left · $pct%",
                            color = habitColor,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(28.dp))
                    }
                }
            }

            // Action buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.setAsWallpaper(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isExporting && uiState.habits.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = DotStreakAccent)
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.height(18.dp))
                    } else {
                        Text("Set as Lock Screen Wallpaper", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.saveToGallery(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isExporting && uiState.habits.isNotEmpty(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Save to Gallery")
                }
            }
        }
    }
}
