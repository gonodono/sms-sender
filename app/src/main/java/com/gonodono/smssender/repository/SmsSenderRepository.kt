package com.gonodono.smssender.repository

import android.content.Context
import android.provider.Telephony
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.gonodono.smssender.data.Message
import com.gonodono.smssender.data.Message.DeliveryStatus
import com.gonodono.smssender.data.Message.SendStatus
import com.gonodono.smssender.data.SmsSenderDatabase
import com.gonodono.smssender.sms.getSmsManager
import com.gonodono.smssender.sms.sendMessage
import com.gonodono.smssender.work.SmsSendWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SmsSenderRepository(
    private val context: Context,
    database: SmsSenderDatabase
) {
    private val messageDao = database.messageDao

    private val sendTaskDao = database.sendTaskDao

    val allMessages = messageDao.allMessages

    val latestSendTask = sendTaskDao.latestSendTask

    suspend fun queueMessagesAndSend(messages: List<Message>) {
        messageDao.insertMessages(messages)
        startImmediateSend(context)
    }

    suspend fun resetFailedAndRetry() {
        messageDao.resetFailedToQueued()
        startImmediateSend(context)
    }

    private fun startImmediateSend(context: Context) {
        val request = OneTimeWorkRequestBuilder<SmsSendWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("SmsSend", ExistingWorkPolicy.KEEP, request)
    }

    private var activeSendTask: ActiveSendTask? = null

    suspend fun doSend(context: Context, id: UUID): Boolean {
        val task = ActiveSendTask(id)
        sendTaskDao.insert(task)
        activeSendTask = task

        send(context, task)

        activeSendTask = null
        sendTaskDao.update(task)
        return task.succeeded
    }

    private suspend fun send(context: Context, task: ActiveSendTask) {
        try {
            suspendCancellableCoroutine { continuation ->
                val scope = CoroutineScope(Dispatchers.IO)
                val exceptionHandler = CoroutineExceptionHandler { _, e ->
                    scope.cancel()
                    continuation.resumeWithException(e)
                }
                scope.launch(exceptionHandler) {
                    val smsManager = getSmsManager(context)
                    messageDao.nextQueuedMessage
                        .distinctUntilChanged()
                        .transformWhile { message ->
                            task.checkForFatalSmsError()
                            if (message != null) {
                                emit(message)
                                true
                            } else {
                                false
                            }
                        }
                        .onEach {
                            task.resetForNextMessage()
                            sendMessage(context, smsManager, it)
                        }
                        .onCompletion { e ->
                            if (e == null) continuation.resume(Unit)
                        }
                        .collect()
                }
            }
            task.setError(null)
        } catch (e: Throwable) {
            task.setError(e)
        }
    }

    suspend fun handleSendResult(
        messageId: Int,
        resultCode: Int,
        isLastPart: Boolean
    ) {
        val task = activeSendTask ?: throw IllegalStateException("Zounds!")
        task.processResultCode(resultCode)
        if (isLastPart) {
            val status = when {
                task.hadSmsError -> SendStatus.Failed
                else -> SendStatus.Sent
            }
            messageDao.updateSendStatus(messageId, status)
        }
    }

    suspend fun handleDeliveryResult(messageId: Int, smsStatus: Int) {
        val status = when {
            smsStatus == Telephony.Sms.STATUS_COMPLETE -> DeliveryStatus.Complete
            smsStatus >= Telephony.Sms.STATUS_FAILED -> DeliveryStatus.Failed
            else -> DeliveryStatus.Pending
        }
        messageDao.updateDeliveryStatus(messageId, status)
    }

    // TESTING ONLY!!
    suspend fun checkForFakeDeliveryReport(address: String, body: String) =
        messageDao.checkForFakeDeliveryReport(address, body)
}