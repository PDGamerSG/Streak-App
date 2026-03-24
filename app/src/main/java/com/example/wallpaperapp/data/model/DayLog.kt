package com.example.wallpaperapp.data.model

import java.time.LocalDate

data class DayLog(
    val id: Long = 0,
    val habitId: Long,
    val date: LocalDate,
    val status: DayStatus
)
