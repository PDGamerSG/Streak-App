package com.example.wallpaperapp.ui.addedit

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.wallpaperapp.ui.components.parseColor
import com.example.wallpaperapp.ui.theme.DotStreakAccent
import com.example.wallpaperapp.ui.theme.DotStreakBackground
import com.example.wallpaperapp.ui.theme.DotStreakCard
import com.example.wallpaperapp.ui.theme.DotStreakSecondaryText
import com.example.wallpaperapp.ui.theme.HABIT_COLOR_PRESETS
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
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

            // Start date
            SectionLabel("Start Date")
            DateFieldButton(
                label = uiState.startDate.format(dateFormatter),
                onClick = { showStartDatePicker = true }
            )

            // End date
            SectionLabel("End Date")
            DateFieldButton(
                label = uiState.endDate.format(dateFormatter),
                onClick = { showEndDatePicker = true }
            )
            uiState.dateError?.let {
                Text(it, color = Color(0xFFE74C3C), fontSize = 12.sp)
            }

            // Color picker
            SectionLabel("Color")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HABIT_COLOR_PRESETS.forEach { (hex, color) ->
                    val isSelected = uiState.selectedColor == hex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                                else Modifier.border(1.dp, Color(0xFF2A2A2A), CircleShape)
                            )
                            .clickable { viewModel.onColorChange(hex) }
                    )
                }
            }

            // Reminder time (optional)
            SectionLabel("Daily Reminder (optional)")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = {
                        val (h, m) = if (uiState.reminderTime.isNotEmpty())
                            uiState.reminderTime.split(":").map { it.toInt() }
                        else listOf(8, 0)
                        TimePickerDialog(context, { _, hour, minute ->
                            viewModel.onReminderTimeChange("%02d:%02d".format(hour, minute))
                        }, h, m, true).show()
                    },
                    modifier = Modifier
                        .background(DotStreakCard, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = if (uiState.reminderTime.isNotEmpty())
                            "⏰ ${uiState.reminderTime}" else "Set Reminder",
                        color = if (uiState.reminderTime.isNotEmpty()) DotStreakAccent else DotStreakSecondaryText
                    )
                }
                if (uiState.reminderTime.isNotEmpty()) {
                    TextButton(onClick = { viewModel.onReminderTimeChange("") }) {
                        Text("Clear", color = Color(0xFFE74C3C), fontSize = 12.sp)
                    }
                }
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
