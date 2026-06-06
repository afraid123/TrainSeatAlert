package com.trainseat.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.trainseat.app.MainActivity
import com.trainseat.app.R
import com.trainseat.app.data.db.DatabaseModule
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SeatAlertWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = androidx.room.Room.databaseBuilder(
                context.applicationContext,
                com.trainseat.app.data.db.AppDatabase::class.java,
                "train_seat_alert.db"
            ).build()

            val alerts = db.alertConfigDao().getAll().first()
                .filter { it.status == AlertConfig.STATUS_ACTIVE }

            val urgentAlert = alerts.minByOrNull { alert ->
                val avail = alert.lastAvailable ?: Int.MAX_VALUE
                avail - alert.threshold
            }

            val views = RemoteViews(context.packageName, R.layout.widget_seat_alert)

            if (urgentAlert != null) {
                views.setTextViewText(
                    R.id.widget_train_number,
                    "Train ${urgentAlert.trainNumber}"
                )
                views.setTextViewText(
                    R.id.widget_route,
                    "${urgentAlert.fromStation} → ${urgentAlert.toStation} | ${urgentAlert.travelClass}"
                )
                views.setTextViewText(
                    R.id.widget_seats,
                    urgentAlert.lastAvailable?.toString() ?: "—"
                )
                views.setTextViewText(
                    R.id.widget_last_checked,
                    "Checked: ${DateUtils.formatTimestamp(urgentAlert.lastChecked)}"
                )
            } else {
                views.setTextViewText(R.id.widget_train_number, "No active alerts")
                views.setTextViewText(R.id.widget_route, "Add a train alert")
                views.setTextViewText(R.id.widget_seats, "—")
                views.setTextViewText(R.id.widget_last_checked, "")
            }

            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                android.app.PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            db.close()
        }
    }
}
