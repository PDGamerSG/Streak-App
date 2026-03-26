# DotStreak – Changelog

---

## [Unreleased]

---

## Features & Fixes (in order of implementation)

### Base App
- Habit tracking with dot-grid visualization
- SQLite database via Room with `Habit` and `DayLog` models
- Home screen with habit cards showing dot grid for current month
- Add/Edit habit screen with name, color picker, start/end dates

### Wallpaper Export
- Renders dot-grid as a bitmap and sets it as the lock screen wallpaper
- Centered layout with habit name, streak number, dot rows, and days left
- Scale factor applied when 2+ habits are present (dots/fonts shrink)

### Streak Logic
- Streak calculator walks backwards from today counting consecutive completed days
- Cross-month streaks supported via `streakOffset` field on `Habit`
- Streak edit dialog on Home screen to carry over previous month's streak
- `streakOffset` only applied when streak is unbroken from the 1st of the current month (if a miss occurs, offset is ignored and streak resets to zero)

### Dot Colors
- Completed day → habit color (white default)
- Missed day → orange `#FF6B35`
- Future day → gray `#3A3A3A`
- Today (not yet logged) → white dot
- Today marked as MISSED → orange dot (fixed ordering bug in `DotGridGenerator`)

### Notifications & Reminders
- Set a daily reminder time per habit
- Notification fires via `AlarmManager.setAlarmClock()` (Doze-safe)
- Notification shows inline **Done** / **Skip** action buttons
- Tapping notification body opens `ReminderPopupActivity` over the lock screen
- Done/Skip from notification updates the log and auto-sets the wallpaper
- `BootReceiver` reschedules all reminders after device reboot
- Missed yesterday auto-logged when next day's alarm fires

### Wallpaper Auto-Update on Check-In
- After any check-in (Done or Skip) from the UI bottom sheet or notification, the lock screen wallpaper is re-rendered and updated immediately

### Ongoing / Infinite Habits
- Toggle "Ongoing habit" when creating a habit — no end date required
- Stored using sentinel `endDate = 9999-12-31`
- Ongoing habits hide "X DAYS LEFT" on wallpaper and preview

### Habit Management
- Delete habit via `⋮` menu button on each card (no long-press required)
- Default 30-day duration when creating a new habit
- End date auto-shifts when start date changes (for non-infinite habits)
- Percentage completion removed from habit cards and wallpaper

### Wallpaper Preview Screen
- Preview mirrors the wallpaper layout: name → streak → dots → days left
- Fixed 160dp spacer bug that pushed content off-screen

### Time Picker
- Reminder time picker uses 12-hour AM/PM format instead of 24-hour
- Stored internally as `HH:mm`; displayed as `h:mm AM/PM`
