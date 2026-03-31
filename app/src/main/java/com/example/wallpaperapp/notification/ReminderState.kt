package com.example.wallpaperapp.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Singleton that holds a pending in-app reminder so MainActivity can show an overlay dialog. */
object ReminderState {
    data class PendingReminder(val habitId: Long, val habitName: String)

    private val _pending = MutableStateFlow<PendingReminder?>(null)
    val pending: StateFlow<PendingReminder?> = _pending

    fun set(habitId: Long, habitName: String) {
        _pending.value = PendingReminder(habitId, habitName)
    }

    fun clear() {
        _pending.value = null
    }
}
