package com.trainseat.app.data.repository

import android.content.Context
import android.util.Log
import com.trainseat.app.data.db.AlertConfigDao
import com.trainseat.app.data.db.CheckResultDao
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.data.model.CheckResult
import com.trainseat.app.data.network.SeatApiService
import com.trainseat.app.data.network.SeatAvailability
import com.trainseat.app.data.network.SeatAvailabilityParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertConfigDao: AlertConfigDao,
    private val checkResultDao: CheckResultDao,
    private val seatApiService: SeatApiService
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

    suspend fun checkSeatAvailability(alertConfig: AlertConfig): SeatAvailability {
        val dateFormatted = alertConfig.journeyDate.replace("-", "")

        return try {
            val response = seatApiService.getIxigoSeats(
                trainNumber = alertConfig.trainNumber,
                fromStation = alertConfig.fromStation,
                toStation = alertConfig.toStation,
                journeyDate = dateFormatted,
                classCode = alertConfig.travelClass,
                quota = alertConfig.quota
            )
            if (response.isSuccessful) {
                val available = response.body()?.data?.seats?.available
                if (available != null) {
                    SeatAvailabilityParser.parseFromInt(available)
                } else {
                    tryErailFallback(alertConfig)
                }
            } else {
                tryErailFallback(alertConfig)
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "Ixigo API failed: ${e.message}")
            tryErailFallback(alertConfig)
        }
    }

    private suspend fun tryErailFallback(alertConfig: AlertConfig): SeatAvailability {
        return try {
            val dateFormatted = alertConfig.journeyDate.let { iso ->
                val parts = iso.split("-")
                if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso
            }
            val url = "https://erail.in/rail/getTrains.aspx?" +
                    "TrainNo=${alertConfig.trainNumber}" +
                    "&stnFrom=${alertConfig.fromStation}" +
                    "&stnTo=${alertConfig.toStation}" +
                    "&tdate=$dateFormatted" +
                    "&ClassID=${alertConfig.travelClass}" +
                    "&Quota=${alertConfig.quota}"

            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(url).build()
            val body = client.newCall(request).execute().body?.string() ?: ""
            val avlMatch = Regex("""(AVL\s+\d+|WL[-\s]\d+|REGRET|NOT\s+AVL)""").find(body)
            if (avlMatch != null) {
                SeatAvailabilityParser.parse(avlMatch.value)
            } else {
                SeatAvailability(
                    com.trainseat.app.data.network.AvailabilityStatus.UNKNOWN,
                    0,
                    "UNKNOWN"
                )
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "Erail fallback failed: ${e.message}")
            SeatAvailability(
                com.trainseat.app.data.network.AvailabilityStatus.UNKNOWN,
                0,
                "UNKNOWN"
            )
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
