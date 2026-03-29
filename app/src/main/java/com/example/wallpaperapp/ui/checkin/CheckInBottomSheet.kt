package com.example.wallpaperapp.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.repository.HabitRepository
import com.example.wallpaperapp.ui.components.parseColor
import com.example.wallpaperapp.ui.theme.DotStreakAccent
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
                text = "CHECK IN",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(2.dp))
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                color = Color(0xFF555555),
                fontSize = 11.sp,
                letterSpacing = 0.2.sp
            )
            Spacer(Modifier.height(18.dp))

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
                                // Toggle off: if already set to this status, clear it
                                if (item.todayStatus == status) {
                                    vm.clearHabit(item.habit.id)
                                } else {
                                    vm.markHabit(item.habit.id, status) { hId, pct ->
                                        onMilestoneReached(hId, pct)
                                    }
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
                colors = ButtonDefaults.buttonColors(containerColor = DotStreakAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "DONE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontSize = 13.sp
                )
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
    val isNil = item.todayStatus == null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF111111))
    ) {
        // Left accent bar — dim when nil, bright when actioned
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (isNil) habitColor.copy(alpha = 0.3f) else habitColor)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Habit name + nil indicator
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.habit.name,
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isNil) {
                    Text(
                        text = "not logged",
                        color = Color(0xFF444444),
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Spacer(Modifier.width(8.dp))

            // Done button (✓) — toggles on/off
            OutlinedButton(
                onClick = { onMark(DayStatus.COMPLETED) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (item.todayStatus == DayStatus.COMPLETED)
                        Color(0xFF27AE60).copy(alpha = 0.15f) else Color.Transparent
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (item.todayStatus == DayStatus.COMPLETED) Color(0xFF27AE60) else Color(0xFF333333)
                ),
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(7.dp)
            ) {
                Text(
                    "✓",
                    color = if (item.todayStatus == DayStatus.COMPLETED) Color(0xFF27AE60) else Color(0xFF555555),
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(6.dp))

            // Missed button (✗) — toggles on/off
            OutlinedButton(
                onClick = { onMark(DayStatus.MISSED) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (item.todayStatus == DayStatus.MISSED)
                        Color(0xFFFF6B35).copy(alpha = 0.12f) else Color.Transparent
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (item.todayStatus == DayStatus.MISSED) Color(0xFFFF6B35) else Color(0xFF333333)
                ),
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(7.dp)
            ) {
                Text(
                    "✗",
                    color = if (item.todayStatus == DayStatus.MISSED) Color(0xFFFF6B35) else Color(0xFF555555),
                    fontSize = 13.sp
                )
            }
        }
    }
}
