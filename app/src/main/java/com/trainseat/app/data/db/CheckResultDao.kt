package com.trainseat.app.data.db

import androidx.room.*
import com.trainseat.app.data.model.CheckResult
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkResult: CheckResult): Long

    @Query("SELECT * FROM check_results WHERE alertId = :alertId ORDER BY timestamp DESC LIMIT 50")
    fun getByAlertId(alertId: Long): Flow<List<CheckResult>>

    @Query("SELECT * FROM check_results WHERE alertId = :alertId ORDER BY timestamp DESC LIMIT 50")
    suspend fun getByAlertIdOnce(alertId: Long): List<CheckResult>

    @Query("""
        DELETE FROM check_results WHERE alertId = :alertId AND id NOT IN (
            SELECT id FROM check_results WHERE alertId = :alertId
            ORDER BY timestamp DESC LIMIT :limit
        )
    """)
    suspend fun deleteOlderThan(alertId: Long, limit: Int = 50)

    @Query("DELETE FROM check_results WHERE alertId = :alertId")
    suspend fun deleteByAlertId(alertId: Long)
}
