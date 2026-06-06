package com.trainseat.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.data.repository.AlertRepository
import com.trainseat.app.worker.WorkerScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AlertRepository

    @Inject
    lateinit var workerScheduler: WorkerScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.expireOldAlerts()
                    val activeAlerts = repository.getActiveAlerts()
                    activeAlerts.forEach { alert ->
                        workerScheduler.scheduleAlert(alert)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
