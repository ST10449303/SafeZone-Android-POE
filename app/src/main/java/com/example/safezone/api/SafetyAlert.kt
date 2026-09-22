package com.example.safezone.api

data class SafetyAlert(
    val id: Int,
    val title: String,
    val message: String,
    val campus: String,
    val location: String,
    val alertType: String,
    val createdAt: String,
    val isActive: Boolean
)