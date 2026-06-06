package com.trainseat.app.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SeatApiService {

    @GET("api/v2/train/seats")
    suspend fun getIxigoSeats(
        @Query("trainNumber") trainNumber: String,
        @Query("fromStation") fromStation: String,
        @Query("toStation") toStation: String,
        @Query("journeyDate") journeyDate: String,
        @Query("classCode") classCode: String,
        @Query("quota") quota: String
    ): Response<IxigoResponse>
}

data class IxigoResponse(
    val data: IxigoData?
)

data class IxigoData(
    val seats: IxigoSeats?
)

data class IxigoSeats(
    val available: Int?
)
