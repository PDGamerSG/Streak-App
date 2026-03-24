package com.example.wallpaperapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.Habit
import com.example.wallpaperapp.data.repository.HabitRepository
import com.example.wallpaperapp.domain.DotGridGenerator
import com.example.wallpaperapp.domain.DotState
import com.example.wallpaperapp.domain.StreakCalculator
import com.example.wallpaperapp.domain.StreakResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

class HomeViewModel(private val repository: HabitRepository) : ViewModel() {

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
        viewModelScope.launch { repository.deleteHabit(habitId) }
    }

    companion object {
        fun factory(repository: HabitRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(repository) as T
        }
    }
}
