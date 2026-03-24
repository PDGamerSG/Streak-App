package com.example.wallpaperapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.wallpaperapp.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        NotificationHelper.createNotificationChannel(context)

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val habits = db.habitDao().getAllHabits().first()
            habits.forEach { habit ->
                if (habit.reminderTime.isNotBlank()) {
                    ReminderScheduler.scheduleForHabit(
                        context = context,
                        habitId = habit.id,
                        habitName = habit.name,
                        reminderTime = habit.reminderTime
                    )
                }
            }
        }
    }
}
