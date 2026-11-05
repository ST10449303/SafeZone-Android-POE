package com.example.safezone

data class SafetyAlert(
    val id: Int = 0,
    val title: String = "",
    val message: String = "",
    val audience: String = "",
    val location: String = "",
    val campus: String = "",
    val alertType: String = "",
    val createdAt: String = "",
    val isActive: Boolean = false
)