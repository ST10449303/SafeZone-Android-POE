
package com.example.safezone

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object EmergencyRequestSyncScheduler {

    fun schedule(context: Context, userId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<EmergencyRequestSyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    EmergencyRequestSyncWorker.KEY_USER_ID to userId
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "emergency_request_sync_$userId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
