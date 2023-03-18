package com.gonodono.smssender.repository

import android.annotation.SuppressLint
import android.app.Activity
import android.telephony.SmsManager
import com.gonodono.smssender.data.SendTask
import java.util.*


internal class ActiveSendTask(id: UUID) : SendTask(id, State.Running) {

    private var lastSmsError: Int? = null

    private var fatalSmsError: Int? = null

    fun resetForNextMessage() {
        lastSmsError = null
        fatalSmsError = null
    }

    fun checkForFatalSmsError() {
        val error = fatalSmsError ?: return
        throw IllegalStateException("SMS Error: $error")
    }

    fun processResultCode(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK) {
            lastSmsError = resultCode
            if (resultCode !in PossiblyIgnorableSmsErrors) {
                fatalSmsError = resultCode
            }
        }
    }

    val hadSmsError: Boolean get() = lastSmsError != null

    fun setError(e: Throwable?) {
        state = if (e == null) State.Succeeded else State.Failed
        error = e?.toString()
    }

    val succeeded: Boolean get() = state == State.Succeeded
}

// No problem if newer error ints checked on older versions
@SuppressLint("InlinedApi")
private val PossiblyIgnorableSmsErrors = intArrayOf(
    // These are just a few examples of ones you may want to use, if any. It's
    // not required to check this, and you could just fail on any error instead.
    // Docs: https://developer.android.com/reference/android/telephony/SmsManager#sendMultipartTextMessage(java.lang.String,%20java.lang.String,%20java.util.ArrayList%3Cjava.lang.String%3E,%20java.util.ArrayList%3Candroid.app.PendingIntent%3E,%20java.util.ArrayList%3Candroid.app.PendingIntent%3E)
    SmsManager.RESULT_ERROR_NULL_PDU,
    SmsManager.RESULT_ENCODING_ERROR,
    SmsManager.RESULT_INVALID_SMS_FORMAT
)