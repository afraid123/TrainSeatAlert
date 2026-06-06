package com.trainseat.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_results",
    foreignKeys = [ForeignKey(
        entity = AlertConfig::class,
        parentColumns = ["id"],
        childColumns = ["alertId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["alertId"])]
)
data class CheckResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alertId: Long,
    val timestamp: Long,
    val availableSeats: Int,
    val statusCode: String,
    val rawResponse: String?
)
