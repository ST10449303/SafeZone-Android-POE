
package com.example.safezone.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.safezone.data.SafeZoneDatabase
import com.example.safezone.data.IncidentReportRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class IncidentReportSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId")
            ?: return Result.failure()

        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser

            // Never sync one user's reports as another user.
            if (currentUser == null || currentUser.uid != userId) {
                return Result.retry()
            }

            val dao = SafeZoneDatabase
                .getDatabase(applicationContext)
                .incidentReportDao()

            val repository = IncidentReportRepository(
                dao = dao,
                firestore = FirebaseFirestore.getInstance()
            )

            repository.syncPendingReports(userId)

            val remaining = dao.getPendingSyncReports(userId)

            if (remaining.isEmpty()) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
