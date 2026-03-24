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
                Spacer(Modifier.height(24.dp))

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
                        val habit  = habitWithDots.habit
                        val dotsPerRow = com.example.wallpaperapp.domain.DotGridGenerator.WALLPAPER_DOTS_PER_ROW
                        val today  = java.time.LocalDate.now()
                        val ym     = java.time.YearMonth.of(today.year, today.month)
                        val daysLeft = ym.lengthOfMonth() - today.dayOfMonth

                        Text(
                            text = habit.name.uppercase(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Light,
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${habitWithDots.streak}",
                            color = DotStreakAccent,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Thin,
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "DAY STREAK",
                            color = Color(0xFF888888),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Light,
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(10.dp))

                        // Dot grid — 7 per row
                        val rows = kotlin.math.ceil(
                            habitWithDots.dots.size.toDouble() / dotsPerRow
                        ).toInt().coerceAtLeast(1)
                        DotGridCanvas(
                            dots = habitWithDots.dots,
                            dotsPerRow = dotsPerRow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((rows * 28).dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "${daysLeft} DAYS LEFT",
                            color = Color(0xFF666666),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Light,
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(Modifier.height(32.dp))
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
