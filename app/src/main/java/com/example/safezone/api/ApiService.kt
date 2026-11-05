
package com.example.safezone.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Header

interface ApiService {

    // GET ALL SAFETY ALERTS
    @GET("api/SafetyAlerts")
    suspend fun getSafetyAlerts(): Response<List<SafetyAlert>>

    // GET ONE SAFETY ALERT
    @GET("api/SafetyAlerts/{id}")
    suspend fun getSafetyAlert(
        @Path("id") id: Int
    ): Response<SafetyAlert>

    // CAMPUS ALERTS
    @GET("api/SafetyAlerts/campus/{campus}")
    suspend fun getAlertsByCampus(
        @Path("campus") campus: String
    ): Response<List<SafetyAlert>>

    // PERSONAL ALERTS
    @GET("api/SafetyAlerts/location/{location}")
    suspend fun getAlertsByLocation(
        @Path("location") location: String
    ): Response<List<SafetyAlert>>

    // ALERTS BY TYPE
    @GET("api/SafetyAlerts/type/{alertType}")
    suspend fun getAlertsByType(
        @Path("alertType") alertType: String
    ): Response<List<SafetyAlert>>

    // REGISTER DEVICE FOR PUSH NOTIFICATIONS
    @POST("api/DeviceTokens/register")
    suspend fun registerDeviceToken(
        @Header("Authorization") authorization: String,
        @Body request: DeviceTokenRegistrationRequest
    ): Response<DeviceTokenRegistrationResponse>
}

data class DeviceTokenRegistrationRequest(
    val fcmToken: String,
    val audience: String,
    val campus: String = "",
    val location: String = ""
)

data class DeviceTokenRegistrationResponse(
    val message: String
)
