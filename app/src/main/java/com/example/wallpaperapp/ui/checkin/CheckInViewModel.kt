package com.example.wallpaperapp.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.model.Habit
import com.example.wallpaperapp.data.repository.HabitRepository
import com.example.wallpaperapp.domain.StreakCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitCheckInItem(
    val habit: Habit,
    val todayStatus: DayStatus?,  // null = not yet logged
    val completionPercent: Float
)

data class CheckInUiState(
    val items: List<HabitCheckInItem> = emptyList(),
    val isLoading: Boolean = true
)

class CheckInViewModel(private val repository: HabitRepository) : ViewModel() {
    private val today = LocalDate.now()
    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState

    init {
        loadTodayItems()
    }

    private fun loadTodayItems() {
        viewModelScope.launch {
            val habits = repository.getAllHabits().first()
            val activeHabits = habits.filter { h ->
                !today.isBefore(h.startDate) && !today.isAfter(h.endDate)
            }
            if (activeHabits.isEmpty()) {
                _uiState.value = CheckInUiState(items = emptyList(), isLoading = false)
                return@launch
            }
            val items = activeHabits.map { habit ->
                val logs = repository.getLogsForHabit(habit.id).first()
                val todayLog = logs.find { it.date == today }
                val streakResult = StreakCalculator.calculate(habit, logs, today)
                HabitCheckInItem(
                    habit = habit,
                    todayStatus = todayLog?.status,
                    completionPercent = streakResult.completionPercent
                )
            }
            _uiState.value = CheckInUiState(items = items, isLoading = false)
        }
    }

    fun markHabit(habitId: Long, status: DayStatus, onMilestone: (Long, Int) -> Unit) {
        viewModelScope.launch {
            val currentItem = _uiState.value.items.find { it.habit.id == habitId } ?: return@launch
            val prevPct = currentItem.completionPercent

            repository.upsertDayLog(
                DayLog(habitId = habitId, date = today, status = status)
            )

            // Recalculate
            val logs = repository.getLogsForHabit(habitId).first()
            val streakResult = StreakCalculator.calculate(currentItem.habit, logs, today)
            val newPct = streakResult.completionPercent

            // Update local state
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items.map { item ->
                    if (item.habit.id == habitId)
                        item.copy(todayStatus = status, completionPercent = newPct)
                    else item
                }
            )

            // Milestone check
            val milestone = StreakCalculator.checkMilestone(prevPct, newPct)
            if (milestone != null && status == DayStatus.COMPLETED) {
                onMilestone(habitId, milestone)
            }
        }
    }

    companion object {
        fun factory(repository: HabitRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CheckInViewModel(repository) as T
        }
    }
}
