package com.trainseat.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.domain.usecase.DeleteAlertUseCase
import com.trainseat.app.domain.usecase.GetAllAlertsUseCase
import com.trainseat.app.data.repository.AlertRepository
import com.trainseat.app.worker.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val alerts: List<AlertConfig> = emptyList(),
    val isLoading: Boolean = true,
    val snackbarMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getAllAlerts: GetAllAlertsUseCase,
    private val deleteAlert: DeleteAlertUseCase,
    private val repository: AlertRepository,
    private val workerScheduler: WorkerScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getAllAlerts().collect { alerts ->
                _uiState.update { it.copy(alerts = alerts, isLoading = false) }
            }
        }
        viewModelScope.launch {
            repository.expireOldAlerts()
        }
    }

    fun deleteAlert(alertId: Long) {
        viewModelScope.launch {
            workerScheduler.cancelAlert(alertId)
            deleteAlert.invoke(alertId)
            _uiState.update { it.copy(snackbarMessage = "Alert deleted") }
        }
    }

    fun togglePause(alertConfig: AlertConfig) {
        viewModelScope.launch {
            if (alertConfig.status == AlertConfig.STATUS_PAUSED) {
                repository.updateAlertStatus(alertConfig.id, AlertConfig.STATUS_ACTIVE)
                workerScheduler.scheduleAlert(alertConfig.copy(status = AlertConfig.STATUS_ACTIVE))
                _uiState.update { it.copy(snackbarMessage = "Alert resumed") }
            } else {
                repository.updateAlertStatus(alertConfig.id, AlertConfig.STATUS_PAUSED)
                workerScheduler.cancelAlert(alertConfig.id)
                _uiState.update { it.copy(snackbarMessage = "Alert paused") }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
