package com.example.wallpaperapp.notification

import android.content.Context
import com.example.wallpaperapp.data.db.AppDatabase
import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.export.WallpaperExporter
import java.time.LocalDate

object HabitCheckInHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_WALLPAPER_ENABLED = "wallpaper_enabled"

    private fun isWallpaperEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WALLPAPER_ENABLED, false)

    /** Write today's status for [habitId] then re-render and set the lock-screen wallpaper. */
    suspend fun checkIn(context: Context, habitId: Long, status: DayStatus) {
        val db = AppDatabase.getInstance(context)

        // 1. Persist the log entry
        db.dayLogDao().upsertDayLog(
            DayLog(habitId = habitId, date = LocalDate.now(), status = status)
        )

        // 2. Re-render wallpaper with the latest data (only if the user opted in)
        autoUpdateWallpaper(context, db)
    }

    suspend fun autoUpdateWallpaper(context: Context, db: AppDatabase = AppDatabase.getInstance(context)) {
        if (!isWallpaperEnabled(context)) return
        val habits = db.habitDao().getAllHabitsDirect()
        if (habits.isEmpty()) return

        val allLogs = habits.associate { habit ->
            habit.id to db.dayLogDao().getAllLogsDirectForHabit(habit.id)
        }

        val bitmap = WallpaperExporter.renderBitmap(context, habits, allLogs)
        WallpaperExporter.setAsLockScreenWallpaper(context, bitmap)
    }
}
