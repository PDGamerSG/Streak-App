package com.example.wallpaperapp.notification

import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallpaperapp.data.model.DayStatus
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppReminderDialog(
    habitId: Long,
    habitName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeNow = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")) }

    fun dismiss() {
        cancelNotification(context, habitId)
        onDismiss()
    }

    fun checkin(status: DayStatus) {
        scope.launch {
            HabitCheckInHelper.checkIn(context.applicationContext, habitId, status)
            cancelNotification(context, habitId)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { dismiss() },
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3A3A3A))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 48.dp)
        ) {
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
                    border = BorderStroke(1.dp, Color(0xFF333333)),
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

private fun cancelNotification(context: Context, habitId: Long) {
    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .cancel(habitId.toInt())
}
