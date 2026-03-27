package com.example.wallpaperapp.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.model.Habit
import com.example.wallpaperapp.data.repository.HabitRepository
import com.example.wallpaperapp.domain.DotGridGenerator
import com.example.wallpaperapp.domain.DotState
import com.example.wallpaperapp.domain.StreakCalculator
import com.example.wallpaperapp.domain.StreakResult
import com.example.wallpaperapp.notification.HabitCheckInHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitWithStats(
    val habit: Habit,
    val dots: List<DotState>,
    val streakResult: StreakResult,
    val milestoneBadge: Int? = null
)

data class HomeUiState(
    val habits: List<HabitWithStats> = emptyList(),
    val showCheckIn: Boolean = false,
    val milestoneHabitId: Long? = null,
    val milestonePct: Int? = null,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val repository: HabitRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadHabits()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadHabits() {
        viewModelScope.launch {
            repository.getAllHabits()
                .flatMapLatest { habits ->
                    if (habits.isEmpty()) {
                        flowOf(emptyList<HabitWithStats>())
                    } else {
                        val logFlows = habits.map { habit ->
                            repository.getLogsForHabit(habit.id).map { logs -> habit to logs }
                        }
                        combine(logFlows) { pairs ->
                            val today = LocalDate.now()
                            pairs.map { (habit, logs) ->
                                val dots = DotGridGenerator.generate(habit, logs, today)
                                val streakResult = StreakCalculator.calculate(habit, logs, today)
                                HabitWithStats(habit = habit, dots = dots, streakResult = streakResult)
                            }
                        }
                    }
                }
                .collect { habitStats ->
                    _uiState.value = _uiState.value.copy(
                        habits = habitStats,
                        isLoading = false
                    )
                }
        }
    }

    fun showCheckIn() {
        _uiState.value = _uiState.value.copy(showCheckIn = true)
    }

    fun hideCheckIn() {
        _uiState.value = _uiState.value.copy(showCheckIn = false)
    }

    fun onMilestoneShown() {
        _uiState.value = _uiState.value.copy(milestoneHabitId = null, milestonePct = null)
    }

    fun triggerMilestone(habitId: Long, pct: Int) {
        _uiState.value = _uiState.value.copy(milestoneHabitId = habitId, milestonePct = pct)
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            autoUpdateWallpaper()
        }
    }

    /** Backfills the fromDay..toDay range as COMPLETED, earlier days as MISSED, sets prior offset.
     *  Also extends habit.startDate backwards if the chosen range predates it. */
    fun updateStreak(habitId: Long, fromDay: Int, toDay: Int, priorDays: Int) {
        viewModelScope.launch {
            val habit = repository.getHabitById(habitId).first() ?: return@launch
            val today = LocalDate.now()
            val firstCompletedDate = today.withDayOfMonth(fromDay)

            // Write a log for every past day of this month (ignore startDate restriction)
            for (dayOfMonth in 1..today.dayOfMonth) {
                val date = today.withDayOfMonth(dayOfMonth)
                if (date.isAfter(habit.endDate)) continue
                val status = if (dayOfMonth in fromDay..toDay) DayStatus.COMPLETED else DayStatus.MISSED
                repository.upsertDayLog(DayLog(habitId = habitId, date = date, status = status))
            }

            // Push habit startDate back if needed so the new logs fall within the range
            val newStartDate = if (firstCompletedDate.isBefore(habit.startDate))
                firstCompletedDate else habit.startDate
            repository.upsertHabit(habit.copy(startDate = newStartDate, streakOffset = priorDays))
            autoUpdateWallpaper()
        }
    }

    private fun autoUpdateWallpaper() {
        viewModelScope.launch {
            try {
                HabitCheckInHelper.autoUpdateWallpaper(appContext)
            } catch (_: Exception) { /* silently skip if wallpaper permission not granted */ }
        }
    }

    companion object {
        fun factory(repository: HabitRepository, appContext: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(repository, appContext) as T
        }
    }
}
