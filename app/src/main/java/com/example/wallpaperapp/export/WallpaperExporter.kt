package com.example.wallpaperapp.export

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.wallpaperapp.data.model.DayLog
import com.example.wallpaperapp.data.model.Habit
import com.example.wallpaperapp.domain.DotGridGenerator
import com.example.wallpaperapp.domain.DotState
import java.io.IOException
import java.time.LocalDate
import kotlin.math.ceil

object WallpaperExporter {

    private const val DOTS_PER_ROW = DotGridGenerator.DOTS_PER_ROW

    fun renderBitmap(
        context: Context,
        habits: List<Habit>,
        allLogs: Map<Long, List<DayLog>>,
        today: LocalDate = LocalDate.now()
    ): Bitmap {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.parseColor("#0D0D0D"))

        var yOffset = height * 0.55f
        val paddingH = width * 0.06f
        val usableWidth = width - paddingH * 2

        val habitNamePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = height * 0.022f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val statsPaint = Paint().apply {
            textSize = height * 0.018f
            isAntiAlias = true
        }

        habits.forEach { habit ->
            val logs = allLogs[habit.id] ?: emptyList()
            val dots = DotGridGenerator.generate(habit, logs, today)
            if (dots.isEmpty()) return@forEach

            // Habit name
            canvas.drawText(habit.name, paddingH, yOffset, habitNamePaint)
            yOffset += height * 0.028f

            // Draw dots
            val dotSpacing = usableWidth / (DOTS_PER_ROW + (DOTS_PER_ROW - 1) * 0.5f) * 1.5f
            val dotRadius = dotSpacing / 3f
            val rows = ceil(dots.size.toDouble() / DOTS_PER_ROW).toInt()
            val dotPaint = Paint().apply { isAntiAlias = true }

            dots.forEachIndexed { index, dot ->
                val col = index % DOTS_PER_ROW
                val row = index / DOTS_PER_ROW
                val cx = paddingH + col * dotSpacing + dotRadius
                val cy = yOffset + row * dotSpacing + dotRadius
                dotPaint.color = parseHexColor(dot.colorHex)

                canvas.drawCircle(cx, cy, dotRadius, dotPaint)
            }

            yOffset += rows * dotSpacing + dotRadius + height * 0.01f

            // Stats text
            val completedCount = logs.count { it.status == com.example.wallpaperapp.data.model.DayStatus.COMPLETED }
            val totalDays = java.time.temporal.ChronoUnit.DAYS.between(habit.startDate, habit.endDate).toInt() + 1
            val daysLeft = maxOf(0, java.time.temporal.ChronoUnit.DAYS.between(today, habit.endDate).toInt())
            val pct = if (totalDays > 0) (completedCount * 100 / totalDays) else 0
            statsPaint.color = android.graphics.Color.WHITE
            canvas.drawText("${daysLeft}d left · $pct%", paddingH, yOffset, statsPaint)
            yOffset += height * 0.045f
        }

        return bitmap
    }

    suspend fun saveToGallery(context: Context, bitmap: Bitmap): Uri? {
        val fileName = "DotStreak_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DotStreak")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = context.contentResolver.insert(collection, values) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
        return uri
    }

    fun setAsLockScreenWallpaper(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val wm = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            } else {
                wm.setBitmap(bitmap)
            }
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun parseHexColor(hex: String): Int {
        return try {
            android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        } catch (e: Exception) {
            android.graphics.Color.GRAY
        }
    }
}
