package com.trainseat.app.domain.usecase

import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.data.network.SeatAvailability
import com.trainseat.app.data.repository.AlertRepository
import javax.inject.Inject

class CheckSeatAvailabilityUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(alertConfig: AlertConfig): SeatAvailability {
        return repository.checkSeatAvailability(alertConfig)
    }
}
