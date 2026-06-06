package com.trainseat.app.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Int.toIntervalLabel(): String = when (this) {
    15 -> "15 min"
    30 -> "30 min"
    60 -> "1 hr"
    120 -> "2 hr"
    240 -> "4 hr"
    360 -> "6 hr"
    720 -> "12 hr"
    else -> "$this min"
}

val INTERVAL_OPTIONS = listOf(15, 30, 60, 120, 240, 360, 720)

val TRAVEL_CLASS_OPTIONS = listOf("SL", "2S", "CC", "3A", "2A", "1A", "3E")

val QUOTA_OPTIONS = listOf("GN", "TQ", "LD", "PT")

fun String.toQuotaLabel(): String = when (this) {
    "GN" -> "General (GN)"
    "TQ" -> "Tatkal (TQ)"
    "LD" -> "Ladies (LD)"
    "PT" -> "Pre-Tatkal (PT)"
    else -> this
}
