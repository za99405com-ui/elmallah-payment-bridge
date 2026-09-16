package com.elmallah.paymentbridge.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BridgeSyncCoordinator {
    private const val IMMEDIATE_WORK_NAME = "almallah_payment_sync_immediate"
    private const val PERIODIC_WORK_NAME = "almallah_payment_sync_retry"

    private val connectedConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun enqueueImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<PaymentSyncWorker>()
            .setConstraints(connectedConstraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    fun ensurePeriodicRetry(context: Context) {
        val request = PeriodicWorkRequestBuilder<PaymentSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connectedConstraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
