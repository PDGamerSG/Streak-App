package com.example.wallpaperapp.domain

import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.model.Habit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class StreakResult(
    val currentStreak: Int,
    val bestStreak: Int,
    val completionPercent: Float,
    val daysLeft: Int,
    val totalDays: Int,
    val reachedMilestonePct: Int? = null
)

object StreakCalculator {
    private val MILESTONES = listOf(25, 50, 75, 100)

    fun calculate(
        habit: Habit,
        logs: List<DayLog>,
        today: LocalDate = LocalDate.now()
    ): StreakResult = if (habit.isWeekly)
        calculateWeekly(habit, logs, today)
    else
        calculateDaily(habit, logs, today)

    // ── Daily ─────────────────────────────────────────────────────────────────

    private fun calculateDaily(
        habit: Habit,
        logs: List<DayLog>,
        today: LocalDate
    ): StreakResult {
        val totalDays = (ChronoUnit.DAYS.between(habit.startDate, habit.endDate) + 1).toInt()
        val daysLeft  = YearMonth.of(today.year, today.month).lengthOfMonth() - today.dayOfMonth
        val logMap: Map<LocalDate, DayStatus> = logs.associate { it.date to it.status }

        val completedCount  = logs.count { it.status == DayStatus.COMPLETED }
        val completionPercent = if (totalDays > 0) (completedCount / totalDays.toFloat()) * 100f else 0f

        // Current streak: walk backwards from today
        var currentStreak = 0
        var cursor = today
        while (!cursor.isBefore(habit.startDate)) {
            val status = logMap[cursor]
            when {
                status == DayStatus.COMPLETED -> { currentStreak++; cursor = cursor.minusDays(1) }
                cursor == today && status == null -> cursor = cursor.minusDays(1)
                else -> break
            }
        }

        // Only carry previous-month offset when streak is unbroken from the 1st
        val monthStart      = today.withDayOfMonth(1)
        val effectiveOffset = if (cursor < monthStart) habit.streakOffset else 0

        // Best streak
        var bestStreak = 0; var running = 0
        var date = habit.startDate
        while (!date.isAfter(minOf(today, habit.endDate))) {
            if (logMap[date] == DayStatus.COMPLETED) { running++; bestStreak = maxOf(bestStreak, running) }
            else running = 0
            date = date.plusDays(1)
        }
        bestStreak = maxOf(bestStreak, currentStreak)

        return StreakResult(
            currentStreak     = currentStreak + effectiveOffset,
            bestStreak        = bestStreak,
            completionPercent = completionPercent,
            daysLeft          = daysLeft,
            totalDays         = totalDays
        )
    }

    // ── Weekly ────────────────────────────────────────────────────────────────

    private fun calculateWeekly(
        habit: Habit,
        logs: List<DayLog>,
        today: LocalDate
    ): StreakResult {
        val logMap         = logs.associate { it.date to it.status }
        val currentMonday  = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val habitMonday    = habit.startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        fun weekCount(monday: LocalDate): Int {
            var n = 0
            for (d in 0..6) {
                val date = monday.plusDays(d.toLong())
                if (!date.isBefore(habit.startDate) && !date.isAfter(today) &&
                    logMap[date] == DayStatus.COMPLETED) n++
            }
            return n
        }

        fun weekMet(monday: LocalDate) = weekCount(monday) >= habit.weeklyTarget

        // Current streak: walk backwards week by week
        var currentStreak = 0
        var cursor = currentMonday
        while (!cursor.isBefore(habitMonday)) {
            when {
                weekMet(cursor) -> { currentStreak++; cursor = cursor.minusWeeks(1) }
                cursor == currentMonday -> cursor = cursor.minusWeeks(1) // in-progress, skip
                else -> break
            }
        }

        val totalWeeks = (ChronoUnit.WEEKS.between(habitMonday, currentMonday) + 1).toInt().coerceAtLeast(1)

        var completedWeeks = 0
        var w = habitMonday
        while (!w.isAfter(currentMonday)) { if (weekMet(w)) completedWeeks++; w = w.plusWeeks(1) }

        val completionPercent = (completedWeeks.toFloat() / totalWeeks) * 100f

        return StreakResult(
            currentStreak     = currentStreak + habit.streakOffset,
            bestStreak        = currentStreak,
            completionPercent = completionPercent,
            daysLeft          = habit.weeklyTarget - weekCount(currentMonday),
            totalDays         = totalWeeks
        )
    }

    fun checkMilestone(previousPct: Float, newPct: Float): Int? =
        MILESTONES.firstOrNull { m -> previousPct < m && newPct >= m }
}
