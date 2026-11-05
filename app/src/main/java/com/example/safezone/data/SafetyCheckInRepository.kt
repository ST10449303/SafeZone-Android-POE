
package com.example.safezone.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SafetyCheckInRepository(
    private val dao: SafetyCheckInDao,
    private val firestore: FirebaseFirestore
) {

    /**
     * Save the check-in locally before attempting any network operation.
     */
    suspend fun saveCheckInLocally(
        userId: String,
        trustedContact: String,
        durationMinutes: Int,
        locationSharing: Boolean,
        startedAt: Long,
        expiresAt: Long
    ): SafetyCheckInEntity {

        val checkIn = SafetyCheckInEntity(
            localId = UUID.randomUUID().toString(),
            userId = userId,
            trustedContact = trustedContact,
            durationMinutes = durationMinutes,
            locationSharing = locationSharing,
            status = "Active",
            startedAt = startedAt,
            expiresAt = expiresAt,
            syncStatus = "Pending Sync"
        )

        dao.insert(checkIn)
        return checkIn
    }

    /**
     * Update the status locally, so this operation works offline.
     */
    suspend fun updateStatusLocally(
        localId: String,
        status: String
    ) {
        val completedAt =
            if (status == "Completed") System.currentTimeMillis() else null

        val cancelledAt =
            if (status == "Cancelled") System.currentTimeMillis() else null

        val expiredAt =
            if (status == "Expired") System.currentTimeMillis() else null

        dao.updateCheckInStatus(
            localId = localId,
            status = status,
            completedAt = completedAt,
            cancelledAt = cancelledAt,
            expiredAt = expiredAt
        )
    }

    /**
     * Upload pending check-ins, including their latest status.
     */
    suspend fun syncPendingCheckIns() {
        val pendingCheckIns = dao.getPendingSyncCheckIns()

        for (checkIn in pendingCheckIns) {
            try {
                val checkInData = hashMapOf<String, Any>(
                    "userId" to checkIn.userId,
                    "trustedContact" to checkIn.trustedContact,
                    "durationMinutes" to checkIn.durationMinutes,
                    "locationSharing" to checkIn.locationSharing,
                    "status" to checkIn.status,
                    "startedAt" to checkIn.startedAt,
                    "expiresAt" to checkIn.expiresAt,
                    "localId" to checkIn.localId
                )

                checkIn.completedAt?.let {
                    checkInData["completedAt"] = it
                }

                checkIn.cancelledAt?.let {
                    checkInData["cancelledAt"] = it
                }

                checkIn.expiredAt?.let {
                    checkInData["expiredAt"] = it
                }

                val documentReference =
                    if (checkIn.firestoreDocumentId.isNullOrBlank()) {
                        firestore.collection("SafetyCheckIns")
                            .document(checkIn.localId)
                    } else {
                        firestore.collection("SafetyCheckIns")
                            .document(checkIn.firestoreDocumentId)
                    }

                documentReference.set(checkInData).await()

                dao.updateSyncStatus(
                    localId = checkIn.localId,
                    syncStatus = "Synced",
                    documentId = documentReference.id
                )
            } catch (_: Exception) {
                // Keep the record pending for another attempt.
            }
        }
    }
}
