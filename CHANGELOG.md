# DotStreak – Changelog

---

## [Unreleased]

---

## Features & Fixes (in order of implementation)

### Weekly Habit Type
- New **frequency selector** in Add/Edit screen: Daily (default) or Weekly
- When Weekly is chosen, a **1–7 target picker** appears (circle buttons) showing how many days per week count as a completed week
- `generateWeekly()`: one dot per ISO week — white if target met, orange if missed, gray if current week in progress
- `calculateWeekly()`: streak counts consecutive completed weeks; current in-progress week is skipped, not counted as a break
- HabitCard shows **"WEEK STREAK"** label and purple **"×N/WK"** pill for weekly habits
- Wallpaper and preview render weekly habits at 4 dots/row with "WEEK STREAK" label
- DB migrated to version 3 adding `frequencyType` and `weeklyTarget` columns

### Wallpaper Preview Redesign
- Preview screen now shows a **phone frame mockup** (9:19.5 aspect ratio, rounded bezel, home indicator bar)
- Inside the frame: simulated lock screen with real current time, today's date, thin divider, and habit dots
- Weekly habit footer shows "×N/WEEK" (purple), infinite shows "ONGOING" (green), finite shows "X DAYS LEFT"

### UI Polish — Precision Dark Theme
- **HabitCard redesign**: left habit-color accent bar (full height), streak number as hero (40sp monospace), dynamic pill badge — blue "XD LEFT" for finite habits, green "ONGOING" for infinite habits
- **Today dot glow**: two concentric semi-transparent rings drawn behind today's dot in `DotGridCanvas` for immediate visual prominence
- **HomeScreen header**: "DOTSTREAK" in monospace with wide letter-spacing; today's full date shown below in dim secondary style
- **Check In CTA**: replaced text button with full-width orange `Button` at bottom of Home screen
- **CheckInBottomSheet**: matching left accent bars on habit rows, ✓/✗ symbol buttons with color state feedback, orange DONE button

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
