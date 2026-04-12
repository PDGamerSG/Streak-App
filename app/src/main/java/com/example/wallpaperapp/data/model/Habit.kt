package com.example.wallpaperapp.data.model

import java.time.LocalDate

val INFINITE_END_DATE: LocalDate = LocalDate.of(9999, 12, 31)

data class Habit(
    val id: Long = 0,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val color: String,        // hex string e.g. "#4A90D9"
    val reminderTime: String, // "HH:mm" or "" if no reminder
    val streakOffset: Int = 0,
    val frequencyType: String = "DAILY",  // "DAILY", "WEEKLY", or "MONTHLY"
    val weeklyTarget: Int = 1             // days per week (1–7), only used when frequencyType=WEEKLY
) {
    val isInfinite: Boolean get() = endDate >= INFINITE_END_DATE
    val isWeekly: Boolean get() = frequencyType == "WEEKLY"
    val isMonthly: Boolean get() = frequencyType == "MONTHLY"
}
