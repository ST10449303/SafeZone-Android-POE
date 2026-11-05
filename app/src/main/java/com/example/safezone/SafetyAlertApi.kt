package com.example.safezone

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface SafetyAlertApi {

    @GET("api/SafetyAlerts")
    suspend fun getActiveAlerts(): Response<List<SafetyAlert>>

    @GET("api/SafetyAlerts/campus/{campus}")
    suspend fun getCampusAlerts(
        @Path("campus") campus: String
    ): Response<List<SafetyAlert>>

    @GET("api/SafetyAlerts/location/{location}")
    suspend fun getLocationAlerts(
        @Path("location") location: String
    ): Response<List<SafetyAlert>>
}