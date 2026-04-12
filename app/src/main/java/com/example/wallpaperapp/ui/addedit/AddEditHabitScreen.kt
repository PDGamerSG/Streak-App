package com.example.wallpaperapp.ui.addedit

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallpaperapp.notification.ReminderScheduler
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.ui.theme.DotStreakAccent
import com.example.wallpaperapp.ui.theme.DotStreakBackground
import com.example.wallpaperapp.ui.theme.DotStreakCard
import com.example.wallpaperapp.ui.theme.DotStreakSecondaryText
import com.example.wallpaperapp.ui.theme.HABIT_COLOR_PRESETS
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditHabitScreen(
    viewModel: AddEditHabitViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCheckinDatePicker by remember { mutableStateOf(false) }

    // Navigate back after successful save
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onNavigateBack()
    }

    Scaffold(
        containerColor = DotStreakBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) "Edit Habit" else "New Habit",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Habit name
            SectionLabel("Habit Name")
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                placeholder = { Text("e.g. Run every day", color = DotStreakSecondaryText) },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it, color = Color(0xFFE74C3C)) } },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DotStreakAccent,
                    unfocusedBorderColor = Color(0xFF2A2A2A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = DotStreakAccent
                ),
                singleLine = true
            )

            // Frequency type
            SectionLabel("Frequency")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val freqBtnModifier = Modifier.weight(1f)
                val monthlyColor = Color(0xFF4ECDC4)

                // Daily button
                val isDaily = uiState.frequencyType == "DAILY"
                androidx.compose.material3.OutlinedButton(
                    onClick = { viewModel.onFrequencyChange("DAILY") },
                    modifier = freqBtnModifier,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDaily) DotStreakAccent.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor = if (isDaily) DotStreakAccent else DotStreakSecondaryText
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isDaily) DotStreakAccent else Color(0xFF2A2A2A)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text("Daily", fontWeight = if (isDaily) FontWeight.Bold else FontWeight.Normal)
                }
                // Weekly button
                val isWeekly = uiState.isWeekly
                androidx.compose.material3.OutlinedButton(
                    onClick = { viewModel.onFrequencyChange("WEEKLY") },
                    modifier = freqBtnModifier,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isWeekly) Color(0xFF9B7FE8).copy(alpha = 0.15f) else Color.Transparent,
                        contentColor = if (isWeekly) Color(0xFF9B7FE8) else DotStreakSecondaryText
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isWeekly) Color(0xFF9B7FE8) else Color(0xFF2A2A2A)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text("Weekly", fontWeight = if (isWeekly) FontWeight.Bold else FontWeight.Normal)
                }
                // Monthly count button
                val isMonthly = uiState.isMonthly
                androidx.compose.material3.OutlinedButton(
                    onClick = { viewModel.onFrequencyChange("MONTHLY") },
                    modifier = freqBtnModifier,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isMonthly) monthlyColor.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor = if (isMonthly) monthlyColor else DotStreakSecondaryText
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isMonthly) monthlyColor else Color(0xFF2A2A2A)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text("Count", fontWeight = if (isMonthly) FontWeight.Bold else FontWeight.Normal)
                }
            }

            // Weekly target picker (only shown for weekly habits)
            if (uiState.isWeekly) {
                SectionLabel("Days per week target")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..7).forEach { n ->
                        val selected = uiState.weeklyTarget == n
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (selected) Color(0xFF9B7FE8)
                                    else Color(0xFF1E1E1E)
                                )
                                .border(
                                    1.dp,
                                    if (selected) Color(0xFF9B7FE8) else Color(0xFF2A2A2A),
                                    androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable { viewModel.onWeeklyTargetChange(n) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$n",
                                color = if (selected) Color.White else DotStreakSecondaryText,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Text(
                    "Streak counts as 1 when you hit ${uiState.weeklyTarget}+ days this week",
                    color = DotStreakSecondaryText,
                    fontSize = 11.sp
                )
            }

            // Start date
            SectionLabel("Start Date")
            DateFieldButton(
                label = uiState.startDate.format(dateFormatter),
                onClick = { showStartDatePicker = true }
            )

            // Ongoing toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Ongoing habit", color = Color.White, fontSize = 14.sp)
                    Text(
                        "No end date — goes on forever",
                        color = DotStreakSecondaryText,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = uiState.isInfinite,
                    onCheckedChange = viewModel::onInfiniteChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DotStreakAccent,
                        uncheckedThumbColor = DotStreakSecondaryText,
                        uncheckedTrackColor = DotStreakCard
                    )
                )
            }

            // End date — hidden for ongoing habits
            if (!uiState.isInfinite) {
                SectionLabel("End Date")
                DateFieldButton(
                    label = uiState.endDate.format(dateFormatter),
                    onClick = { showEndDatePicker = true }
                )
                uiState.dateError?.let {
                    Text(it, color = Color(0xFFE74C3C), fontSize = 12.sp)
                }
            }

            // Color picker
            SectionLabel("Color")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HABIT_COLOR_PRESETS.forEach { (hex, color) ->
                    val isSelected = uiState.selectedColor == hex
                    val isLight = hex == "#FFFFFF"
                    val borderColor = when {
                        isSelected && isLight -> Color(0xFF888888)
                        isSelected            -> Color.White
                        else                  -> Color(0xFF2A2A2A)
                    }
                    val checkTint = if (isLight) Color(0xFF1A1A1A) else Color.White
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, CircleShape)
                            .clickable { viewModel.onColorChange(hex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = checkTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Reminder time (optional)
            SectionLabel("Daily Reminder (optional)")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val reminderDisplay = if (uiState.reminderTime.isNotEmpty()) {
                    val t = LocalTime.parse(uiState.reminderTime)
                    t.format(DateTimeFormatter.ofPattern("h:mm a"))
                } else ""
                TextButton(
                    onClick = {
                        val (h, m) = if (uiState.reminderTime.isNotEmpty())
                            uiState.reminderTime.split(":").map { it.toInt() }
                        else listOf(8, 0)
                        TimePickerDialog(context, { _, hour, minute ->
                            viewModel.onReminderTimeChange("%02d:%02d".format(hour, minute))
                        }, h, m, false).show()
                    },
                    modifier = Modifier
                        .background(DotStreakCard, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = if (reminderDisplay.isNotEmpty()) "⏰ $reminderDisplay" else "Set Reminder",
                        color = if (reminderDisplay.isNotEmpty()) DotStreakAccent else DotStreakSecondaryText
                    )
                }
                if (uiState.reminderTime.isNotEmpty()) {
                    TextButton(onClick = { viewModel.onReminderTimeChange("") }) {
                        Text("Clear", color = Color(0xFFE74C3C), fontSize = 12.sp)
                    }
                }
            }

            // Edit check-in for a specific date (only in edit mode)
            if (uiState.isEditMode) {
                HorizontalDivider(color = Color(0xFF1E1E1E), thickness = 1.dp)

                // Section header
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "EDIT CHECK-IN",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Change the logged status for any date",
                        color = DotStreakSecondaryText,
                        fontSize = 11.sp
                    )
                }

                // Date selector
                SectionLabel("Date")
                DateFieldButton(
                    label = uiState.checkinDate.format(dateFormatter),
                    onClick = { showCheckinDatePicker = true }
                )

                // Status row: label + current value + clear button
                val statusText = when (uiState.checkinStatus) {
                    DayStatus.COMPLETED -> "Completed"
                    DayStatus.MISSED    -> "Missed"
                    else                -> "Not logged"
                }
                val statusColor = when (uiState.checkinStatus) {
                    DayStatus.COMPLETED -> Color(0xFF27AE60)
                    DayStatus.MISSED    -> Color(0xFFFF6B35)
                    else                -> DotStreakSecondaryText
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Current status", color = DotStreakSecondaryText, fontSize = 11.sp)
                        if (uiState.checkinLoading) {
                            Text("Loading…", color = DotStreakSecondaryText, fontSize = 14.sp)
                        } else {
                            Text(statusText, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (uiState.checkinStatus != null && !uiState.checkinLoading) {
                        androidx.compose.material3.TextButton(
                            onClick = { viewModel.markCheckinDate(uiState.checkinStatus!!) }
                        ) {
                            Text("Clear", color = Color(0xFF555555), fontSize = 12.sp)
                        }
                    }
                }

                // Done / Missed buttons — full-width with labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.markCheckinDate(DayStatus.COMPLETED) },
                        enabled = !uiState.checkinLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (uiState.checkinStatus == DayStatus.COMPLETED)
                                Color(0xFF27AE60).copy(alpha = 0.15f) else Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uiState.checkinStatus == DayStatus.COMPLETED) Color(0xFF27AE60) else Color(0xFF2A2A2A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "✓  Done",
                            color = if (uiState.checkinStatus == DayStatus.COMPLETED) Color(0xFF27AE60) else DotStreakSecondaryText,
                            fontWeight = if (uiState.checkinStatus == DayStatus.COMPLETED) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.markCheckinDate(DayStatus.MISSED) },
                        enabled = !uiState.checkinLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (uiState.checkinStatus == DayStatus.MISSED)
                                Color(0xFFFF6B35).copy(alpha = 0.12f) else Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uiState.checkinStatus == DayStatus.MISSED) Color(0xFFFF6B35) else Color(0xFF2A2A2A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "✗  Missed",
                            color = if (uiState.checkinStatus == DayStatus.MISSED) Color(0xFFFF6B35) else DotStreakSecondaryText,
                            fontWeight = if (uiState.checkinStatus == DayStatus.MISSED) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }

                Text(
                    "Tap a button to set status. Tap the active one again (or use Clear) to remove it.",
                    color = DotStreakSecondaryText,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    viewModel.saveHabit { savedId ->
                        // Schedule reminder if set
                        if (uiState.reminderTime.isNotEmpty()) {
                            ReminderScheduler.scheduleForHabit(
                                context = context,
                                habitId = savedId,
                                habitName = uiState.name,
                                reminderTime = uiState.reminderTime
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = DotStreakAccent)
            ) {
                Text(
                    if (uiState.isSaving) "Saving…" else if (uiState.isEditMode) "Save Changes" else "Create Habit",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Start date picker dialog
    if (showStartDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = uiState.startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.onStartDateChange(date)
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    // End date picker dialog
    if (showEndDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = uiState.endDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.onEndDateChange(date)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    // Check-in date picker dialog
    if (showCheckinDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = uiState.checkinDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showCheckinDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.onCheckinDateChange(date)
                    }
                    showCheckinDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCheckinDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = DotStreakSecondaryText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun DateFieldButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DotStreakCard)
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp)
    }
}
