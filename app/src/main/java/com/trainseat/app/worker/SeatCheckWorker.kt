package com.trainseat.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trainseat.app.alarm.AlarmActivity
import com.trainseat.app.alarm.AlarmHelper
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.data.model.CheckResult
import com.trainseat.app.data.network.AvailabilityStatus
import com.trainseat.app.data.network.SeatAvailabilityParser
import com.trainseat.app.data.repository.AlertRepository
import com.trainseat.app.util.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SeatCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AlertRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val alertId = inputData.getLong("alertId", -1L)
        if (alertId == -1L) {
            Log.e("SeatCheckWorker", "No alertId provided")
            return Result.failure()
        }

        val alert = repository.getAlertById(alertId)
        if (alert == null) {
            Log.e("SeatCheckWorker", "Alert not found: $alertId")
            return Result.failure()
        }

        if (alert.status == AlertConfig.STATUS_PAUSED ||
            alert.status == AlertConfig.STATUS_EXPIRED
        ) {
            return Result.success()
        }

        val today = DateUtils.todayIso()
        if (alert.journeyDate < today) {
            repository.updateAlertStatus(alertId, AlertConfig.STATUS_EXPIRED)
            return Result.success()
        }

        return try {
            val availability = repository.checkSeatAvailability(alert)

            val checkResult = CheckResult(
                alertId = alertId,
                timestamp = System.currentTimeMillis(),
                availableSeats = availability.count,
                statusCode = availability.rawString,
                rawResponse = availability.rawString
            )
            repository.saveCheckResult(checkResult)
            repository.updateLastChecked(alertId, checkResult.timestamp, availability.count)

            if (SeatAvailabilityParser.shouldTriggerAlarm(availability, alert.threshold)) {
                triggerAlarm(alert, availability.count)
                repository.updateAlertStatus(alertId, AlertConfig.STATUS_TRIGGERED)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SeatCheckWorker", "Error checking seats for alert $alertId: ${e.message}")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                val checkResult = CheckResult(
                    alertId = alertId,
                    timestamp = System.currentTimeMillis(),
                    availableSeats = 0,
                    statusCode = "PARSE_ERROR",
                    rawResponse = e.message
                )
                repository.saveCheckResult(checkResult)
                Result.failure()
            }
        }
    }

    private fun triggerAlarm(alert: AlertConfig, availableSeats: Int) {
        try {
            AlarmHelper.triggerAlarm(context, alert, availableSeats)

            val alarmIntent = AlarmActivity.createIntent(
                context = context,
                alertId = alert.id,
                availableSeats = availableSeats,
                trainNumber = alert.trainNumber,
                trainName = alert.trainName,
                journeyDate = alert.journeyDate,
                travelClass = alert.travelClass,
                fromStation = alert.fromStation,
                toStation = alert.toStation,
                threshold = alert.threshold,
                repeatUntilDismissed = alert.repeatUntilDismissed,
                vibrate = alert.vibrate
            )
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.e("SeatCheckWorker", "Failed to trigger alarm: ${e.message}")
        }
    }
}
