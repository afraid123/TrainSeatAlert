package com.trainseat.app.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val DISPLAY_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val TIMESTAMP_FORMAT = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun formatIsoDate(isoDate: String): String {
        return try {
            val date = ISO_FORMAT.parse(isoDate) ?: return isoDate
            DISPLAY_FORMAT.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatTimestamp(epochMillis: Long?): String {
        if (epochMillis == null) return "Never"
        return TIMESTAMP_FORMAT.format(Date(epochMillis))
    }

    fun todayIso(): String = ISO_FORMAT.format(Date())

    fun isDatePastOrToday(isoDate: String): Boolean {
        return try {
            val date = ISO_FORMAT.parse(isoDate) ?: return false
            val today = ISO_FORMAT.parse(todayIso()) ?: return false
            !date.before(today)
        } catch (e: Exception) {
            false
        }
    }

    fun epochToIso(epochMillis: Long): String = ISO_FORMAT.format(Date(epochMillis))

    fun isoToEpoch(isoDate: String): Long {
        return try {
            ISO_FORMAT.parse(isoDate)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
