package com.example.wallpaperapp.data.repository

import com.example.wallpaperapp.data.db.AppDatabase
import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.Habit
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val db: AppDatabase) {
    fun getAllHabits(): Flow<List<Habit>> = db.habitDao().getAllHabits()

    fun getHabitById(id: Long): Flow<Habit?> = db.habitDao().getHabitById(id)

    suspend fun upsertHabit(habit: Habit): Long = db.habitDao().insertHabit(habit)

    suspend fun deleteHabit(id: Long) = db.habitDao().deleteHabitById(id)

    fun getLogsForHabit(habitId: Long): Flow<List<DayLog>> =
        db.dayLogDao().getLogsForHabit(habitId)

    fun getLogsForDate(date: LocalDate): Flow<List<DayLog>> =
        db.dayLogDao().getLogsForDate(date)

    suspend fun getLogForHabitAndDate(habitId: Long, date: LocalDate): DayLog? =
        db.dayLogDao().getLogForHabitAndDate(habitId, date)

    suspend fun upsertDayLog(log: DayLog) = db.dayLogDao().upsertDayLog(log)
}
