package com.trainseat.app.data.db

import androidx.room.*
import com.trainseat.app.data.model.AlertConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alertConfig: AlertConfig): Long

    @Update
    suspend fun update(alertConfig: AlertConfig)

    @Delete
    suspend fun delete(alertConfig: AlertConfig)

    @Query("SELECT * FROM alert_configs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<AlertConfig>>

    @Query("SELECT * FROM alert_configs WHERE id = :id")
    suspend fun getById(id: Long): AlertConfig?

    @Query("SELECT * FROM alert_configs WHERE status = :status")
    suspend fun getByStatus(status: String): List<AlertConfig>

    @Query("UPDATE alert_configs SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE alert_configs SET lastChecked = :lastChecked, lastAvailable = :lastAvailable WHERE id = :id")
    suspend fun updateLastChecked(id: Long, lastChecked: Long, lastAvailable: Int)

    @Query("SELECT * FROM alert_configs WHERE status IN ('ACTIVE') AND journeyDate < :today")
    suspend fun getExpiredAlerts(today: String): List<AlertConfig>

    @Query("DELETE FROM alert_configs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
