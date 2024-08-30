package com.gonodono.smssender.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import com.gonodono.smssender.repository.SmsSenderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@AndroidEntryPoint
class SmsResultReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var repository: SmsSenderRepository

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.data?.fragment?.toIntOrNull() ?: return

        doAsync(scope) { pendingResult ->
            when (intent.action) {
                ACTION_SMS_SENT -> {
                    repository.handleSendResult(
                        messageId,
                        pendingResult.resultCode,
                        intent.getBooleanExtra(EXTRA_IS_LAST_PART, false)
                    )
                }
                ACTION_SMS_DELIVERED -> {
                    val message = SmsMessage.createFromPdu(
                        intent.getByteArrayExtra("pdu"),
                        intent.getStringExtra("format")
                    )
                    message?.let { msg ->
                        repository.handleDeliveryResult(messageId, msg.status)
                    }
                }
            }
        }
    }
}

internal fun BroadcastReceiver.doAsync(
    scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    block: suspend (BroadcastReceiver.PendingResult) -> Unit
) {
    val pendingResult = goAsync()
    scope.launch {
        try {
            block(pendingResult)
        } catch (e: CancellationException) {
            throw e
        } finally {
            pendingResult.finish()
        }
    }
}