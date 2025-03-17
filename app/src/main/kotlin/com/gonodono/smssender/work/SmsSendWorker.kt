package com.gonodono.smssender.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.gonodono.smssender.repository.SendResult
import com.gonodono.smssender.repository.SmsSenderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@HiltWorker
class SmsSendWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SmsSenderRepository
) : CoroutineWorker(context, workerParams) {

    // The demo always returns Success to avoid auto-rescheduling.
    override suspend fun doWork(): Result {
        val result = repository.doSend()
        val data = when (result) {
            SendResult.Completed -> Data.EMPTY
            SendResult.Cancelled -> workDataOf(CANCELLED_KEY to true)
            is SendResult.Error -> workDataOf(ERROR_KEY to result.toString())
        }
        return Result.success(data)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        ForegroundInfo(0, createNotification(applicationContext))

    companion object {

        private const val CANCELLED_KEY = "cancelled"
        private const val ERROR_KEY = "error"
        private const val WORK_NAME = "sendSms"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SmsSendWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        fun workerState(context: Context): Flow<WorkerState> =
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(WORK_NAME)
                .map { workInfos ->
                    val info = workInfos.firstOrNull()
                    val data = info?.outputData
                    WorkerState(
                        info?.state?.isFinished == false,
                        data?.getBoolean(CANCELLED_KEY, false) == true,
                        data?.getString(ERROR_KEY)
                    )
                }
    }
}