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
    val streakOffset: Int = 0 // manually added prior-streak days
) {
    val isInfinite: Boolean get() = endDate >= INFINITE_END_DATE
}
