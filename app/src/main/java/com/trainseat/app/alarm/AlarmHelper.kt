package com.trainseat.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.util.NotificationUtils

object AlarmHelper {

    private var currentRingtone: Ringtone? = null
    private val VIBRATION_PATTERN = longArrayOf(0L, 500L, 200L, 500L)

    fun triggerAlarm(context: Context, alert: AlertConfig, availableSeats: Int) {
        try {
            NotificationUtils.showAlarmNotification(context, alert, availableSeats)

            if (alert.alertSound != AlertConfig.SOUND_SILENT) {
                playAlarmSound(context, alert.alertSound)
            }

            if (alert.vibrate) {
                vibrate(context)
            }

            acquireWakeLock(context)
        } catch (e: Exception) {
            Log.e("AlarmHelper", "Failed to trigger alarm: ${e.message}")
        }
    }

    fun dismissAlarm(context: Context, alertId: Long) {
        try {
            stopRingtone()
            stopVibration(context)
            NotificationUtils.cancelAlarmNotification(context, alertId)
            releaseWakeLock()
        } catch (e: Exception) {
            Log.e("AlarmHelper", "Failed to dismiss alarm: ${e.message}")
        }
    }

    private fun playAlarmSound(context: Context, soundType: String) {
        try {
            stopRingtone()
            val ringtoneType = if (soundType == AlertConfig.SOUND_ALARM) {
                RingtoneManager.TYPE_ALARM
            } else {
                RingtoneManager.TYPE_NOTIFICATION
            }
            val uri = RingtoneManager.getDefaultUri(ringtoneType)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )

            currentRingtone = RingtoneManager.getRingtone(context, uri)?.also { ringtone ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.isLooping = true
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone.play()
            }
        } catch (e: Exception) {
            Log.e("AlarmHelper", "Failed to play sound: ${e.message}")
        }
    }

    fun stopRingtone() {
        currentRingtone?.let { if (it.isPlaying) it.stop() }
        currentRingtone = null
    }

    private fun vibrate(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(VIBRATION_PATTERN, 0)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmHelper", "Failed to vibrate: ${e.message}")
        }
    }

    fun stopVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.cancel()
            }
        } catch (e: Exception) {
            Log.e("AlarmHelper", "Failed to stop vibration: ${e.message}")
        }
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private fun acquireWakeLock(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "TrainSeatAlert:AlarmWakeLock"
            ).also {
                it.acquire(5 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.e("AlarmHelper", "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        } catch (e: Exception) {
            Log.e("AlarmHelper", "Failed to release wake lock: ${e.message}")
        }
    }
}
