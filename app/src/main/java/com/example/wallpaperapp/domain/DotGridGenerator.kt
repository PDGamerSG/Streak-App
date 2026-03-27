package com.example.wallpaperapp.domain

import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.model.Habit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class DotState(
    val colorHex: String,
    val isToday: Boolean = false,
    val isGlowing: Boolean = false,
    val isVisible: Boolean = true
)

object DotGridGenerator {
    const val DOTS_PER_ROW          = 20  // home screen compact view
    const val WALLPAPER_DOTS_PER_ROW = 7  // wallpaper / preview — one week per row
    const val WEEKLY_DOTS_PER_ROW   = 4   // weekly habits — ~1 month per row

    const val COLOR_MISSED = "#FF6B35"
    const val COLOR_FUTURE = "#3A3A3A"
    const val COLOR_TODAY  = "#FFFFFF"

    /**
     * One dot per day of the current month. Outside habit range = invisible placeholder.
     * [completedColor] defaults to white; pass [habit.color] for wallpaper rendering so
     * each habit's completed dots use its accent color.
     */
    fun generate(
        habit: Habit,
        logs: List<DayLog>,
        today: LocalDate = LocalDate.now(),
        completedColor: String = COLOR_TODAY
    ): List<DotState> {
        val monthStart  = today.withDayOfMonth(1)
        val daysInMonth = YearMonth.of(today.year, today.month).lengthOfMonth()
        val logMap      = logs.associate { it.date to it.status }

        return (0 until daysInMonth).map { index ->
            val date = monthStart.plusDays(index.toLong())
            when {
                date.isBefore(habit.startDate) || date.isAfter(habit.endDate) ->
                    DotState(colorHex = COLOR_FUTURE)
                date.isAfter(today) ->
                    DotState(colorHex = COLOR_FUTURE)
                date == today -> when (logMap[date]) {
                    DayStatus.MISSED    -> DotState(colorHex = COLOR_MISSED,    isToday = true)
                    DayStatus.COMPLETED -> DotState(colorHex = completedColor,  isToday = true)
                    else                -> DotState(colorHex = COLOR_TODAY,     isToday = true)
                }
                logMap[date] == DayStatus.COMPLETED ->
                    DotState(colorHex = completedColor)
                else ->
                    DotState(colorHex = COLOR_MISSED)
            }
        }
    }

    /**
     * One dot per ISO week from the habit's start week through the current week.
     * A week is "completed" when the number of COMPLETED logs in that week
     * meets or exceeds [Habit.weeklyTarget].
     */
    fun generateWeekly(
        habit: Habit,
        logs: List<DayLog>,
        today: LocalDate = LocalDate.now()
    ): List<DotState> {
        val logMap          = logs.associate { it.date to it.status }
        val currentMonday   = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val habitStartMonday = habit.startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val dots = mutableListOf<DotState>()
        var weekCursor = habitStartMonday

        while (!weekCursor.isAfter(currentMonday)) {
            val isCurrentWeek = weekCursor == currentMonday
            var completedCount = 0
            for (d in 0..6) {
                val date = weekCursor.plusDays(d.toLong())
                if (!date.isBefore(habit.startDate) && !date.isAfter(today) &&
                    logMap[date] == DayStatus.COMPLETED) {
                    completedCount++
                }
            }
            val weekMet = completedCount >= habit.weeklyTarget

            val dot = when {
                isCurrentWeek && weekMet  -> DotState(colorHex = COLOR_TODAY,  isToday = true)
                isCurrentWeek             -> DotState(colorHex = COLOR_FUTURE, isToday = true)
                weekMet                   -> DotState(colorHex = COLOR_TODAY)
                else                      -> DotState(colorHex = COLOR_MISSED)
            }
            dots.add(dot)
            weekCursor = weekCursor.plusWeeks(1)
        }

        return dots
    }
}
