
package com.example.safezone.data

import android.content.Context
import com.example.safezone.EmergencyRequestSyncScheduler
import java.util.UUID

class EmergencyRequestRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = SafeZoneDatabase
        .getDatabase(appContext)
        .emergencyRequestDao()

    suspend fun saveRequest(
        userId: String,
        assistanceType: String
    ): String {
        val requestId = UUID.randomUUID().toString()

        val request = EmergencyRequestEntity(
            requestId = requestId,
            userId = userId,
            assistanceType = assistanceType,
            status = "Requested",
            createdAt = System.currentTimeMillis(),
            syncStatus = "Pending Sync"
        )

        // Save on the phone first, even without internet.
        dao.insert(request)

        // Attempt synchronisation when a network is available.
        EmergencyRequestSyncScheduler.schedule(appContext, userId)

        return requestId
    }
}
