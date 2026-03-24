package com.example.wallpaperapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

        // Show the notification
        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.showReminder(context, habitId, habitName)

        // Re-schedule tomorrow's alarm
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val habit = db.habitDao().getHabitById(habitId).first() ?: return@launch
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
