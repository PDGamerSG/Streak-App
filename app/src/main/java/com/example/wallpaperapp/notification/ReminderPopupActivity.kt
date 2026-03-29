package com.example.wallpaperapp.notification

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

        // Dim behind so it looks like a modal
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.7f)
        window.setBackgroundDrawableResource(android.R.color.transparent)

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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                // ── Main card ──
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1C1C1E),
                                    Color(0xFF141414)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFF2A2A2A),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    // ── Header: timestamp + close ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            // Category pill
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

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF252525))
                                .clickable { finish() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "\u00D7",   // ×
                                color = Color(0xFF888888),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Habit name ──
                    Text(
                        text = habitName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                        letterSpacing = (-0.3).sp,
                        lineHeight = 28.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Did you complete this today?",
                        color = Color(0xFF777777),
                        fontSize = 13.sp,
                        letterSpacing = 0.2.sp
                    )

                    Spacer(Modifier.height(32.dp))

                    // ── Action buttons: Not Done | Done ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Not Done button
                        OutlinedButton(
                            onClick = { checkin(DayStatus.MISSED) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF1A1A1A)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFF333333)
                            ),
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

                        // Done button
                        Button(
                            onClick = { checkin(DayStatus.COMPLETED) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B35)
                            ),
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

                    Spacer(Modifier.height(16.dp))

                    // ── Snooze link ──
                    Text(
                        text = "Tap outside to snooze",
                        color = Color(0xFF3A3A3A),
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }

    // Tapping outside the card (on the scrim) dismisses without logging
    override fun onPause() {
        super.onPause()
        if (!isFinishing) finish()
    }
}
