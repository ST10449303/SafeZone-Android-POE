
package com.example.safezone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safety_check_ins")
data class SafetyCheckInEntity(

    @PrimaryKey
    val localId: String,

    val userId: String,

    val trustedContact: String,

    val durationMinutes: Int,

    val locationSharing: Boolean,

    val status: String = "Active",

    val startedAt: Long,

    val expiresAt: Long,

    val completedAt: Long? = null,

    val cancelledAt: Long? = null,

    val expiredAt: Long? = null,

    val syncStatus: String = "Pending Sync",

    val firestoreDocumentId: String? = null
)
