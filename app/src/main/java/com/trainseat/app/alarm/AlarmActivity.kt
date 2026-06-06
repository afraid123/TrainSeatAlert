package com.trainseat.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trainseat.app.presentation.theme.TrainSeatAlertTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    private var alertId: Long = -1L
    private var availableSeats: Int = 0
    private var trainNumber: String = ""
    private var trainName: String = ""
    private var journeyDate: String = ""
    private var travelClass: String = ""
    private var fromStation: String = ""
    private var toStation: String = ""
    private var threshold: Int = 0
    private var repeatUntilDismissed: Boolean = true
    private var vibrate: Boolean = true
    private var snoozeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_DISMISS) {
                handleDismiss()
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.trainseat.app.DISMISS_ALARM"

        fun createIntent(
            context: Context,
            alertId: Long,
            availableSeats: Int,
            trainNumber: String,
            trainName: String,
            journeyDate: String,
            travelClass: String,
            fromStation: String,
            toStation: String,
            threshold: Int,
            repeatUntilDismissed: Boolean,
            vibrate: Boolean
        ): Intent {
            return Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("alertId", alertId)
                putExtra("availableSeats", availableSeats)
                putExtra("trainNumber", trainNumber)
                putExtra("trainName", trainName)
                putExtra("journeyDate", journeyDate)
                putExtra("travelClass", travelClass)
                putExtra("fromStation", fromStation)
                putExtra("toStation", toStation)
                putExtra("threshold", threshold)
                putExtra("repeatUntilDismissed", repeatUntilDismissed)
                putExtra("vibrate", vibrate)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        extractExtras()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, IntentFilter(ACTION_DISMISS), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(dismissReceiver, IntentFilter(ACTION_DISMISS))
        }

        setContent {
            TrainSeatAlertTheme {
                AlarmScreen(
                    trainNumber = trainNumber,
                    trainName = trainName,
                    journeyDate = journeyDate,
                    travelClass = travelClass,
                    fromStation = fromStation,
                    toStation = toStation,
                    availableSeats = availableSeats,
                    threshold = threshold,
                    onDismiss = { handleDismiss() },
                    onSnooze = { handleSnooze() }
                )
            }
        }
    }

    private fun extractExtras() {
        alertId = intent.getLongExtra("alertId", -1L)
        availableSeats = intent.getIntExtra("availableSeats", 0)
        trainNumber = intent.getStringExtra("trainNumber") ?: ""
        trainName = intent.getStringExtra("trainName") ?: ""
        journeyDate = intent.getStringExtra("journeyDate") ?: ""
        travelClass = intent.getStringExtra("travelClass") ?: ""
        fromStation = intent.getStringExtra("fromStation") ?: ""
        toStation = intent.getStringExtra("toStation") ?: ""
        threshold = intent.getIntExtra("threshold", 0)
        repeatUntilDismissed = intent.getBooleanExtra("repeatUntilDismissed", true)
        vibrate = intent.getBooleanExtra("vibrate", true)
    }

    private fun handleDismiss() {
        snoozeJob?.cancel()
        AlarmHelper.dismissAlarm(this, alertId)
        finish()
    }

    private fun handleSnooze() {
        AlarmHelper.stopRingtone()
        AlarmHelper.stopVibration(this)
        snoozeJob?.cancel()
        snoozeJob = scope.launch {
            delay(10 * 60 * 1000L)
            // Re-trigger alarm after snooze
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dismissReceiver)
        snoozeJob?.cancel()
        scope.cancel()
    }

    override fun onBackPressed() {
        // Prevent back press from dismissing alarm
    }
}

@Composable
fun AlarmScreen(
    trainNumber: String,
    trainName: String,
    journeyDate: String,
    travelClass: String,
    fromStation: String,
    toStation: String,
    availableSeats: Int,
    threshold: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF8B0000), Color(0xFFCC0000), Color(0xFF8B0000))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .semantics { contentDescription = "Alarm screen - seats available" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "SEATS AVAILABLE!",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "$availableSeats",
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    contentDescription = "$availableSeats seats available"
                }
            )

            Text(
                text = "seats available",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Train $trainNumber",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (trainName.isNotEmpty()) {
                Text(
                    text = trainName,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp
                )
            }

            Text(
                text = "$fromStation → $toStation",
                color = Color.White,
                fontSize = 18.sp
            )

            Text(
                text = "Class: $travelClass | Date: $journeyDate",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )

            Text(
                text = "Threshold: $threshold seats",
                color = Color.Yellow,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Snooze alarm for 10 minutes" },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("SNOOZE 10 MIN", color = Color.White)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Dismiss alarm" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF8B0000)
                    )
                ) {
                    Text("DISMISS", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
