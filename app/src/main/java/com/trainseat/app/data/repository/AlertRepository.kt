package com.trainseat.app.data.repository

import android.content.Context
import android.util.Log
import com.trainseat.app.data.db.AlertConfigDao
import com.trainseat.app.data.db.CheckResultDao
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.data.model.CheckResult
import com.trainseat.app.data.network.AvailabilityStatus
import com.trainseat.app.data.network.SeatAvailability
import com.trainseat.app.data.network.SeatAvailabilityParser
import com.trainseat.app.data.prefs.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertConfigDao: AlertConfigDao,
    private val checkResultDao: CheckResultDao,
    private val okHttpClient: OkHttpClient,
    private val userPreferences: UserPreferences
) {

    fun getAllAlerts(): Flow<List<AlertConfig>> = alertConfigDao.getAll()

    suspend fun getAlertById(id: Long): AlertConfig? = alertConfigDao.getById(id)

    suspend fun saveAlert(alertConfig: AlertConfig): Long = alertConfigDao.insert(alertConfig)

    suspend fun updateAlert(alertConfig: AlertConfig) = alertConfigDao.update(alertConfig)

    suspend fun deleteAlert(alertConfig: AlertConfig) = alertConfigDao.delete(alertConfig)

    suspend fun deleteAlertById(id: Long) = alertConfigDao.deleteById(id)

    suspend fun updateAlertStatus(id: Long, status: String) = alertConfigDao.updateStatus(id, status)

    suspend fun updateLastChecked(id: Long, lastChecked: Long, lastAvailable: Int) =
        alertConfigDao.updateLastChecked(id, lastChecked, lastAvailable)

    fun getCheckResults(alertId: Long): Flow<List<CheckResult>> =
        checkResultDao.getByAlertId(alertId)

    suspend fun saveCheckResult(checkResult: CheckResult): Long {
        val id = checkResultDao.insert(checkResult)
        checkResultDao.deleteOlderThan(checkResult.alertId, 50)
        return id
    }

    suspend fun getActiveAlerts(): List<AlertConfig> =
        alertConfigDao.getByStatus(AlertConfig.STATUS_ACTIVE)

    suspend fun expireOldAlerts() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val expired = alertConfigDao.getExpiredAlerts(today)
        expired.forEach { alert ->
            alertConfigDao.updateStatus(alert.id, AlertConfig.STATUS_EXPIRED)
        }
    }

    /**
     * Calls the RapidAPI "irctc1" Check Seat Availability endpoint.
     * Requires a RapidAPI key saved in Settings.
     *
     * Endpoint:
     *   GET https://irctc1.p.rapidapi.com/api/v1/checkSeatAvailability
     *       ?classType=SL&fromStationCode=..&quota=GN&toStationCode=..&trainNo=..&date=YYYY-MM-DD
     */
    suspend fun checkSeatAvailability(alertConfig: AlertConfig): SeatAvailability {
        val apiKey = userPreferences.rapidApiKeyFlow.first()
        if (apiKey.isBlank()) {
            Log.e("AlertRepository", "No RapidAPI key set")
            return SeatAvailability(AvailabilityStatus.UNKNOWN, 0, "NO_API_KEY")
        }

        // irctc1 returns each entry's date as dd-mm-yyyy; build that to match later.
        val targetDate = alertConfig.journeyDate.split("-").let { p ->
            if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}" else alertConfig.journeyDate
        }

        return try {
            val url = "https://irctc1.p.rapidapi.com/api/v1/checkSeatAvailability" +
                    "?classType=${alertConfig.travelClass}" +
                    "&fromStationCode=${alertConfig.fromStation}" +
                    "&quota=${alertConfig.quota}" +
                    "&toStationCode=${alertConfig.toStation}" +
                    "&trainNo=${alertConfig.trainNumber}" +
                    "&date=${alertConfig.journeyDate}"

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("X-RapidAPI-Key", apiKey)
                .addHeader("X-RapidAPI-Host", "irctc1.p.rapidapi.com")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("AlertRepository", "IRCTC API HTTP ${response.code}: $body")
                    return SeatAvailability(AvailabilityStatus.UNKNOWN, 0, "HTTP_${response.code}")
                }
                SeatAvailabilityParser.parseIrctc1(body, targetDate)
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "IRCTC API failed: ${e.message}")
            SeatAvailability(AvailabilityStatus.UNKNOWN, 0, "ERROR")
        }
    }

    suspend fun exportAlerts(): String {
        val alerts = alertConfigDao.getByStatus(AlertConfig.STATUS_ACTIVE) +
                alertConfigDao.getByStatus(AlertConfig.STATUS_PAUSED) +
                alertConfigDao.getByStatus(AlertConfig.STATUS_TRIGGERED)
        return com.google.gson.Gson().toJson(alerts)
    }

    suspend fun importAlerts(json: String): Int {
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<AlertConfig>>() {}.type
            val alerts: List<AlertConfig> = com.google.gson.Gson().fromJson(json, type)
            alerts.forEach { alert ->
                alertConfigDao.insert(alert.copy(id = 0, status = AlertConfig.STATUS_PAUSED))
            }
            alerts.size
        } catch (e: Exception) {
            Log.e("AlertRepository", "Import failed: ${e.message}")
            -1
        }
    }
}
