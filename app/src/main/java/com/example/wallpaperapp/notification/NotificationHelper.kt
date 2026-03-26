package com.example.wallpaperapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
object NotificationHelper {
    const val CHANNEL_ID = "dotstreak_reminders"
    private const val CHANNEL_NAME = "DotStreak Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily habit check-in reminders"
                enableVibration(true)
                enableLights(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showReminder(context: Context, habitId: Long, habitName: String) {
        // Tap notification body / full-screen → open popup dialog
        val popupIntent = Intent(context, ReminderPopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderPopupActivity.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderPopupActivity.EXTRA_HABIT_NAME, habitName)
        }
        val popupPendingIntent = PendingIntent.getActivity(
            context,
            habitId.toInt(),
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Done" action (inline, without opening popup)
        val doneIntent = Intent(context, CheckInActionReceiver::class.java).apply {
            action = CheckInActionReceiver.ACTION_DONE
            putExtra(CheckInActionReceiver.EXTRA_HABIT_ID, habitId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 10 + 1).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Skip" action (inline)
        val skipIntent = Intent(context, CheckInActionReceiver::class.java).apply {
            action = CheckInActionReceiver.ACTION_SKIP
            putExtra(CheckInActionReceiver.EXTRA_HABIT_ID, habitId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 10 + 2).toInt(),
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(habitName)
            .setContentText("Did you complete today's habit?")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Time to check in! Did you complete \"$habitName\" today?")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(popupPendingIntent)   // tap body → popup
            .setFullScreenIntent(popupPendingIntent, true)  // lock screen → popup
            .setAutoCancel(false)
            .addAction(android.R.drawable.checkbox_on_background, "✓  Done", donePendingIntent)
            .addAction(android.R.drawable.ic_delete, "✗  Skip", skipPendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(habitId.toInt(), notification)
    }
}
