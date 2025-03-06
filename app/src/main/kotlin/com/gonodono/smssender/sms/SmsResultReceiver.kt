package com.gonodono.smssender.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gonodono.smssender.internal.logInvalidBroadcast
import com.gonodono.smssender.repository.SmsSenderRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SmsResultReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SmsSenderRepository

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.data?.fragment?.toIntOrNull() ?: return

        when (intent.action) {
            ACTION_SMS_SENT -> {
                repository.handleSendResult(intent, messageId, resultCode)
            }
            ACTION_SMS_DELIVERED -> {
                repository.handleDeliveryResult(intent, messageId)
            }
            else -> {
                logInvalidBroadcast(intent)
            }
        }
    }
}