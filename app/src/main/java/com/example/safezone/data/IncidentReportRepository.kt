
package com.example.safezone.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class IncidentReportRepository(
    private val dao: IncidentReportDao,
    private val firestore: FirebaseFirestore
) {

    suspend fun saveReportLocally(
        userId: String,
        incidentType: String,
        description: String,
        location: String,
        latitude: Double,
        longitude: Double,
        dateTime: String,
        photoEvidence: String,
        videoEvidence: String,
        fileEvidence: String,
        photoName: String,
        videoName: String,
        fileName: String,
        createdAt: Long
    ): IncidentReportEntity {

        val report = IncidentReportEntity(
            localId = UUID.randomUUID().toString(),
            userId = userId,
            incidentType = incidentType,
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude,
            dateTime = dateTime,
            status = "Submitted",
            photoEvidence = photoEvidence,
            videoEvidence = videoEvidence,
            fileEvidence = fileEvidence,
            photoName = photoName,
            videoName = videoName,
            fileName = fileName,
            createdAt = createdAt,
            syncStatus = "Pending Sync"
        )

        // Save to the phone before attempting network access.
        dao.insert(report)

        return report
    }

    suspend fun syncPendingReports(userId: String) {
        val pendingReports = dao.getPendingSyncReports(userId)

        for (report in pendingReports) {
            try {
                val reportData = hashMapOf<String, Any>(
                    "localId" to report.localId,
                    "userId" to report.userId,
                    "incidentType" to report.incidentType,
                    "description" to report.description,
                    "location" to report.location,
                    "latitude" to report.latitude,
                    "longitude" to report.longitude,
                    "dateTime" to report.dateTime,
                    "status" to report.status,
                    "photoEvidence" to report.photoEvidence,
                    "videoEvidence" to report.videoEvidence,
                    "fileEvidence" to report.fileEvidence,
                    "photoName" to report.photoName,
                    "videoName" to report.videoName,
                    "fileName" to report.fileName,
                    "createdAt" to report.createdAt
                )

                // Stable document ID prevents duplicate reports on retries.
                firestore.collection("IncidentReports")
                    .document(report.localId)
                    .set(reportData)
                    .await()

                dao.updateSyncStatus(
                    localId = report.localId,
                    userId = userId,
                    syncStatus = "Synced"
                )

            } catch (_: Exception) {
                // Leave the report pending so it can be retried.
            }
        }
    }
}
