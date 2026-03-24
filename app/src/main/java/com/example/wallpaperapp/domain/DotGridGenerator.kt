package com.example.wallpaperapp.domain

import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.model.Habit
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DotState(
    val colorHex: String,
    val isToday: Boolean = false,
    val isGlowing: Boolean = false
)

object DotGridGenerator {
    const val DOTS_PER_ROW = 20

    // Standard colors
    const val COLOR_MISSED = "#E74C3C"
    const val COLOR_FUTURE = "#3A3A3A"
    const val COLOR_TODAY = "#FFFFFF"

    fun generate(
        habit: Habit,
        logs: List<DayLog>,
        today: LocalDate = LocalDate.now()
    ): List<DotState> {
        val totalDays = (ChronoUnit.DAYS.between(habit.startDate, habit.endDate) + 1).toInt()
        val logMap: Map<LocalDate, DayStatus> = logs.associate { it.date to it.status }

        return (0 until totalDays).map { index ->
            val date = habit.startDate.plusDays(index.toLong())
            when {
                date == today -> DotState(
                    colorHex = COLOR_TODAY,
                    isToday = true,
                    isGlowing = true
                )
                date.isAfter(today) -> DotState(colorHex = COLOR_FUTURE)
                logMap[date] == DayStatus.COMPLETED -> DotState(colorHex = COLOR_TODAY)
                else -> DotState(colorHex = COLOR_MISSED) // missed or no log for past day
            }
        }
    }

    fun canvasHeight(dotCount: Int, dotDiameter: Float): Float {
        val rows = Math.ceil(dotCount.toDouble() / DOTS_PER_ROW).toInt()
        val spacing = dotDiameter * 1.5f
        return rows * spacing
    }
}
