package com.example.wallpaperapp.data.backup

import android.content.Context
import android.net.Uri
import com.example.wallpaperapp.data.db.AppDatabase
import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.model.Habit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object BackupManager {

    private const val KEY_VERSION = "backupVersion"
    private const val KEY_HABITS = "habits"
    private const val KEY_DAY_LOGS = "dayLogs"

    /**
     * Serialise the entire database to a JSON string.
     */
    suspend fun exportToJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val habits = db.habitDao().getAllHabitsDirect()
        val root = JSONObject()
        root.put(KEY_VERSION, 1)

        // Habits
        val habitsArr = JSONArray()
        for (h in habits) {
            habitsArr.put(JSONObject().apply {
                put("id", h.id)
                put("name", h.name)
                put("startDate", h.startDate.toString())
                put("endDate", h.endDate.toString())
                put("color", h.color)
                put("reminderTime", h.reminderTime)
                put("streakOffset", h.streakOffset)
                put("frequencyType", h.frequencyType)
                put("weeklyTarget", h.weeklyTarget)
            })
        }
        root.put(KEY_HABITS, habitsArr)

        // Day logs — batch per habit
        val logsArr = JSONArray()
        for (h in habits) {
            val logs = db.dayLogDao().getAllLogsDirectForHabit(h.id)
            for (l in logs) {
                logsArr.put(JSONObject().apply {
                    put("habitId", l.habitId)
                    put("date", l.date.toString())
                    put("status", l.status.name)
                })
            }
        }
        root.put(KEY_DAY_LOGS, logsArr)

        root.toString(2)
    }

    /**
     * Write the JSON string to the given [uri] (obtained from a SAF file picker).
     */
    suspend fun writeToUri(context: Context, uri: Uri, json: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
    }

    /**
     * Read a backup JSON from [uri] and restore into the database.
     * Existing data is replaced.
     */
    suspend fun restoreFromUri(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            ?: throw IllegalStateException("Cannot read backup file")
        restoreFromJson(context, json)
    }

    private suspend fun restoreFromJson(context: Context, json: String) {
        val root = JSONObject(json)
        val db = AppDatabase.getInstance(context)
        val sqDb = db.helper.writableDatabase

        sqDb.beginTransaction()
        try {
            // Clear existing data
            sqDb.delete("day_logs", null, null)
            sqDb.delete("habits", null, null)

            // Restore habits
            val habitsArr = root.getJSONArray(KEY_HABITS)
            val idMapping = mutableMapOf<Long, Long>()  // old id → new id
            for (i in 0 until habitsArr.length()) {
                val hj = habitsArr.getJSONObject(i)
                val oldId = hj.getLong("id")
                val habit = Habit(
                    id = 0, // let DB assign new id
                    name = hj.getString("name"),
                    startDate = LocalDate.parse(hj.getString("startDate")),
                    endDate = LocalDate.parse(hj.getString("endDate")),
                    color = hj.getString("color"),
                    reminderTime = hj.optString("reminderTime", ""),
                    streakOffset = hj.optInt("streakOffset", 0),
                    frequencyType = hj.optString("frequencyType", "DAILY"),
                    weeklyTarget = hj.optInt("weeklyTarget", 1)
                )
                val values = android.content.ContentValues().apply {
                    put("name", habit.name)
                    put("startDate", habit.startDate.toString())
                    put("endDate", habit.endDate.toString())
                    put("color", habit.color)
                    put("reminderTime", habit.reminderTime)
                    put("streakOffset", habit.streakOffset)
                    put("frequencyType", habit.frequencyType)
                    put("weeklyTarget", habit.weeklyTarget)
                }
                val newId = sqDb.insert("habits", null, values)
                idMapping[oldId] = newId
            }

            // Restore day logs
            val logsArr = root.getJSONArray(KEY_DAY_LOGS)
            for (i in 0 until logsArr.length()) {
                val lj = logsArr.getJSONObject(i)
                val oldHabitId = lj.getLong("habitId")
                val newHabitId = idMapping[oldHabitId] ?: continue
                val values = android.content.ContentValues().apply {
                    put("habitId", newHabitId)
                    put("date", lj.getString("date"))
                    put("status", lj.getString("status"))
                }
                sqDb.insertWithOnConflict(
                    "day_logs", null, values,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
            }

            sqDb.setTransactionSuccessful()
        } finally {
            sqDb.endTransaction()
        }

        db.triggerRefresh()
    }
}
