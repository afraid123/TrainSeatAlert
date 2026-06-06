package com.trainseat.app.domain.usecase

import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.data.repository.AlertRepository
import javax.inject.Inject

class SaveAlertUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(alertConfig: AlertConfig): Long {
        return if (alertConfig.id == 0L) {
            repository.saveAlert(alertConfig)
        } else {
            repository.updateAlert(alertConfig)
            alertConfig.id
        }
    }
}
