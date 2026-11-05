
package com.example.safezone.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.safezone.data.SafeZoneDatabase
import com.example.safezone.data.SafetyCheckInRepository
import com.google.firebase.firestore.FirebaseFirestore

class SafetyCheckInSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database =
                SafeZoneDatabase.getDatabase(applicationContext)

            val repository = SafetyCheckInRepository(
                dao = database.safetyCheckInDao(),
                firestore = FirebaseFirestore.getInstance()
            )

            repository.syncPendingCheckIns()

            // Verify no pending records remain before reporting success.
            val pending =
                database.safetyCheckInDao().getPendingSyncCheckIns()

            if (pending.isEmpty()) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (exception: Exception) {
            Result.retry()
        }
    }
}
