package com.example.safezone.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    // =========================================================
    // GET ALL SAFETY ALERTS
    // =========================================================

    @GET("api/SafetyAlerts")
    suspend fun getSafetyAlerts(): Response<List<SafetyAlert>>


    // =========================================================
    // GET ONE SAFETY ALERT
    // Example:
    // api/SafetyAlerts/1
    // =========================================================

    @GET("api/SafetyAlerts/{id}")
    suspend fun getSafetyAlert(
        @Path("id") id: Int
    ): Response<SafetyAlert>


    // =========================================================
    // CAMPUS ALERTS
    // Example:
    // api/SafetyAlerts/campus/Pretoria
    // =========================================================

    @GET("api/SafetyAlerts/campus/{campus}")
    suspend fun getAlertsByCampus(
        @Path("campus") campus: String
    ): Response<List<SafetyAlert>>


    // =========================================================
    // PERSONAL ALERTS
    // Example:
    // api/SafetyAlerts/location/Pretoria
    // =========================================================

    @GET("api/SafetyAlerts/location/{location}")
    suspend fun getAlertsByLocation(
        @Path("location") location: String
    ): Response<List<SafetyAlert>>


    // =========================================================
    // ALERTS BY TYPE
    // Example:
    // api/SafetyAlerts/type/Emergency
    // =========================================================

    @GET("api/SafetyAlerts/type/{alertType}")
    suspend fun getAlertsByType(
        @Path("alertType") alertType: String
    ): Response<List<SafetyAlert>>
}