package com.example.wallpaperapp.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.model.Habit
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
        if (habitId > 0) {
            loadHabit()
        }
    }

    private fun loadHabit() {
        viewModelScope.launch {
            val habit = repository.getHabitById(habitId).first() ?: return@launch
            _uiState.value = _uiState.value.copy(
                name = habit.name,
                startDate = habit.startDate,
                endDate = habit.endDate,
                selectedColor = habit.color,
                reminderTime = habit.reminderTime,
                isEditMode = true
            )
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = null)
    }

    fun onStartDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(startDate = date, dateError = null)
    }

    fun onEndDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(endDate = date, dateError = null)
    }

    fun onColorChange(hex: String) {
        _uiState.value = _uiState.value.copy(selectedColor = hex)
    }

    fun onReminderTimeChange(time: String) {
        _uiState.value = _uiState.value.copy(reminderTime = time)
    }

    fun saveHabit(onSaved: (Long) -> Unit) {
        val state = _uiState.value

        // Validate
        var hasError = false
        if (state.name.isBlank()) {
            _uiState.value = _uiState.value.copy(nameError = "Habit name is required")
            hasError = true
        }
        if (!state.endDate.isAfter(state.startDate)) {
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
                endDate = state.endDate,
                color = state.selectedColor,
                reminderTime = state.reminderTime
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
