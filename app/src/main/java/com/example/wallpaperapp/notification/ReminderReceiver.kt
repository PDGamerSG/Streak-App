package com.example.wallpaperapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.wallpaperapp.data.db.AppDatabase
import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.DayStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: return
        if (habitId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val habit = db.habitDao().getHabitById(habitId).first() ?: return@launch
                val today = LocalDate.now()
                val yesterday = today.minusDays(1)

                // Auto-mark yesterday as MISSED if the user never tapped Done or Skip
                val isYesterdayInRange = !yesterday.isBefore(habit.startDate) &&
                        (habit.isInfinite || !yesterday.isAfter(habit.endDate))
                if (isYesterdayInRange) {
                    val existingLog = db.dayLogDao().getLogForHabitAndDate(habitId, yesterday)
                    if (existingLog == null) {
                        db.dayLogDao().upsertDayLog(
                            DayLog(habitId = habitId, date = yesterday, status = DayStatus.MISSED)
                        )
                    }
                }

                // Show today's reminder notification
                NotificationHelper.createNotificationChannel(context)
                NotificationHelper.showReminder(context, habitId, habitName)

                // Re-schedule tomorrow's alarm
                if (habit.reminderTime.isNotBlank()) {
                    ReminderScheduler.scheduleForHabit(
                        context = context,
                        habitId = habit.id,
                        habitName = habit.name,
                        reminderTime = habit.reminderTime
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
