package com.trainseat.app.worker

import android.content.Context
import androidx.work.*
import com.trainseat.app.data.model.AlertConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleAlert(alertConfig: AlertConfig) {
        val intervalMinutes = maxOf(alertConfig.intervalMinutes.toLong(), 15L)

        val request = PeriodicWorkRequestBuilder<SeatCheckWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setInputData(workDataOf("alertId" to alertConfig.id))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("alert_${alertConfig.id}")
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "alert_${alertConfig.id}",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAlert(alertId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("alert_$alertId")
    }

    fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWork()
    }

    fun scheduleImmediateCheck(alertId: Long) {
        val request = OneTimeWorkRequestBuilder<SeatCheckWorker>()
            .setInputData(workDataOf("alertId" to alertId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("immediate_check_$alertId")
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
