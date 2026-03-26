package com.example.wallpaperapp.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.model.Habit
import com.example.wallpaperapp.data.model.INFINITE_END_DATE
import com.example.wallpaperapp.data.repository.HabitRepository
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
    val isEditMode: Boolean = false
)

class AddEditHabitViewModel(
    private val repository: HabitRepository,
    private val habitId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState

    init {
        if (habitId > 0) loadHabit()
    }

    private fun loadHabit() {
        viewModelScope.launch {
            val habit = repository.getHabitById(habitId).first() ?: return@launch
            _uiState.value = _uiState.value.copy(
                name = habit.name,
                startDate = habit.startDate,
                endDate = habit.endDate,
                isInfinite = habit.isInfinite,
                isWeekly = habit.isWeekly,
                weeklyTarget = habit.weeklyTarget,
                selectedColor = habit.color,
                reminderTime = habit.reminderTime,
                isEditMode = true
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
                frequencyType = if (state.isWeekly) "WEEKLY" else "DAILY",
                weeklyTarget = state.weeklyTarget
            )
            val savedId = repository.upsertHabit(habit)
            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
            onSaved(savedId)
        }
    }

    companion object {
        fun factory(repository: HabitRepository, habitId: Long) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AddEditHabitViewModel(repository, habitId) as T
            }
    }
}
