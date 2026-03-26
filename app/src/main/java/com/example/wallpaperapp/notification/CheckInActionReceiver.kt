package com.example.wallpaperapp.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.wallpaperapp.data.model.DayStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CheckInActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DONE = "com.example.wallpaperapp.ACTION_DONE"
        const val ACTION_SKIP = "com.example.wallpaperapp.ACTION_SKIP"
        const val EXTRA_HABIT_ID = "habit_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId < 0) return

        val status = when (intent.action) {
            ACTION_DONE -> DayStatus.COMPLETED
            ACTION_SKIP -> DayStatus.MISSED
            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Write log + auto-update wallpaper
                HabitCheckInHelper.checkIn(context, habitId, status)
            } finally {
                // Dismiss notification
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(habitId.toInt())
                pendingResult.finish()
            }
        }
    }
}
