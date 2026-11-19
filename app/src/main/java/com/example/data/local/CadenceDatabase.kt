package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FocusSessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CadenceDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var INSTANCE: CadenceDatabase? = null

        fun getInstance(context: Context): CadenceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CadenceDatabase::class.java,
                    "cadence_pomodoro.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
