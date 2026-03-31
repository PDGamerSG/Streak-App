package com.example.wallpaperapp.notification

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallpaperapp.data.model.DayStatus
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ReminderPopupActivity : ComponentActivity() {

    companion object {
        const val EXTRA_HABIT_ID   = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over the lock screen and wake the device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Dim the screen behind the bottom sheet
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.6f)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Anchor window to the bottom of the screen, full width
        window.setGravity(Gravity.BOTTOM)
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val habitId   = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: ""

        // Dismiss the companion notification immediately
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(habitId.toInt())

        setContent {
            val scope = rememberCoroutineScope()
            val timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))

            fun checkin(status: DayStatus) {
                scope.launch {
                    HabitCheckInHelper.checkIn(applicationContext, habitId, status)
                    finish()
                }
            }

            // Bottom sheet surface — no outer Box needed, window is already bottom-anchored
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 36.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF3A3A3A))
                )

                Spacer(Modifier.height(20.dp))

                // Timestamp + category pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Today, $timeNow",
                        color = Color(0xFFFF6B35),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF252525))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "HABIT",
                            color = Color(0xFF666666),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Habit name
                Text(
                    text = habitName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    lineHeight = 28.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Did you complete this today?",
                    color = Color(0xFF777777),
                    fontSize = 13.sp,
                    letterSpacing = 0.2.sp
                )

                Spacer(Modifier.height(28.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { checkin(DayStatus.MISSED) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1A1A1A)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "✗  Not Done",
                            color = Color(0xFFAAAAAA),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            letterSpacing = 0.3.sp
                        )
                    }

                    Button(
                        onClick = { checkin(DayStatus.COMPLETED) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "✓  Done",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }

    // Tapping outside (on the dim scrim) dismisses without logging
    override fun onPause() {
        super.onPause()
        if (!isFinishing) finish()
    }
}
