package com.trainseat.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.data.model.CheckResult

@Database(
    entities = [AlertConfig::class, CheckResult::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertConfigDao(): AlertConfigDao
    abstract fun checkResultDao(): CheckResultDao
}
