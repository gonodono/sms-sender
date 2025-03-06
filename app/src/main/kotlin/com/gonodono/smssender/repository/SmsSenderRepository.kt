package com.gonodono.smssender.repository

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import com.gonodono.smssender.database.DeliveryStatus
import com.gonodono.smssender.database.Message
import com.gonodono.smssender.database.SendStatus
import com.gonodono.smssender.database.SmsSenderDatabase
import com.gonodono.smssender.internal.log
import com.gonodono.smssender.sms.EXTRA_IS_LAST_PART
import com.gonodono.smssender.sms.SmsSendResult
import com.gonodono.smssender.sms.sendMessage
import com.gonodono.smssender.work.SmsSendWorker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
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

    val allMessages: Flow<List<Message>> = messageDao.allMessages

    suspend fun insertMessages(messages: List<Message>) =
        messageDao.insertMessages(messages)

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

    suspend fun doSend(): SendResult {
        fatalError = FatalError.None
        isSendCancelled = false

        // There's nothing to unregister or clean up, and we stop the flow with
        // an inline signal, so there's no need for suspendCancellableCoroutine.
        suspendCoroutine { continuation ->
            scope.launch { sendMessages(continuation) }
        }

        return when {
            fatalError.isError() -> SendResult.Error(fatalError)
            isSendCancelled -> SendResult.Cancelled
            else -> SendResult.Completed
        }
    }

    private suspend fun sendMessages(continuation: Continuation<Unit>) {
        messageDao.nextQueuedMessage
            .distinctUntilChanged()
            .onEach { delay(1000L) }  // <- Artificial delay. Demo only.
            .transformWhile { message ->
                when {
                    fatalError.isError() -> false
                    isSendCancelled -> false
                    message == null -> false
                    else -> true.also { emit(message) }
                }
            }
            .onEach { message ->
                val result = sendMessage(context, message)
                if (result is SmsSendResult.Error) {
                    setSendFailed(message, result)
                    if (result is SmsSendResult.FatalError) {
                        fatalError = FatalError(result.exception)
                    }
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

    // Setting fatalError will stop the flow before the next send.
    private val sendUpdateExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            log("Error updating send", throwable)
            fatalError = FatalError(throwable)
        }

    //endregion

    //region Used by SmsResultReceiver

    fun handleSendResult(
        intent: Intent,
        messageId: Int,
        resultCode: Int
    ) {
        scope.launch(sendUpdateExceptionHandler) {
            // RESULT_ERROR_NONE didn't exist in the original SMS API,
            // so Activity.RESULT_OK was used to indicate no send errors.
            val errorCode =
                if (resultCode == Activity.RESULT_OK) RESULT_ERROR_NONE
                else resultCode

            if (errorCode in FatalSmsErrors) fatalError = FatalError(errorCode)

            // We just track fatal errors until the last part arrives.
            val isLastPart = intent.getBooleanExtra(EXTRA_IS_LAST_PART, false)
            if (!isLastPart) return@launch

            val isError = errorCode != RESULT_ERROR_NONE
            val status = if (isError) SendStatus.Failed else SendStatus.Complete
            val error = if (isError) errorCodeToString(errorCode) else null

            messageDao.updateSend(messageId, status, error)
        }
    }

    // Delivery results only fire for the last message part.
    fun handleDeliveryResult(
        intent: Intent,
        messageId: Int
    ) {
        scope.launch(deliveryUpdateExceptionHandler) {
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
    }

    // Delivery results aren't vital to the flow, so we just log errors here.
    private val deliveryUpdateExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            log("Error updating delivery", throwable)
        }

    //endregion
}