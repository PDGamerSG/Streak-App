package com.example.wallpaperapp.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Plain SQLiteOpenHelper — no annotation processing required.
 * Exposes a [refreshSignal] that DAOs use to emit reactive Flow updates.
 */
class AppDatabase private constructor(context: Context) {

    internal val refreshSignal = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    internal val helper: DbHelper = DbHelper(context.applicationContext)

    fun habitDao(): HabitDao = HabitDao(this)
    fun dayLogDao(): DayLogDao = DayLogDao(this)

    fun triggerRefresh() {
        refreshSignal.tryEmit(Unit)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                AppDatabase(context).also { INSTANCE = it }
            }
    }

    internal class DbHelper(context: Context) :
        SQLiteOpenHelper(context, "dotstreak.db", null, 2) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE habits (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    startDate TEXT NOT NULL,
                    endDate TEXT NOT NULL,
                    color TEXT NOT NULL,
                    reminderTime TEXT NOT NULL,
                    streakOffset INTEGER NOT NULL DEFAULT 0
                )"""
            )
            db.execSQL(
                """CREATE TABLE day_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    habitId INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    status TEXT NOT NULL,
                    UNIQUE(habitId, date),
                    FOREIGN KEY(habitId) REFERENCES habits(id) ON DELETE CASCADE
                )"""
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE habits ADD COLUMN streakOffset INTEGER NOT NULL DEFAULT 0")
            }
        }

        override fun onConfigure(db: SQLiteDatabase) {
            db.setForeignKeyConstraintsEnabled(true)
        }
    }
}
