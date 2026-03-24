package com.example.wallpaperapp.data.db

import android.content.ContentValues
import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate

class DayLogDao(private val db: AppDatabase) {

    fun getLogsForHabit(habitId: Long): Flow<List<DayLog>> =
        db.refreshSignal.flatMapLatest {
            flow { emit(queryForHabit(habitId)) }.flowOn(Dispatchers.IO)
        }

    fun getLogsForDate(date: LocalDate): Flow<List<DayLog>> =
        db.refreshSignal.flatMapLatest {
            flow { emit(queryForDate(date)) }.flowOn(Dispatchers.IO)
        }

    suspend fun getLogForHabitAndDate(habitId: Long, date: LocalDate): DayLog? =
        withContext(Dispatchers.IO) { queryForHabitAndDate(habitId, date) }

    suspend fun upsertDayLog(log: DayLog) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("habitId", log.habitId)
            put("date", log.date.toString())
            put("status", log.status.name)
        }
        // INSERT OR REPLACE handles the UNIQUE(habitId, date) constraint
        db.helper.writableDatabase.insertWithOnConflict(
            "day_logs", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        db.triggerRefresh()
    }

    private fun queryForHabit(habitId: Long): List<DayLog> {
        val cursor = db.helper.readableDatabase.query(
            "day_logs", null, "habitId=?", arrayOf(habitId.toString()),
            null, null, "date ASC"
        )
        return cursor.use { c -> buildList { while (c.moveToNext()) add(c.toDayLog()) } }
    }

    private fun queryForDate(date: LocalDate): List<DayLog> {
        val cursor = db.helper.readableDatabase.query(
            "day_logs", null, "date=?", arrayOf(date.toString()), null, null, null
        )
        return cursor.use { c -> buildList { while (c.moveToNext()) add(c.toDayLog()) } }
    }

    private fun queryForHabitAndDate(habitId: Long, date: LocalDate): DayLog? {
        val cursor = db.helper.readableDatabase.query(
            "day_logs", null, "habitId=? AND date=?",
            arrayOf(habitId.toString(), date.toString()), null, null, null, "1"
        )
        return cursor.use { if (it.moveToFirst()) it.toDayLog() else null }
    }

    private fun android.database.Cursor.toDayLog() = DayLog(
        id = getLong(getColumnIndexOrThrow("id")),
        habitId = getLong(getColumnIndexOrThrow("habitId")),
        date = LocalDate.parse(getString(getColumnIndexOrThrow("date"))),
        status = DayStatus.valueOf(getString(getColumnIndexOrThrow("status")))
    )
}
