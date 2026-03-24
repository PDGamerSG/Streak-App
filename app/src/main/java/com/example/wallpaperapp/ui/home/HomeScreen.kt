package com.example.wallpaperapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
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
                            onDelete = { viewModel.deleteHabit(habitStats.habit.id) },
                            onSaveStreakOffset = { from, to, prior ->
                                viewModel.updateStreak(habitStats.habit.id, from, to, prior)
                            }
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
    onDelete: () -> Unit,
    onSaveStreakOffset: (fromDay: Int, toDay: Int, priorDays: Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStreakEdit by remember { mutableStateOf(false) }

    val today = java.time.LocalDate.now()
    val initialThisMonth = (habitStats.streakResult.currentStreak - habitStats.habit.streakOffset)
        .coerceIn(0, today.dayOfMonth)
    var fromDay by remember(habitStats.habit.id) {
        mutableStateOf((today.dayOfMonth - initialThisMonth + 1).coerceAtLeast(1))
    }
    var toDay by remember(habitStats.habit.id) { mutableStateOf(today.dayOfMonth) }
    var priorDays by remember(habitStats.habit.id) { mutableStateOf(habitStats.habit.streakOffset) }

    Box {
        HabitCard(
            name = habitStats.habit.name,
            color = habitStats.habit.color,
            dots = habitStats.dots,
            streakResult = habitStats.streakResult,
            milestoneBadge = habitStats.milestoneBadge,
            onEditStreak = { showStreakEdit = true },
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { showMenu = true })
            }
        )
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() })
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
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showStreakEdit) {
        val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d")
        val monthLabel = today.month.getDisplayName(
            java.time.format.TextStyle.FULL, java.util.Locale.getDefault()
        ) + " ${today.year}"
        val thisMonthDays = (toDay - fromDay + 1).coerceAtLeast(0)
        val totalStreak = thisMonthDays + priorDays

        AlertDialog(
            onDismissRequest = { showStreakEdit = false },
            title = { Text("Edit Streak") },
            text = {
                Column {
                    // Month label
                    Text(monthLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        color = Color(0xFFAAAAAA))
                    Spacer(Modifier.height(12.dp))

                    // From → To steppers
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("From", fontSize = 11.sp, color = Color(0xFF888888))
                            DayStepper(fromDay, min = 1, max = toDay) {
                                fromDay = it
                            }
                            Text(
                                today.withDayOfMonth(fromDay).format(fmt),
                                fontSize = 11.sp, color = Color(0xFF888888)
                            )
                        }
                        Text("→", fontSize = 18.sp, color = Color(0xFFAAAAAA))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("To", fontSize = 11.sp, color = Color(0xFF888888))
                            DayStepper(toDay, min = fromDay, max = today.dayOfMonth) {
                                toDay = it
                            }
                            Text(
                                today.withDayOfMonth(toDay).format(fmt),
                                fontSize = 11.sp, color = Color(0xFF888888)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "= $thisMonthDays days this month",
                        fontSize = 12.sp, color = Color(0xFFAAAAAA),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    // Prior days row
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Days before this month", fontSize = 12.sp)
                        OutlinedTextField(
                            value = if (priorDays == 0) "" else priorDays.toString(),
                            onValueChange = { v ->
                                priorDays = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                            },
                            placeholder = { Text("0") },
                            modifier = Modifier.width(72.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Total streak summary
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "Total streak: $totalStreak days",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showStreakEdit = false
                    onSaveStreakOffset(fromDay, toDay, priorDays)
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showStreakEdit = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DayStepper(value: Int, min: Int, max: Int, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { if (value > min) onValueChange(value - 1) },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier.size(32.dp)
        ) { Text("−", fontSize = 18.sp) }
        Text(
            value.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        TextButton(
            onClick = { if (value < max) onValueChange(value + 1) },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier.size(32.dp)
        ) { Text("+", fontSize = 18.sp) }
    }
}
