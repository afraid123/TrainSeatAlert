package com.trainseat.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.trainseat.app.MainActivity
import com.trainseat.app.R
import com.trainseat.app.alarm.AlarmActivity
import com.trainseat.app.data.model.AlertConfig

object NotificationUtils {

    const val CHANNEL_MONITORING = "monitoring"
    const val CHANNEL_ALARM = "alarm"
    const val NOTIFICATION_ID_BASE = 1000
    const val NOTIFICATION_ID_ALARM_BASE = 2000

    fun createNotificationChannels(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val monitoringChannel = NotificationChannel(
            CHANNEL_MONITORING,
            "Seat Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background seat availability monitoring"
        }

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "Seat Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when seats become available"
            enableVibration(true)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(monitoringChannel)
        notificationManager.createNotificationChannel(alarmChannel)
    }

    fun buildAlarmNotification(
        context: Context,
        alertConfig: AlertConfig,
        availableSeats: Int
    ): Notification {
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("alertId", alertConfig.id)
            putExtra("availableSeats", availableSeats)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alertConfig.id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val dismissIntent = Intent(context, com.trainseat.app.receiver.AlarmDismissReceiver::class.java).apply {
            putExtra("alertId", alertConfig.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alertConfig.id.toInt() + 10000,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_train)
            .setContentTitle("Seats Available! Train ${alertConfig.trainNumber}")
            .setContentText("$availableSeats seats available in ${alertConfig.travelClass} | ${alertConfig.fromStation} → ${alertConfig.toStation}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Train ${alertConfig.trainNumber} (${alertConfig.trainName})\n" +
                                "${alertConfig.fromStation} → ${alertConfig.toStation}\n" +
                                "Class: ${alertConfig.travelClass} | Date: ${alertConfig.journeyDate}\n" +
                                "$availableSeats seats available (threshold: ${alertConfig.threshold})"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_dismiss,
                "DISMISS",
                dismissPendingIntent
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun buildMonitoringNotification(context: Context, activeCount: Int): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_train)
            .setContentTitle("Train Seat Alert")
            .setContentText("Monitoring $activeCount alert(s)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun cancelAlarmNotification(context: Context, alertId: Long) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_ALARM_BASE + alertId.toInt())
    }

    fun showAlarmNotification(context: Context, alertConfig: AlertConfig, availableSeats: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildAlarmNotification(context, alertConfig, availableSeats)
        notificationManager.notify(NOTIFICATION_ID_ALARM_BASE + alertConfig.id.toInt(), notification)
    }
}
