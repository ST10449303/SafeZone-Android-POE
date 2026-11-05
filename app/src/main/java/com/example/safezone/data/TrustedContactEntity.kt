
package com.example.safezone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey
    val contactId: String,
    val userId: String,
    val fullName: String,
    val relationship: String,
    val phone: String,
    val email: String,
    val emergencyAlerts: Boolean,
    val createdAt: Long,
    val syncStatus: String = "Pending Sync"
)
