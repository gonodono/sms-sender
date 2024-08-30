package com.gonodono.smssender.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import com.gonodono.smssender.BuildConfig
import com.gonodono.smssender.data.Message

internal const val ACTION_SMS_SENT =
    "${BuildConfig.APPLICATION_ID}.action.SMS_SENT"

internal const val ACTION_SMS_DELIVERED =
    "${BuildConfig.APPLICATION_ID}.action.SMS_DELIVERED"

internal const val EXTRA_IS_LAST_PART =
    "${BuildConfig.APPLICATION_ID}.extra.IS_LAST_PART"

internal fun Context.getSmsManager(): SmsManager = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        getSystemService(SmsManager::class.java)
    }
    else -> @Suppress("DEPRECATION") SmsManager.getDefault()
}

internal sealed class SendResult {
    data object Success : SendResult()
    data class Error(val e: Throwable) : SendResult() {
        override fun toString() = "${e.javaClass.simpleName}: ${e.message}"
    }
}

internal fun sendMessage(context: Context, message: Message): SendResult {
    val manager = context.getSmsManager()
    val parts = manager.divideMessage(message.body)
    val sendIntents = arrayListOf<PendingIntent>()
    val deliveryIntents = arrayListOf<PendingIntent?>()

    for (partNumber in 1..parts.size) {
        val isLastPart = partNumber == parts.size
        sendIntents += PendingIntent.getBroadcast(
            context,
            partNumber,
            createSendIntent(message.id, context, isLastPart),
            RESULT_FLAGS
        )
        deliveryIntents += if (isLastPart) {
            PendingIntent.getBroadcast(
                context,
                0,
                createDeliveryIntent(context, message.id),
                RESULT_FLAGS
            )
        } else null
    }

    return try {
        manager.sendMultipartTextMessage(
            message.address,
            null,
            parts,
            sendIntents,
            deliveryIntents
        )
        SendResult.Success
    } catch (e: Exception) {
        SendResult.Error(e)
    }
}

private fun createSendIntent(
    messageId: Int,
    context: Context,
    isLastPart: Boolean
) = createResultIntent(context, messageId)
    .setAction(ACTION_SMS_SENT)
    .putExtra(EXTRA_IS_LAST_PART, isLastPart)

// internal for use in fake delivery reporting
internal fun createDeliveryIntent(
    context: Context,
    messageId: Int
) = createResultIntent(context, messageId)
    .setAction(ACTION_SMS_DELIVERED)

private fun createResultIntent(
    context: Context,
    messageId: Int
) = Intent(
    null,
    Uri.fromParts("app", BuildConfig.APPLICATION_ID, messageId.toString()),
    context,
    SmsResultReceiver::class.java
)

private val RESULT_FLAGS =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
    } else {
        PendingIntent.FLAG_ONE_SHOT
    }