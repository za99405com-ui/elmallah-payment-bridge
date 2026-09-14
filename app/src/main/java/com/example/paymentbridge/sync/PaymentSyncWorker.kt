package com.example.paymentbridge.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.paymentbridge.data.PaymentDatabase
import com.example.paymentbridge.data.PaymentRepository
import java.util.concurrent.TimeUnit

class PaymentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = PaymentDatabase.getInstance(applicationContext)
        val dao = database.paymentEventDao()
        val repository = PaymentRepository(applicationContext, dao)

        val pending = dao.getPendingUploads()
        if (pending.isEmpty()) {
            return Result.success()
        }

        var anyFailed = false
        for (entity in pending) {
            val result = repository.uploadEvent(entity)
            if (result.isFailure) {
                anyFailed = true
            }
        }

        return if (anyFailed) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}

object SyncScheduler {

    private const val UNIQUE_WORK_NAME = "almallah_payment_sync_retry"

    fun scheduleRetry(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<PaymentSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                15,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun syncNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<PaymentSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
