
package com.example.safezone.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_safety_alerts")
data class SafetyAlertEntity(
    @PrimaryKey
    val alertId: String,

    // Used by Personal Alerts
    val location: String,

    // Used by Campus Alerts
    @ColumnInfo(defaultValue = "''")
    val campus: String = "",

    val alertType: String,
    val title: String,
    val message: String,
    val createdAt: String,

    val cachedAt: Long = System.currentTimeMillis()
)
