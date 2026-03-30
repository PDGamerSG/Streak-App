package com.example.wallpaperapp.ui.addedit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.model.Habit
import com.example.wallpaperapp.data.model.INFINITE_END_DATE
import com.example.wallpaperapp.data.repository.HabitRepository
import com.example.wallpaperapp.notification.HabitCheckInHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddEditUiState(
    val name: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusDays(30),
    val isInfinite: Boolean = false,
    val isWeekly: Boolean = false,
    val weeklyTarget: Int = 4,
    val selectedColor: String = "#4A90D9",
    val reminderTime: String = "",
    val nameError: String? = null,
    val dateError: String? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val isEditMode: Boolean = false,
    // Edit check-in state
    val checkinDate: LocalDate = LocalDate.now(),
    val checkinStatus: DayStatus? = null,
    val checkinLoading: Boolean = false
)

class AddEditHabitViewModel(
    private val repository: HabitRepository,
    private val habitId: Long,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState

    /** Preserved from the loaded habit so edits don't wipe it. */
    private var existingStreakOffset: Int = 0

    init {
        if (habitId > 0) loadHabit()
    }

    private fun loadHabit() {
        viewModelScope.launch {
            val habit = repository.getHabitById(habitId).first() ?: return@launch
            existingStreakOffset = habit.streakOffset
            val todayLog = repository.getLogForHabitAndDate(habitId, LocalDate.now())
            _uiState.value = _uiState.value.copy(
                name = habit.name,
                startDate = habit.startDate,
                endDate = habit.endDate,
                isInfinite = habit.isInfinite,
                isWeekly = habit.isWeekly,
                weeklyTarget = habit.weeklyTarget,
                selectedColor = habit.color,
                reminderTime = habit.reminderTime,
                isEditMode = true,
                checkinStatus = todayLog?.status
            )
        }
    }

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value, nameError = null) }

    fun onStartDateChange(date: LocalDate) {
        val state = _uiState.value
        val newEndDate = if (!state.isEditMode && !state.isInfinite) date.plusDays(30) else state.endDate
        _uiState.value = state.copy(startDate = date, endDate = newEndDate, dateError = null)
    }

    fun onEndDateChange(date: LocalDate) { _uiState.value = _uiState.value.copy(endDate = date, dateError = null) }
    fun onInfiniteChange(infinite: Boolean) { _uiState.value = _uiState.value.copy(isInfinite = infinite, dateError = null) }
    fun onWeeklyChange(weekly: Boolean) { _uiState.value = _uiState.value.copy(isWeekly = weekly) }
    fun onWeeklyTargetChange(target: Int) { _uiState.value = _uiState.value.copy(weeklyTarget = target.coerceIn(1, 7)) }
    fun onColorChange(hex: String) { _uiState.value = _uiState.value.copy(selectedColor = hex) }
    fun onReminderTimeChange(time: String) { _uiState.value = _uiState.value.copy(reminderTime = time) }

    fun onCheckinDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(checkinDate = date, checkinLoading = true)
        viewModelScope.launch {
            val log = repository.getLogForHabitAndDate(habitId, date)
            _uiState.value = _uiState.value.copy(checkinStatus = log?.status, checkinLoading = false)
        }
    }

    fun markCheckinDate(status: DayStatus) {
        val state = _uiState.value
        viewModelScope.launch {
            if (state.checkinStatus == status) {
                repository.deleteDayLog(habitId, state.checkinDate)
                _uiState.value = _uiState.value.copy(checkinStatus = null)
            } else {
                repository.upsertDayLog(DayLog(habitId = habitId, date = state.checkinDate, status = status))
                _uiState.value = _uiState.value.copy(checkinStatus = status)
            }
            try {
                HabitCheckInHelper.autoUpdateWallpaper(appContext)
            } catch (_: Exception) { /* wallpaper permission may not be granted */ }
        }
    }

    fun saveHabit(onSaved: (Long) -> Unit) {
        val state = _uiState.value
        var hasError = false
        if (state.name.isBlank()) {
            _uiState.value = _uiState.value.copy(nameError = "Habit name is required")
            hasError = true
        }
        if (!state.isInfinite && !state.endDate.isAfter(state.startDate)) {
            _uiState.value = _uiState.value.copy(dateError = "End date must be after start date")
            hasError = true
        }
        if (hasError) return

        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            val habit = Habit(
                id = if (habitId > 0) habitId else 0L,
                name = state.name.trim(),
                startDate = state.startDate,
                endDate = if (state.isInfinite) INFINITE_END_DATE else state.endDate,
                color = state.selectedColor,
                reminderTime = state.reminderTime,
                streakOffset = existingStreakOffset,
                frequencyType = if (state.isWeekly) "WEEKLY" else "DAILY",
                weeklyTarget = state.weeklyTarget
            )
            val savedId = repository.upsertHabit(habit)
            try {
                HabitCheckInHelper.autoUpdateWallpaper(appContext)
            } catch (_: Exception) { /* wallpaper permission may not be granted */ }
            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
            onSaved(savedId)
        }
    }

    companion object {
        fun factory(repository: HabitRepository, habitId: Long, appContext: Context) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AddEditHabitViewModel(repository, habitId, appContext) as T
            }
    }
}
