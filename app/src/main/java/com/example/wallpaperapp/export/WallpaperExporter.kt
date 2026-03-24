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
import com.example.wallpaperapp.data.model.DayStatus
import com.example.wallpaperapp.data.model.Habit
import com.example.wallpaperapp.domain.DotGridGenerator
import com.example.wallpaperapp.domain.DotState
import com.example.wallpaperapp.domain.StreakCalculator
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

object WallpaperExporter {

    private const val DOTS_PER_ROW  = DotGridGenerator.WALLPAPER_DOTS_PER_ROW  // 7
    private const val BG_COLOR      = "#0D0D0D"
    private const val ACCENT_COLOR  = "#FF6B35"
    private const val DIM_COLOR     = "#444444"    // divider / month label

    fun renderBitmap(
        context: Context,
        habits: List<Habit>,
        allLogs: Map<Long, List<DayLog>>,
        today: LocalDate = LocalDate.now()
    ): Bitmap {
        val metrics = context.resources.displayMetrics
        val w = metrics.widthPixels
        val h = metrics.heightPixels

        val bmp    = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.parseColor(BG_COLOR))

        // ── grid geometry ────────────────────────────────────────────────────
        val paddingH    = w * 0.10f
        val usableWidth = w - paddingH * 2
        val cellSize    = usableWidth / DOTS_PER_ROW
        val dotRadius   = cellSize * 0.20f          // small, clean dots
        val gridStartX  = paddingH

        // ── paints ───────────────────────────────────────────────────────────
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color         = android.graphics.Color.WHITE
            textSize      = h * 0.030f
            typeface      = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.04f
        }
        val statsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color         = android.graphics.Color.parseColor(ACCENT_COLOR)
            textSize      = h * 0.018f
            typeface      = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.06f
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color   = android.graphics.Color.parseColor(DIM_COLOR)
            strokeWidth = 1.5f
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // ── pre-compute sections ─────────────────────────────────────────────
        data class Section(
            val habit  : Habit,
            val logs   : List<DayLog>,
            val dots   : List<DotState>,
            val rows   : Int,
            val pct    : Int,
            val daysLeft: Int,
            val streak : Int
        )

        val monthStart    = today.withDayOfMonth(1)
        val daysInMonth   = YearMonth.of(today.year, today.month).lengthOfMonth()
        val daysLeft      = daysInMonth - today.dayOfMonth

        val sections = habits.mapNotNull { habit ->
            val logs = allLogs[habit.id] ?: emptyList()
            val dots = DotGridGenerator.generate(habit, logs, today)
            if (dots.isEmpty()) return@mapNotNull null

            val rows = ceil(dots.size.toDouble() / DOTS_PER_ROW).toInt().coerceAtLeast(1)

            // Percentage = completed this month / days passed in month
            val completedThisMonth = logs.count { log ->
                !log.date.isBefore(monthStart) && !log.date.isAfter(today) &&
                log.status == DayStatus.COMPLETED
            }
            val pct = if (today.dayOfMonth > 0) completedThisMonth * 100 / today.dayOfMonth else 0

            val streak = StreakCalculator.calculate(habit, logs, today).currentStreak
            Section(habit, logs, dots, rows, pct, daysLeft, streak)
        }
        if (sections.isEmpty()) return bmp

        // ── measure total content height for centering ───────────────────────
        val habitGap  = h * 0.05f       // space between habit blocks
        val lineH     = h * 0.012f      // general line gap

        fun sectionHeight(s: Section): Float =
            namePaint.textSize  + lineH * 1.5f +        // habit name
            s.rows * cellSize   +                       // dot grid
            lineH * 1.5f        +                       // gap
            statsPaint.textSize                         // stats

        val totalH = sections.sumOf { sectionHeight(it).toDouble() }.toFloat() +
                     (sections.size - 1) * habitGap

        // True vertical centre
        var y = ((h - totalH) / 2f).coerceAtLeast(h * 0.30f)

        // ── draw sections ────────────────────────────────────────────────────
        sections.forEachIndexed { i, s ->

            // Habit name — centred
            val nameX = (w - namePaint.measureText(s.habit.name)) / 2f
            canvas.drawText(s.habit.name, nameX, y + namePaint.textSize, namePaint)
            y += namePaint.textSize + lineH * 1.8f

            // Dot grid — each row aligns to cellSize
            s.dots.forEachIndexed { index, dot ->
                val col = index % DOTS_PER_ROW
                val row = index / DOTS_PER_ROW
                val cx  = gridStartX + col * cellSize + cellSize / 2f
                val cy  = y + row * cellSize + cellSize / 2f
                if (dot.isVisible) {
                    dotPaint.color = parseHexColor(dot.colorHex)
                    canvas.drawCircle(cx, cy, dotRadius, dotPaint)
                }
            }
            y += s.rows * cellSize + lineH * 1.8f

            // Stats — centred, orange
            val statsText = "${s.daysLeft}d left"
            val statsX    = (w - statsPaint.measureText(statsText)) / 2f
            canvas.drawText(statsText, statsX, y + statsPaint.textSize, statsPaint)
            y += statsPaint.textSize

            // Divider between habits (skip after last)
            if (i < sections.size - 1) {
                y += habitGap / 2f
                canvas.drawLine(paddingH * 2, y, w - paddingH * 2, y, dividerPaint)
                y += habitGap / 2f
            }
        }

        return bmp
    }

    // ── save / set helpers ────────────────────────────────────────────────────

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
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val uri = context.contentResolver.insert(collection, values) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { out ->
            val buf = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, buf)
            out.write(buf.toByteArray())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
        return uri
    }

    fun setAsLockScreenWallpaper(context: Context, bitmap: Bitmap): Boolean = try {
        val wm = WallpaperManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        else wm.setBitmap(bitmap)
        true
    } catch (_: IOException) { false }

    private fun parseHexColor(hex: String): Int = try {
        android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
    } catch (_: Exception) { android.graphics.Color.GRAY }
}
