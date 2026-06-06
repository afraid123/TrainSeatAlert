package com.trainseat.app.presentation.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.data.repository.AlertRepository
import com.trainseat.app.domain.usecase.SaveAlertUseCase
import com.trainseat.app.util.DateUtils
import com.trainseat.app.worker.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val savedAlertId: Long = 0L,
    val errorMessage: String? = null,
    val trainNumber: String = "",
    val trainName: String = "",
    val fromStation: String = "",
    val toStation: String = "",
    val journeyDate: String = "",
    val travelClass: String = "SL",
    val quota: String = "GN",
    val threshold: String = "10",
    val intervalMinutes: Int = 30,
    val alertSound: String = AlertConfig.SOUND_ALARM,
    val vibrate: Boolean = true,
    val repeatUntilDismissed: Boolean = true,
    val notes: String = "",
    val validationErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class AddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AlertRepository,
    private val saveAlertUseCase: SaveAlertUseCase,
    private val workerScheduler: WorkerScheduler
) : ViewModel() {

    private val alertId: Long = savedStateHandle.get<Long>("alertId") ?: 0L

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (alertId != 0L) {
            loadAlert(alertId)
        }
    }

    private fun loadAlert(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val alert = repository.getAlertById(id)
            if (alert != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        trainNumber = alert.trainNumber,
                        trainName = alert.trainName,
                        fromStation = alert.fromStation,
                        toStation = alert.toStation,
                        journeyDate = alert.journeyDate,
                        travelClass = alert.travelClass,
                        quota = alert.quota,
                        threshold = alert.threshold.toString(),
                        intervalMinutes = alert.intervalMinutes,
                        alertSound = alert.alertSound,
                        vibrate = alert.vibrate,
                        repeatUntilDismissed = alert.repeatUntilDismissed,
                        notes = alert.notes
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTrainNumberChange(v: String) = _uiState.update { it.copy(trainNumber = v.filter { c -> c.isDigit() }.take(5)) }
    fun onTrainNameChange(v: String) = _uiState.update { it.copy(trainName = v) }
    fun onFromStationChange(v: String) = _uiState.update { it.copy(fromStation = v.uppercase().filter { c -> c.isLetter() }.take(5)) }
    fun onToStationChange(v: String) = _uiState.update { it.copy(toStation = v.uppercase().filter { c -> c.isLetter() }.take(5)) }
    fun onJourneyDateChange(v: String) = _uiState.update { it.copy(journeyDate = v) }
    fun onTravelClassChange(v: String) = _uiState.update { it.copy(travelClass = v) }
    fun onQuotaChange(v: String) = _uiState.update { it.copy(quota = v) }
    fun onThresholdChange(v: String) = _uiState.update { it.copy(threshold = v.filter { c -> c.isDigit() }.take(3)) }
    fun onIntervalChange(v: Int) = _uiState.update { it.copy(intervalMinutes = v) }
    fun onAlertSoundChange(v: String) = _uiState.update { it.copy(alertSound = v) }
    fun onVibrateChange(v: Boolean) = _uiState.update { it.copy(vibrate = v) }
    fun onRepeatUntilDismissedChange(v: Boolean) = _uiState.update { it.copy(repeatUntilDismissed = v) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v.take(100)) }

    fun saveAlert() {
        val state = _uiState.value
        val errors = validate(state)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val alert = AlertConfig(
                id = alertId,
                trainNumber = state.trainNumber,
                trainName = state.trainName,
                fromStation = state.fromStation,
                toStation = state.toStation,
                journeyDate = state.journeyDate,
                travelClass = state.travelClass,
                quota = state.quota,
                threshold = state.threshold.toInt(),
                intervalMinutes = maxOf(state.intervalMinutes, 15),
                alertSound = state.alertSound,
                vibrate = state.vibrate,
                repeatUntilDismissed = state.repeatUntilDismissed,
                notes = state.notes,
                status = AlertConfig.STATUS_ACTIVE,
                lastChecked = null,
                lastAvailable = null,
                createdAt = System.currentTimeMillis()
            )
            val savedId = saveAlertUseCase(alert)
            val savedAlert = alert.copy(id = savedId)
            workerScheduler.scheduleAlert(savedAlert)
            _uiState.update { it.copy(isLoading = false, isSaved = true, savedAlertId = savedId) }
        }
    }

    private fun validate(state: AddEditUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (state.trainNumber.length !in 4..5) errors["trainNumber"] = "Enter a valid 4–5 digit train number"
        if (state.fromStation.length !in 2..5) errors["fromStation"] = "Enter a valid 2–5 letter station code"
        if (state.toStation.length !in 2..5) errors["toStation"] = "Enter a valid 2–5 letter station code"
        if (state.journeyDate.isBlank()) errors["journeyDate"] = "Select a journey date"
        else if (!DateUtils.isDatePastOrToday(state.journeyDate)) errors["journeyDate"] = "Date must be today or future"
        val thresholdInt = state.threshold.toIntOrNull()
        if (thresholdInt == null || thresholdInt !in 1..500) errors["threshold"] = "Enter a number between 1 and 500"
        return errors
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
