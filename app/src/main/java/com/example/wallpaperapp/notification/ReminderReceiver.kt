package com.example.wallpaperapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.wallpaperapp.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

                if (Settings.canDrawOverlays(context)) {
                    // "Display over other apps" is granted.
                    // SYSTEM_ALERT_WINDOW allows background activity launches on Android 10+,
                    // so we can start the popup directly — no notification bar entry at all.
                    context.startActivity(
                        Intent(context, ReminderPopupActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra(ReminderPopupActivity.EXTRA_HABIT_ID, habitId)
                            putExtra(ReminderPopupActivity.EXTRA_HABIT_NAME, habitName)
                        }
                    )
                } else {
                    // Permission not granted — signal the in-app dialog (if app is open)
                    // and post a notification with fullScreenIntent as fallback.
                    ReminderState.set(habitId, habitName)
                    NotificationHelper.createNotificationChannel(context)
                    NotificationHelper.showReminder(context, habitId, habitName)
                }

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
