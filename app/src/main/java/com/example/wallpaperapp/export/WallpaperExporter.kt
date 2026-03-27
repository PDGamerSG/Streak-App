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
import com.example.wallpaperapp.data.model.INFINITE_END_DATE
import com.example.wallpaperapp.domain.DotGridGenerator
import com.example.wallpaperapp.domain.DotState
import com.example.wallpaperapp.domain.StreakCalculator
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

object WallpaperExporter {

    private const val DOTS_PER_ROW = DotGridGenerator.WALLPAPER_DOTS_PER_ROW  // 7
    private const val BG_COLOR     = "#0D0D0D"
    private const val ACCENT_COLOR = "#FF6B35"
    private const val DIM_COLOR    = "#333333"

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

        // ── layout constants ──────────────────────────────────────────────
        // Reserve space for lock-screen clock (top) and shortcuts (bottom)
        val topReserve    = h * 0.28f   // clock + date area
        val bottomReserve = h * 0.15f   // shortcut icons + nav bar
        val availableH    = h - topReserve - bottomReserve   // ~57 % of screen

        val paddingH    = w * 0.14f     // wide side margins → narrower, smaller grid
        val usableWidth = w - paddingH * 2

        // ── pre-compute sections ──────────────────────────────────────────
        data class Section(
            val habit      : Habit,
            val dots       : List<DotState>,
            val rows       : Int,
            val daysLeft   : Int,
            val streak     : Int,
            val dotsPerRow : Int
        )

        val monthStart  = today.withDayOfMonth(1)
        val daysInMonth = YearMonth.of(today.year, today.month).lengthOfMonth()
        val daysLeft    = daysInMonth - today.dayOfMonth

        val sections = habits.mapNotNull { habit ->
            val logs = allLogs[habit.id] ?: emptyList()
            val dots = DotGridGenerator.generate(habit, logs, today, completedColor = habit.color)
            if (dots.isEmpty()) return@mapNotNull null
            val rows   = ceil(dots.size.toDouble() / DOTS_PER_ROW).toInt().coerceAtLeast(1)
            val streak = StreakCalculator.calculate(habit, logs, today).currentStreak
            Section(habit, dots, rows, daysLeft, streak, DOTS_PER_ROW)
        }
        if (sections.isEmpty()) return bmp

        // ── dynamic scaling: fit content into the available zone ──────────
        //    Reference values are at scale = 1.0  (medium-small baseline)
        val dotCellRef  = usableWidth / DOTS_PER_ROW
        val lineHRef    = h * 0.008f
        val habitGapRef = h * 0.025f
        val nameRef     = h * 0.016f
        val streakRef   = h * 0.040f
        val labelRef    = h * 0.010f
        val statsRef    = h * 0.010f

        fun sectionHRef(s: Section): Float =
            nameRef   + lineHRef * 1.2f +
            streakRef + lineHRef * 0.4f +
            labelRef  + lineHRef * 1.8f +
            s.rows * dotCellRef + lineHRef * 1.2f +
            if (s.habit.isInfinite) 0f else statsRef + lineHRef * 0.6f

        val totalHRef = sections.sumOf { sectionHRef(it).toDouble() }.toFloat() +
                        (sections.size - 1) * habitGapRef

        // Shrink to fit availableH; never scale up beyond 1.0
        val scale       = (availableH / totalHRef).coerceAtMost(1.0f)
        val dotCellSize = dotCellRef  * scale
        val dotRadius   = dotCellSize * 0.28f   // medium-small dots
        val lineH       = lineHRef    * scale
        val habitGap    = habitGapRef * scale

        // ── paints ────────────────────────────────────────────────────────
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color         = android.graphics.Color.WHITE
            textSize      = nameRef * scale
            typeface      = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.12f
        }
        val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color         = android.graphics.Color.parseColor(ACCENT_COLOR)
            textSize      = streakRef * scale
            typeface      = Typeface.create("sans-serif-thin", Typeface.NORMAL)
            letterSpacing = 0.02f
        }
        val streakLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color         = android.graphics.Color.parseColor("#777777")
            textSize      = labelRef * scale
            typeface      = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.14f
        }
        val statsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color         = android.graphics.Color.parseColor("#888888")
            textSize      = statsRef * scale
            typeface      = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.10f
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color       = android.graphics.Color.parseColor(DIM_COLOR)
            strokeWidth = 1.5f
        }
        val dotPaint  = Paint(Paint.ANTI_ALIAS_FLAG)

        // ── center content in the available zone (between clock and shortcuts)
        val totalH = totalHRef * scale
        var y = topReserve + ((availableH - totalH) / 2f).coerceAtLeast(0f)

        // ── draw sections ─────────────────────────────────────────────────
        sections.forEachIndexed { i, s ->

            // Habit name
            val nameText = s.habit.name.uppercase()
            val nameX = (w - namePaint.measureText(nameText)) / 2f
            canvas.drawText(nameText, nameX, y + namePaint.textSize, namePaint)
            y += namePaint.textSize + lineH * 1.2f

            // Streak number
            val streakText = "${s.streak}"
            val streakX = (w - streakPaint.measureText(streakText)) / 2f
            canvas.drawText(streakText, streakX, y + streakPaint.textSize, streakPaint)
            y += streakPaint.textSize + lineH * 0.4f

            // Streak label
            val labelText = if (s.habit.isWeekly) "WEEK STREAK" else "DAY STREAK"
            val labelX = (w - streakLabelPaint.measureText(labelText)) / 2f
            canvas.drawText(labelText, labelX, y + streakLabelPaint.textSize, streakLabelPaint)
            y += streakLabelPaint.textSize + lineH * 1.8f

            // Dot grid — centered in usable width
            val gridW     = dotCellSize * s.dotsPerRow
            val gridStartX = paddingH + (usableWidth - gridW) / 2f
            s.dots.forEachIndexed { index, dot ->
                val col = index % s.dotsPerRow
                val row = index / s.dotsPerRow
                val cx  = gridStartX + col * dotCellSize + dotCellSize / 2f
                val cy  = y + row * dotCellSize + dotCellSize / 2f
                if (dot.isVisible) {
                    val color = parseHexColor(dot.colorHex)
                    dotPaint.color = color
                    canvas.drawCircle(cx, cy, dotRadius, dotPaint)
                }
            }
            y += s.rows * dotCellSize + lineH * 1.2f

            // Stats footer
            if (!s.habit.isInfinite) {
                val statsText = "${s.daysLeft} DAYS LEFT"
                val statsX = (w - statsPaint.measureText(statsText)) / 2f
                canvas.drawText(statsText, statsX, y + statsPaint.textSize, statsPaint)
                y += statsPaint.textSize + lineH * 0.6f
            }

            // Divider between habits (skip after last)
            if (i < sections.size - 1) {
                y += habitGap / 2f
                canvas.drawLine(paddingH, y, w - paddingH, y, dividerPaint)
                y += habitGap / 2f
            }
        }

        return bmp
    }

    // ── save / set helpers ─────────────────────────────────────────────────

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
