package com.trainseat.app.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.data.model.CheckResult
import com.trainseat.app.data.repository.AlertRepository
import com.trainseat.app.domain.usecase.CheckSeatAvailabilityUseCase
import com.trainseat.app.domain.usecase.DeleteAlertUseCase
import com.trainseat.app.worker.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertDetailUiState(
    val alert: AlertConfig? = null,
    val checkResults: List<CheckResult> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val isCheckingNow: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AlertRepository,
    private val deleteAlertUseCase: DeleteAlertUseCase,
    private val checkSeatAvailabilityUseCase: CheckSeatAvailabilityUseCase,
    private val workerScheduler: WorkerScheduler
) : ViewModel() {

    private val alertId: Long = checkNotNull(savedStateHandle["alertId"])

    private val _uiState = MutableStateFlow(AlertDetailUiState())
    val uiState: StateFlow<AlertDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getCheckResults(alertId).collect { results ->
                _uiState.update { it.copy(checkResults = results) }
            }
        }
        loadAlert()
    }

    private fun loadAlert() {
        viewModelScope.launch {
            val alert = repository.getAlertById(alertId)
            _uiState.update { it.copy(alert = alert, isLoading = false) }
        }
    }

    fun deleteAlert() {
        viewModelScope.launch {
            workerScheduler.cancelAlert(alertId)
            deleteAlertUseCase(alertId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun togglePause() {
        viewModelScope.launch {
            val alert = _uiState.value.alert ?: return@launch
            if (alert.status == AlertConfig.STATUS_PAUSED) {
                repository.updateAlertStatus(alertId, AlertConfig.STATUS_ACTIVE)
                workerScheduler.scheduleAlert(alert.copy(status = AlertConfig.STATUS_ACTIVE))
                _uiState.update { it.copy(snackbarMessage = "Alert resumed") }
            } else {
                repository.updateAlertStatus(alertId, AlertConfig.STATUS_PAUSED)
                workerScheduler.cancelAlert(alertId)
                _uiState.update { it.copy(snackbarMessage = "Alert paused") }
            }
            loadAlert()
        }
    }

    fun checkNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingNow = true) }
            try {
                workerScheduler.scheduleImmediateCheck(alertId)
                _uiState.update { it.copy(snackbarMessage = "Checking now...") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Check failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isCheckingNow = false) }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
