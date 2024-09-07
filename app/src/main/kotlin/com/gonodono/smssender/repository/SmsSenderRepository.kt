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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
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

    fun startImmediateSend() {
        val request = OneTimeWorkRequestBuilder<SmsSendWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("SmsSend", ExistingWorkPolicy.KEEP, request)
    }

    suspend fun resetFailedAndRetry() {
        messageDao.resetFailedToQueued()
        startImmediateSend()
    }

    fun tryCancelCurrentSend() {
        currentTask?.state = SendTask.State.Cancelled
    }

    private var currentTask: SendTask? = null

    private val smsErrors = SmsErrors(context)

    suspend fun doSend(id: UUID): Boolean {
        val task = SendTask(id).also { currentTask = it }
        sendTaskDao.insert(task)
        smsErrors.resetForNextTask()

        suspendCancellableCoroutine { continuation ->
            scope.launch {
                messageDao.nextQueuedMessage
                    .distinctUntilChanged()
                    .onEach { delay(1000L) } // <- Artificial delay. Demo only.
                    .takeWhile { msg -> checkStateAndSend(task, msg) }
                    .onCompletion { continuation.resume(Unit) }
                    .collect()
            }
        }

        currentTask = null
        sendTaskDao.update(task)
        return task.state == SendTask.State.Succeeded
    }

    private fun checkStateAndSend(
        task: SendTask,
        message: Message?
    ): Boolean = when {
        task.state == SendTask.State.Cancelled -> {
            // State is already set, so just stop.
            false
        }
        smsErrors.hadFatalSmsError -> {
            task.state = SendTask.State.Failed
            task.error = smsErrors.createFatalMessage()
            false
        }
        message == null -> {
            task.state = SendTask.State.Succeeded
            false
        }
        else -> sendMessage(task, message)
    }

    private fun sendMessage(
        task: SendTask,
        message: Message
    ): Boolean {
        smsErrors.resetForNextMessage()
        return when (val result = sendMessage(context, message)) {
            SendResult.Success -> true
            is SendResult.Error -> {
                task.state = SendTask.State.Failed
                task.error = result.toString()
                false
            }
        }
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