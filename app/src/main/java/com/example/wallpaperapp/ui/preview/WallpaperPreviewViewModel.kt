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
    val savedUri: Uri? = null,
    val isWallpaperEnabled: Boolean = false
)

private const val PREFS_NAME = "app_prefs"
private const val KEY_WALLPAPER_ENABLED = "wallpaper_enabled"

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
                                val dots = DotGridGenerator.generate(habit, logs, today)
                                HabitWithDots(
                                    habit = habit,
                                    dots = dots,
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
                if (success) persistWallpaperEnabled(context, true)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = success,
                    exportError = if (!success) "Failed to set wallpaper" else null,
                    isWallpaperEnabled = if (success) true else _uiState.value.isWallpaperEnabled
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportError = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun disableWallpaper(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportError = null)
            val success = WallpaperExporter.clearLockScreenWallpaper(context)
            if (success) persistWallpaperEnabled(context, false)
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportSuccess = success,
                exportError = if (!success) "Failed to disable wallpaper" else null,
                isWallpaperEnabled = if (success) false else _uiState.value.isWallpaperEnabled
            )
        }
    }

    fun loadWallpaperState(context: Context) {
        val enabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WALLPAPER_ENABLED, false)
        _uiState.value = _uiState.value.copy(isWallpaperEnabled = enabled)
    }

    private fun persistWallpaperEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_WALLPAPER_ENABLED, enabled).apply()
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
