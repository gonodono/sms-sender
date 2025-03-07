package com.gonodono.smssender.repository

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import com.gonodono.smssender.database.MessageEntity
import com.gonodono.smssender.database.SmsSenderDatabase
import com.gonodono.smssender.database.toModel
import com.gonodono.smssender.internal.log
import com.gonodono.smssender.model.DeliveryStatus
import com.gonodono.smssender.model.Message
import com.gonodono.smssender.model.SendStatus
import com.gonodono.smssender.model.toEntity
import com.gonodono.smssender.sms.ACTION_SMS_DELIVERED
import com.gonodono.smssender.sms.ACTION_SMS_SENT
import com.gonodono.smssender.sms.EXTRA_IS_LAST_PART
import com.gonodono.smssender.sms.SmsSendResult
import com.gonodono.smssender.sms.sendMessage
import com.gonodono.smssender.work.SmsSendWorker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SmsSenderRepository(
    private val context: Context,
    database: SmsSenderDatabase,
    dispatcher: CoroutineDispatcher
) {
    //region Used by MainViewModel

    private val messageDao = database.messageDao

    val messages: Flow<List<Message>> =
        messageDao.messages.map { it.map(MessageEntity::toModel) }

    suspend fun insertMessages(messages: List<Message>) =
        messageDao.insertMessages(messages.map(Message::toEntity))

    fun startSend() = SmsSendWorker.enqueue(context)

    private var isSendCancelled: Boolean = false

    fun cancelSend() {
        isSendCancelled = true
    }

    suspend fun resetFailed() = messageDao.resetFailedToQueued()

    //endregion

    //region Used by SmsSendWorker

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var fatalError: FatalError = FatalError.None

    private var sendJob: Job? = null

    suspend fun doSend(): SendResult {
        fatalError = FatalError.None
        isSendCancelled = false

        suspendCoroutine { continuation ->
            sendJob = scope.launch { sendMessages(continuation) }
        }
        sendJob = null

        return when {
            fatalError.isError() -> SendResult.Error(fatalError)
            isSendCancelled -> SendResult.Cancelled
            else -> SendResult.Completed
        }
    }

    private suspend fun sendMessages(continuation: Continuation<Unit>) {
        // This flow will run each time the oldest queued message changes. If
        // none of the stop conditions are met in transformWhile, the message
        // is then sent. Subsequent updates to the message's status then keep
        // the flow going until all of the queued messages are processed.
        messageDao.nextQueuedMessage
            .distinctUntilChanged()
            .onEach { delay(1000L) }  // <- Artificial delay. Demo only.
            .transformWhile { message ->
                when {
                    fatalError.isError() -> false
                    isSendCancelled -> false
                    message == null -> false
                    else -> true.also { emit(message.toModel()) }
                }
            }
            .onEach { message ->
                val result = sendMessage(context, message)
                if (result is SmsSendResult.Error) {
                    if (result is SmsSendResult.FatalError) {
                        fatalError = FatalError(result.exception)
                    }
                    setSendFailed(message, result)
                }
            }
            .onCompletion { continuation.resume(Unit) }
            .collect()
    }

    private fun setSendFailed(
        message: Message,
        result: SmsSendResult.Error
    ) {
        scope.launch(sendUpdateExceptionHandler) {
            val id = message.id
            val error = result.toString()
            messageDao.updateSend(id, SendStatus.Failed, error)
        }
    }

    private val sendUpdateExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            log("Error updating send", throwable)
            fatalError = FatalError(throwable)
            sendJob?.cancel()
        }

    //endregion

    //region Used by SmsResultReceiver

    // Returns true if intent holds a valid result, false otherwise.
    fun handleSmsResult(intent: Intent, resultCode: Int): Boolean {
        val messageId = intent.data?.fragment?.toIntOrNull()
        if (messageId == null) return false

        val isSentResult = intent.action == ACTION_SMS_SENT
        val isDeliveryResult = intent.action == ACTION_SMS_DELIVERED
        if (!isSentResult && !isDeliveryResult) return false

        if (isSentResult) {
            handleSendResult(intent, messageId, resultCode)
        } else {
            handleDeliveryResult(intent, messageId)
        }
        return true
    }

    private fun handleSendResult(
        intent: Intent,
        messageId: Int,
        resultCode: Int
    ) = scope.launch(sendUpdateExceptionHandler) {
        // RESULT_ERROR_NONE didn't exist in the original SMS API,
        // so Activity.RESULT_OK was used to indicate no send errors.
        val errorCode =
            if (resultCode == Activity.RESULT_OK) RESULT_ERROR_NONE
            else resultCode

        val isFatalError = errorCode in FatalSmsErrors
        if (isFatalError) fatalError = FatalError(errorCode)

        val isLastPart = intent.getBooleanExtra(EXTRA_IS_LAST_PART, false)
        if (!isLastPart && !isFatalError) return@launch

        val isError = errorCode != RESULT_ERROR_NONE
        val status = if (isError) SendStatus.Failed else SendStatus.Complete
        val error = if (isError) errorCodeToString(errorCode) else null

        messageDao.updateSend(messageId, status, error)
    }

    // Delivery results only fire for the last message part.
    private fun handleDeliveryResult(
        intent: Intent,
        messageId: Int
    ) = scope.launch(deliveryUpdateExceptionHandler) {
        // Status must be read from the SmsMessage extra, not resultCode.
        val pdu = intent.getByteArrayExtra("pdu")
        val format = intent.getStringExtra("format")
        val message = SmsMessage.createFromPdu(pdu, format)
        val smsStatus = message?.status ?: Sms.STATUS_NONE

        val status = when {
            smsStatus == Sms.STATUS_COMPLETE -> DeliveryStatus.Complete
            smsStatus >= Sms.STATUS_FAILED -> DeliveryStatus.Failed
            else -> DeliveryStatus.Pending
        }
        // Specific errors can be parsed out of the status int, if needed,
        // but we usually don't care: https://stackoverflow.com/a/33240109.
        val error =
            if (status == DeliveryStatus.Failed) smsStatus.toString()
            else null

        messageDao.updateDelivery(messageId, status, error)
    }

    // Delivery results aren't vital to the flow, so we just log errors here.
    private val deliveryUpdateExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            log("Error updating delivery", throwable)
        }

    //endregion
}