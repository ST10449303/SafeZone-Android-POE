
package com.example.safezone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_requests")
data class EmergencyRequestEntity(
    @PrimaryKey
    val requestId: String,

    val userId: String,
    val assistanceType: String,
    val status: String = "Requested",
    val createdAt: Long = System.currentTimeMillis(),

    // Pending Sync, Synced, or Sync Failed
    val syncStatus: String = "Pending Sync"
)
