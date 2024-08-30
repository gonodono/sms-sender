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
import com.gonodono.smssender.data.SendTask
import com.gonodono.smssender.data.SmsSenderDatabase
import com.gonodono.smssender.sms.SendResult
import com.gonodono.smssender.sms.sendMessage
import com.gonodono.smssender.work.SmsSendWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

class SmsSenderRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    database: SmsSenderDatabase
) {
    private val messageDao = database.messageDao

    private val sendTaskDao = database.sendTaskDao

    val allMessages: Flow<List<Message>> = messageDao.allMessages

    val latestSendTask: Flow<SendTask?> = sendTaskDao.latestSendTask

    suspend fun insertMessagesAndSend(messages: List<Message>) {
        messageDao.insertMessages(messages)
        startImmediateSend()
    }

    suspend fun resetFailedAndRetry() {
        messageDao.resetFailedToQueued()
        startImmediateSend()
    }

    private fun startImmediateSend() {
        val request = OneTimeWorkRequestBuilder<SmsSendWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("SmsSend", ExistingWorkPolicy.KEEP, request)
    }

    private val smsErrors = SmsErrors(context)

    suspend fun doSend(id: UUID): Boolean {
        val task = SendTask(id)
        sendTaskDao.insert(task)
        smsErrors.resetForNextTask()

        suspendCancellableCoroutine { continuation ->
            scope.launch {
                messageDao.nextQueuedMessage
                    .distinctUntilChanged()
                    .takeWhile { message ->
                        when {
                            smsErrors.hadFatalSmsError -> {
                                task.state = SendTask.State.Failed
                                task.error = smsErrors.createFatalMessage()
                                false
                            }
                            message == null -> {
                                task.state = SendTask.State.Succeeded
                                false
                            }
                            else -> {
                                smsErrors.resetForNextMessage()
                                val result = sendMessage(context, message)
                                when (result) {
                                    SendResult.Success -> true
                                    is SendResult.Error -> {
                                        task.state = SendTask.State.Failed
                                        task.error = result.toString()
                                        false
                                    }
                                }
                            }
                        }
                    }
                    .onCompletion { continuation.resume(Unit) }
                    .collect()
            }
        }

        sendTaskDao.update(task)
        return task.state == SendTask.State.Succeeded
    }

    suspend fun handleSendResult(
        messageId: Int,
        resultCode: Int,
        isLastPart: Boolean
    ) {
        smsErrors.processResultCode(resultCode)
        if (isLastPart) {
            val status = when {
                smsErrors.hadSmsError -> SendStatus.Failed
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
}