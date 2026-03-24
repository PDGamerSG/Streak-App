package com.example.wallpaperapp.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderScheduler {

    fun scheduleForHabit(
        context: Context,
        habitId: Long,
        habitName: String,
        reminderTime: String  // "HH:mm"
    ) {
        if (reminderTime.isBlank()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check exact alarm permission on API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return  // Permission not granted; caller should redirect to settings
        }

        val parts = reminderTime.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val now = LocalDateTime.now()
        var triggerTime = now.toLocalDate().atTime(hour, minute)
        if (!triggerTime.isAfter(now)) {
            triggerTime = triggerTime.plusDays(1)
        }
        val triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = buildIntent(context, habitId, habitName)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerMillis, pendingIntent),
            pendingIntent
        )
    }

    fun cancel(context: Context, habitId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            buildIntent(context, habitId, ""),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
    }

    private fun buildIntent(context: Context, habitId: Long, habitName: String): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderReceiver.EXTRA_HABIT_NAME, habitName)
        }
}
