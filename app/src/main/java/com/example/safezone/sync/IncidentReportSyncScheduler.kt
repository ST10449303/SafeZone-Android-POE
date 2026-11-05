
package com.example.safezone.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object IncidentReportSyncScheduler {

    fun schedule(context: Context, userId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<IncidentReportSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf("userId" to userId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "incident_report_sync_$userId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
