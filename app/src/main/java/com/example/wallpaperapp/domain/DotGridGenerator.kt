package com.example.wallpaperapp.domain

import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.model.Habit
import java.time.LocalDate
import java.time.YearMonth

data class DotState(
    val colorHex: String,
    val isToday: Boolean = false,
    val isGlowing: Boolean = false,
    val isVisible: Boolean = true
)

object DotGridGenerator {
    const val DOTS_PER_ROW           = 20   // home screen compact view
    const val WALLPAPER_DOTS_PER_ROW = 7    // wallpaper / preview — one week per row

    const val COLOR_MISSED = "#FF6B35"
    const val COLOR_FUTURE = "#3A3A3A"
    const val COLOR_TODAY  = "#FFFFFF"

    /** One dot per day of the current month. Outside habit range = invisible placeholder. */
    fun generate(
        habit: Habit,
        logs: List<DayLog>,
        today: LocalDate = LocalDate.now()
    ): List<DotState> {
        val monthStart   = today.withDayOfMonth(1)
        val daysInMonth  = YearMonth.of(today.year, today.month).lengthOfMonth()
        val logMap       = logs.associate { it.date to it.status }

        return (0 until daysInMonth).map { index ->
            val date = monthStart.plusDays(index.toLong())
            when {
                date.isBefore(habit.startDate) || date.isAfter(habit.endDate) ->
                    DotState(colorHex = COLOR_FUTURE, isVisible = false)   // blank slot, preserves grid shape
                date == today ->
                    DotState(colorHex = COLOR_TODAY, isToday = true)
                date.isAfter(today) ->
                    DotState(colorHex = COLOR_FUTURE)                      // gray, visible
                logMap[date] == DayStatus.COMPLETED ->
                    DotState(colorHex = COLOR_TODAY)
                else ->
                    DotState(colorHex = COLOR_MISSED)
            }
        }
    }
}
