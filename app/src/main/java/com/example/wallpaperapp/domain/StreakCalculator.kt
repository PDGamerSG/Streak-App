package com.example.wallpaperapp.domain

import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.model.Habit
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StreakResult(
    val currentStreak: Int,
    val bestStreak: Int,
    val completionPercent: Float,
    val daysLeft: Int,
    val totalDays: Int,
    /** Non-null when completion just crossed a milestone (25/50/75/100). */
    val reachedMilestonePct: Int? = null
)

object StreakCalculator {
    private val MILESTONES = listOf(25, 50, 75, 100)

    fun calculate(
        habit: Habit,
        logs: List<DayLog>,
        today: LocalDate = LocalDate.now()
    ): StreakResult {
        val totalDays = (ChronoUnit.DAYS.between(habit.startDate, habit.endDate) + 1).toInt()
        val daysLeft = maxOf(0, ChronoUnit.DAYS.between(today, habit.endDate).toInt())
        val logMap: Map<LocalDate, DayStatus> = logs.associate { it.date to it.status }

        // Count completed days (including today if logged)
        val completedCount = logs.count { it.status == DayStatus.COMPLETED }
        val completionPercent = if (totalDays > 0) (completedCount / totalDays.toFloat()) * 100f else 0f

        // Current streak: walk backwards from today
        var currentStreak = 0
        var cursor = today
        while (!cursor.isBefore(habit.startDate)) {
            val status = logMap[cursor]
            if (status == DayStatus.COMPLETED) {
                currentStreak++
                cursor = cursor.minusDays(1)
            } else if (cursor == today && status == null) {
                // Today not yet logged — don't break streak, just skip today
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }

        // Best streak: walk entire range
        var bestStreak = 0
        var running = 0
        var date = habit.startDate
        while (!date.isAfter(minOf(today, habit.endDate))) {
            if (logMap[date] == DayStatus.COMPLETED) {
                running++
                bestStreak = maxOf(bestStreak, running)
            } else {
                running = 0
            }
            date = date.plusDays(1)
        }
        bestStreak = maxOf(bestStreak, currentStreak)

        return StreakResult(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            completionPercent = completionPercent,
            daysLeft = daysLeft,
            totalDays = totalDays
        )
    }

    /** Returns the milestone pct if the previous count was below it and new count crosses it. */
    fun checkMilestone(previousPct: Float, newPct: Float): Int? {
        return MILESTONES.firstOrNull { m -> previousPct < m && newPct >= m }
    }
}
