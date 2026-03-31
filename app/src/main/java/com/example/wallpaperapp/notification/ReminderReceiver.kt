package com.example.wallpaperapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
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

                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isScreenOn = powerManager.isInteractive

                if (Settings.canDrawOverlays(context) && isScreenOn) {
                    // Screen is ON and overlay permission is granted.
                    // SYSTEM_ALERT_WINDOW allows background activity launches on Android 10+,
                    // so start the popup directly — no notification bar entry.
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
                    // Screen is OFF (or overlay permission not granted).
                    // Send a notification — the fullScreenIntent will wake the screen
                    // and show ReminderPopupActivity over the lock screen.
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
