package com.example.wallpaperapp

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallpaperapp.data.db.AppDatabase
import com.example.wallpaperapp.notification.InAppReminderDialog
import com.example.wallpaperapp.notification.ReminderState
import com.example.wallpaperapp.ui.navigation.DotStreakNavGraph
import com.example.wallpaperapp.ui.theme.DotStreakAccent
import com.example.wallpaperapp.ui.theme.WallpaperappTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
        com.example.wallpaperapp.notification.NotificationHelper
            .createNotificationChannel(this)

        val database = AppDatabase.getInstance(applicationContext)

        setContent {
            WallpaperappTheme {

                // Show "Display over other apps" explanation dialog on first launch
                // if the permission hasn't been granted yet.
                val needsOverlayPerm = remember {
                    !Settings.canDrawOverlays(this@MainActivity) && !hasShownOverlayDialog()
                }
                var showOverlayDialog by remember { mutableStateOf(needsOverlayPerm) }

                DotStreakNavGraph(database = database)

                // Permission explanation dialog
                if (showOverlayDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showOverlayDialog = false
                            markOverlayDialogShown()
                        },
                        title = {
                            Text(
                                "Enable Habit Reminder Popups",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                "To show habit reminders as a popup over any screen — " +
                                "even when you're using another app — DotStreak needs the " +
                                "\"Display over other apps\" permission.\n\n" +
                                "Without it, reminders will only appear as a notification " +
                                "in the top bar.",
                                color = Color(0xFFAAAAAA)
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showOverlayDialog = false
                                    markOverlayDialogShown()
                                    startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:$packageName")
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DotStreakAccent)
                            ) {
                                Text("Allow", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showOverlayDialog = false
                                    markOverlayDialogShown()
                                }
                            ) {
                                Text("Not Now", color = Color(0xFF666666))
                            }
                        },
                        containerColor = Color(0xFF1C1C1E),
                        titleContentColor = Color.White,
                        textContentColor = Color(0xFFAAAAAA)
                    )
                }

                // In-app reminder bottom sheet (when alarm fires while app is open)
                val pendingReminder by ReminderState.pending.collectAsStateWithLifecycle()
                pendingReminder?.let { reminder ->
                    InAppReminderDialog(
                        habitId = reminder.habitId,
                        habitName = reminder.habitName,
                        onDismiss = { ReminderState.clear() }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Returns true if we've already shown the overlay permission explanation once. */
    private fun hasShownOverlayDialog(): Boolean =
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("overlay_dialog_shown", false)

    /** Marks that the overlay permission dialog has been shown so it won't show again. */
    private fun markOverlayDialogShown() {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit().putBoolean("overlay_dialog_shown", true).apply()
    }

    /** Called from AddEditHabitScreen when exact-alarm permission is missing (API 31-32). */
    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }
}
