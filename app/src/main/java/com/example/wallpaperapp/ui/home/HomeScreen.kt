package com.example.wallpaperapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallpaperapp.ui.checkin.CheckInBottomSheet
import com.example.wallpaperapp.ui.components.HabitCard
import com.example.wallpaperapp.ui.components.MilestoneOverlay
import com.example.wallpaperapp.ui.theme.DotStreakAccent
import com.example.wallpaperapp.ui.theme.DotStreakBackground
import com.example.wallpaperapp.ui.theme.DotStreakSecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    repository: com.example.wallpaperapp.data.repository.HabitRepository,
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onOpenPreview: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DotStreakBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DotStreak",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = onOpenPreview) {
                        Icon(
                            Icons.Filled.Wallpaper,
                            contentDescription = "Export Wallpaper",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DotStreakBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabit,
                containerColor = DotStreakAccent
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Habit", tint = Color.White)
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DotStreakBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { viewModel.showCheckIn() }) {
                    Text(
                        text = "✓ Check In Today",
                        color = DotStreakAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DotStreakBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading...", color = DotStreakSecondaryText)
                }
            } else if (uiState.habits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DotStreakBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No habits yet.\nTap + to add one!",
                        color = DotStreakSecondaryText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DotStreakBackground)
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.habits, key = { it.habit.id }) { habitStats ->
                        HabitCardWithMenu(
                            habitStats = habitStats,
                            onEdit = { onEditHabit(habitStats.habit.id) },
                            onDelete = { viewModel.deleteHabit(habitStats.habit.id) }
                        )
                    }
                }
            }

            // Milestone overlay
            if (uiState.milestoneHabitId != null && uiState.milestonePct != null) {
                MilestoneOverlay(
                    pct = uiState.milestonePct!!,
                    onDismiss = { viewModel.onMilestoneShown() }
                )
            }
        }

        // Check-in bottom sheet
        if (uiState.showCheckIn) {
            CheckInBottomSheet(
                repository = repository,
                onDismiss = { viewModel.hideCheckIn() },
                onMilestoneReached = { habitId, pct ->
                    viewModel.hideCheckIn()
                    viewModel.triggerMilestone(habitId, pct)
                }
            )
        }
    }
}

@Composable
private fun HabitCardWithMenu(
    habitStats: HabitWithStats,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box {
        HabitCard(
            name = habitStats.habit.name,
            color = habitStats.habit.color,
            dots = habitStats.dots,
            streakResult = habitStats.streakResult,
            milestoneBadge = habitStats.milestoneBadge,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { showMenu = true })
            }
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = { showMenu = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = Color(0xFFE74C3C)) },
                onClick = { showMenu = false; showDeleteConfirm = true }
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit") },
            text = { Text("Delete \"${habitStats.habit.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Delete", color = Color(0xFFE74C3C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
