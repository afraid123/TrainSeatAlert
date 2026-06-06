package com.trainseat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_configs")
data class AlertConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trainNumber: String,
    val trainName: String,
    val fromStation: String,
    val toStation: String,
    val journeyDate: String,
    val travelClass: String,
    val quota: String,
    val threshold: Int,
    val intervalMinutes: Int,
    val alertSound: String,
    val vibrate: Boolean,
    val repeatUntilDismissed: Boolean,
    val notes: String,
    val status: String,
    val lastChecked: Long?,
    val lastAvailable: Int?,
    val createdAt: Long
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_TRIGGERED = "TRIGGERED"
        const val STATUS_EXPIRED = "EXPIRED"

        const val SOUND_ALARM = "ALARM"
        const val SOUND_NOTIFICATION = "NOTIFICATION"
        const val SOUND_SILENT = "SILENT"
    }
}
