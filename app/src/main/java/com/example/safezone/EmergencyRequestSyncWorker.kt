
package com.example.safezone

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.safezone.data.SafeZoneDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class EmergencyRequestSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_USER_ID)
            ?: return Result.failure()

        // Never sync another user's requests.
        if (FirebaseAuth.getInstance().currentUser?.uid != userId) {
            return Result.retry()
        }

        val dao = SafeZoneDatabase
            .getDatabase(applicationContext)
            .emergencyRequestDao()

        val pendingRequests = dao.getPendingRequests(userId)
        var failed = false

        for (request in pendingRequests) {
            try {
                val data = hashMapOf(
                    "requestId" to request.requestId,
                    "userId" to request.userId,
                    "assistanceType" to request.assistanceType,
                    "status" to request.status,
                    "createdAt" to request.createdAt,
                    "syncStatus" to "Synced"
                )

                // Stable document ID prevents duplicate requests on retry.
                FirebaseFirestore.getInstance()
                    .collection("EmergencyRequests")
                    .document(request.requestId)
                    .set(data)
                    .await()

                dao.updateSyncStatus(
                    requestId = request.requestId,
                    userId = userId,
                    newStatus = "Synced"
                )
            } catch (e: Exception) {
                failed = true
            }
        }

        return if (failed) Result.retry() else Result.success()
    }

    companion object {
        const val KEY_USER_ID = "emergency_request_user_id"
    }
}
