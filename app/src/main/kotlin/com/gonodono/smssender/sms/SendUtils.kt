package com.gonodono.smssender.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import com.gonodono.smssender.BuildConfig
import com.gonodono.smssender.database.Message
import com.gonodono.smssender.internal.nameAndMessage

internal const val ACTION_SMS_SENT =
    "${BuildConfig.APPLICATION_ID}.action.SMS_SENT"

internal const val ACTION_SMS_DELIVERED =
    "${BuildConfig.APPLICATION_ID}.action.SMS_DELIVERED"

internal const val EXTRA_IS_LAST_PART =
    "${BuildConfig.APPLICATION_ID}.extra.IS_LAST_PART"

internal fun Context.getSmsManager(): SmsManager =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(SmsManager::class.java)
    } else {
        @Suppress("DEPRECATION") SmsManager.getDefault()
    }

internal sealed interface SmsSendResult {

    data object Success : SmsSendResult

    sealed interface Error : SmsSendResult

    data class DataError(val exception: IllegalArgumentException) : Error {
        override fun toString(): String = exception.nameAndMessage()
    }

    data class FatalError(val exception: Exception) : Error {
        override fun toString(): String = exception.nameAndMessage()
    }
}

internal fun sendMessage(context: Context, message: Message): SmsSendResult {
    val manager = context.getSmsManager()
    val parts = manager.divideMessage(message.body)
    val sentIntents = arrayListOf<PendingIntent>()
    val deliveryIntents = arrayListOf<PendingIntent?>()

    for (partIndex in parts.indices) {
        val isLastPart = partIndex == parts.lastIndex

        val sent = createSentIntent(message.id, context, isLastPart)
        sentIntents +=
            PendingIntent.getBroadcast(context, partIndex, sent, RESULT_FLAGS)

        deliveryIntents +=
            if (isLastPart) {
                val delivery = createDeliveryIntent(context, message.id)
                PendingIntent.getBroadcast(context, 0, delivery, RESULT_FLAGS)
            } else {
                null
            }
    }

    return try {
        manager.sendMultipartTextMessage(
            message.address,
            null,
            parts,
            sentIntents,
            deliveryIntents
        )
        SmsSendResult.Success
    } catch (e: IllegalArgumentException) {
        SmsSendResult.DataError(e)
    } catch (e: Exception) {
        SmsSendResult.FatalError(e)
    }
}

private fun createSentIntent(
    messageId: Int,
    context: Context,
    isLastPart: Boolean
) = createResultIntent(context, messageId)
    .setAction(ACTION_SMS_SENT)
    .putExtra(EXTRA_IS_LAST_PART, isLastPart)

internal fun createDeliveryIntent(
    context: Context,
    messageId: Int
) = createResultIntent(context, messageId)
    .setAction(ACTION_SMS_DELIVERED)

private fun createResultIntent(context: Context, messageId: Int): Intent {
    val id = messageId.toString()
    val uri = Uri.fromParts("app", BuildConfig.APPLICATION_ID, id)
    return Intent(null, uri, context, SmsResultReceiver::class.java)
}

private val RESULT_FLAGS =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
    } else {
        PendingIntent.FLAG_ONE_SHOT
    }