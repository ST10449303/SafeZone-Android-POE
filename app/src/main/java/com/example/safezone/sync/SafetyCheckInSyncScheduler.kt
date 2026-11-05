
package com.example.safezone.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SafetyCheckInSyncScheduler {

    private const val PERIODIC_WORK_NAME =
        "safezone_safety_checkin_sync"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Retry pending records when network connectivity returns.
        val immediateWork =
            OneTimeWorkRequestBuilder<SafetyCheckInSyncWorker>()
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context)
            .enqueue(immediateWork)

        // Keep checking periodically as a backup.
        val periodicWork =
            PeriodicWorkRequestBuilder<SafetyCheckInSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
    }
}
