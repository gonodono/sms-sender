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
        val isValidResult = repository.handleSmsResult(intent, resultCode)
        if (!isValidResult) logInvalidBroadcast(intent)
    }
}