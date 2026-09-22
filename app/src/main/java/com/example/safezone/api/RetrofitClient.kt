package com.example.safezone.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // =========================================================
    // AZURE HOSTED SAFEZONE API
    // =========================================================
    //
    // The API is hosted on Azure App Service.
    // Do not use the local computer IP address here.
    //
    // Old local URL:
    // http://192.168.0.149:5036/
    //
    // Hosted Azure URL:
    // https://safezone-api-2026-anehabhmdegdfqcm.southafricanorth-01.azurewebsites.net/
    //

    private const val BASE_URL =
        "https://safezone-api-2026-anehabhmdegdfqcm.southafricanorth-01.azurewebsites.net/"

    // =========================================================
    // RETROFIT API SERVICE
    // =========================================================

    val apiService: ApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}