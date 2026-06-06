package com.trainseat.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.trainseat.app.alarm.AlarmActivity
import com.trainseat.app.alarm.AlarmHelper

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alertId = intent.getLongExtra("alertId", -1L)
        if (alertId != -1L) {
            AlarmHelper.dismissAlarm(context, alertId)
            val dismissIntent = Intent(AlarmActivity.ACTION_DISMISS)
            context.sendBroadcast(dismissIntent)
        }
    }
}
