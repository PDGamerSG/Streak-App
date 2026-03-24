package com.example.wallpaperapp.ui.preview

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.Habit
import com.example.wallpaperapp.data.repository.HabitRepository
import com.example.wallpaperapp.domain.DotGridGenerator
import com.example.wallpaperapp.domain.DotState
import com.example.wallpaperapp.domain.StreakCalculator
import com.example.wallpaperapp.export.WallpaperExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitWithDots(
    val habit: Habit,
    val dots: List<DotState>,
    val logs: List<DayLog>,
    val streak: Int = 0
)

data class PreviewUiState(
    val habits: List<HabitWithDots> = emptyList(),
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val exportError: String? = null,
    val savedUri: Uri? = null
)

class WallpaperPreviewViewModel(private val repository: HabitRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState

    init {
        loadHabits()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadHabits() {
        viewModelScope.launch {
            repository.getAllHabits()
                .flatMapLatest { habits ->
                    if (habits.isEmpty()) flowOf(emptyList<HabitWithDots>())
                    else {
                        val flows = habits.map { habit ->
                            repository.getLogsForHabit(habit.id).map { logs ->
                                val today = LocalDate.now()
                                HabitWithDots(
                                    habit = habit,
                                    dots = DotGridGenerator.generate(habit, logs, today),
                                    logs = logs,
                                    streak = StreakCalculator.calculate(habit, logs, today).currentStreak
                                )
                            }
                        }
                        combine(flows) { it.toList() }
                    }
                }
                .collect { list ->
                    _uiState.value = _uiState.value.copy(habits = list, isLoading = false)
                }
        }
    }

    fun saveToGallery(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportError = null)
            try {
                val state = _uiState.value
                val allLogs = state.habits.associate { it.habit.id to it.logs }
                val bitmap = WallpaperExporter.renderBitmap(context, state.habits.map { it.habit }, allLogs)
                val uri = WallpaperExporter.saveToGallery(context, bitmap)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = uri != null,
                    exportError = if (uri == null) "Failed to save image" else null,
                    savedUri = uri
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportError = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun setAsWallpaper(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportError = null)
            try {
                val state = _uiState.value
                val allLogs = state.habits.associate { it.habit.id to it.logs }
                val bitmap = WallpaperExporter.renderBitmap(context, state.habits.map { it.habit }, allLogs)
                val success = WallpaperExporter.setAsLockScreenWallpaper(context, bitmap)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = success,
                    exportError = if (!success) "Failed to set wallpaper" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportError = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun clearExportStatus() {
        _uiState.value = _uiState.value.copy(exportSuccess = false, exportError = null, savedUri = null)
    }

    companion object {
        fun factory(repository: HabitRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WallpaperPreviewViewModel(repository) as T
        }
    }
}
