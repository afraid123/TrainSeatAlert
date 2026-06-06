package com.trainseat.app.domain.usecase

import com.trainseat.app.data.repository.AlertRepository
import javax.inject.Inject

class DeleteAlertUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(alertId: Long) {
        repository.deleteAlertById(alertId)
    }
}
