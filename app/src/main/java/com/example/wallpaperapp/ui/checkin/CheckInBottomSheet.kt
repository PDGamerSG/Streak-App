package com.example.wallpaperapp.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.repository.HabitRepository
import com.example.wallpaperapp.ui.components.parseColor
import com.example.wallpaperapp.ui.theme.DotStreakBackground
import com.example.wallpaperapp.ui.theme.DotStreakCard
import com.example.wallpaperapp.ui.theme.DotStreakSecondaryText
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInBottomSheet(
    repository: HabitRepository,
    onDismiss: () -> Unit,
    onMilestoneReached: (Long, Int) -> Unit
) {
    val context = LocalContext.current
    val vm: CheckInViewModel = viewModel(factory = CheckInViewModel.factory(repository, context))
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismiss() {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = DotStreakCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Check In — ${LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d"))}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "How did you do today?",
                color = DotStreakSecondaryText,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))

            if (uiState.isLoading) {
                Text("Loading…", color = DotStreakSecondaryText)
            } else if (uiState.items.isEmpty()) {
                Text(
                    "No active habits today.",
                    color = DotStreakSecondaryText,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.items, key = { it.habit.id }) { item ->
                        CheckInHabitRow(
                            item = item,
                            onMark = { status ->
                                vm.markHabit(item.habit.id, status) { hId, pct ->
                                    onMilestoneReached(hId, pct)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { dismiss() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Text("Done", color = Color.White)
            }
        }
    }
}

@Composable
private fun CheckInHabitRow(
    item: HabitCheckInItem,
    onMark: (DayStatus) -> Unit
) {
    val habitColor = parseColor(item.habit.color)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotStreakBackground, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(habitColor)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = item.habit.name,
            color = Color.White,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp
        )
        Spacer(Modifier.width(8.dp))
        // Done button
        OutlinedButton(
            onClick = { onMark(DayStatus.COMPLETED) },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (item.todayStatus == DayStatus.COMPLETED)
                    Color(0xFF27AE60).copy(alpha = 0.2f) else Color.Transparent
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (item.todayStatus == DayStatus.COMPLETED) Color(0xFF27AE60) else Color(0xFF2A2A2A)
            ),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                "✓ Done",
                color = if (item.todayStatus == DayStatus.COMPLETED) Color(0xFF27AE60) else DotStreakSecondaryText,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(6.dp))
        // Missed button
        OutlinedButton(
            onClick = { onMark(DayStatus.MISSED) },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (item.todayStatus == DayStatus.MISSED)
                    Color(0xFFE74C3C).copy(alpha = 0.2f) else Color.Transparent
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (item.todayStatus == DayStatus.MISSED) Color(0xFFE74C3C) else Color(0xFF2A2A2A)
            ),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                "✗ Miss",
                color = if (item.todayStatus == DayStatus.MISSED) Color(0xFFE74C3C) else DotStreakSecondaryText,
                fontSize = 12.sp
            )
        }
    }
}
