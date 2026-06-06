package com.trainseat.app.domain.usecase

import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.data.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllAlertsUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    operator fun invoke(): Flow<List<AlertConfig>> = repository.getAllAlerts()
}
