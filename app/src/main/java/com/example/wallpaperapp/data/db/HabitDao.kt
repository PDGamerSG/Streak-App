package com.example.wallpaperapp.data.db

import android.content.ContentValues
import com.example.wallpaperapp.data.model.Habit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

class HabitDao(private val db: AppDatabase) {

    fun getAllHabits(): Flow<List<Habit>> =
        db.refreshSignal.flatMapLatest {
            flow { emit(queryAll()) }.flowOn(Dispatchers.IO)
        }

    fun getHabitById(id: Long): Flow<Habit?> =
        db.refreshSignal.flatMapLatest {
            flow { emit(queryById(id)) }.flowOn(Dispatchers.IO)
        }

    suspend fun insertHabit(habit: Habit): Long {
        val values = habit.toContentValues()
        val rowId = if (habit.id > 0) {
            val updated = db.helper.writableDatabase.update(
                "habits", values, "id=?", arrayOf(habit.id.toString())
            )
            if (updated > 0) habit.id else db.helper.writableDatabase.insert("habits", null, values)
        } else {
            db.helper.writableDatabase.insert("habits", null, values)
        }
        db.triggerRefresh()
        return rowId
    }

    suspend fun deleteHabitById(id: Long) {
        db.helper.writableDatabase.delete("habits", "id=?", arrayOf(id.toString()))
        db.triggerRefresh()
    }

    suspend fun getAllHabitsDirect(): List<Habit> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { queryAll() }

    private fun queryAll(): List<Habit> {
        val cursor = db.helper.readableDatabase.query(
            "habits", null, null, null, null, null, "id ASC"
        )
        return cursor.use { c ->
            buildList {
                while (c.moveToNext()) add(c.toHabit())
            }
        }
    }

    private fun queryById(id: Long): Habit? {
        val cursor = db.helper.readableDatabase.query(
            "habits", null, "id=?", arrayOf(id.toString()), null, null, null
        )
        return cursor.use { if (it.moveToFirst()) it.toHabit() else null }
    }

    private fun Habit.toContentValues() = ContentValues().apply {
        put("name", name)
        put("startDate", startDate.toString())
        put("endDate", endDate.toString())
        put("color", color)
        put("reminderTime", reminderTime)
        put("streakOffset", streakOffset)
    }

    private fun android.database.Cursor.toHabit() = Habit(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        startDate = LocalDate.parse(getString(getColumnIndexOrThrow("startDate"))),
        endDate = LocalDate.parse(getString(getColumnIndexOrThrow("endDate"))),
        color = getString(getColumnIndexOrThrow("color")),
        reminderTime = getString(getColumnIndexOrThrow("reminderTime")),
        streakOffset = getInt(getColumnIndexOrThrow("streakOffset"))
    )
}
