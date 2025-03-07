package com.gonodono.smssender.repository

import android.annotation.SuppressLint
import android.telephony.SmsManager

// Value of SmsManager.RESULT_ERROR_NONE, not available until API level 30.
internal const val RESULT_ERROR_NONE = 0

// You may wish to inspect this selection to adjust it for your specific setup
// (or just to double-check that I got all that I should've). The full list is
// available in the docs for SmsManager.
// https://developer.android.com/reference/android/telephony/SmsManager#sendMultipartTextMessage(java.lang.String,%20java.lang.String,%20java.util.ArrayList%3Cjava.lang.String%3E,%20java.util.ArrayList%3Candroid.app.PendingIntent%3E,%20java.util.ArrayList%3Candroid.app.PendingIntent%3E)
@SuppressLint("InlinedApi")  // <- No issue if new ints checked on old versions.
internal val FatalSmsErrors = intArrayOf(
    SmsManager.RESULT_ERROR_GENERIC_FAILURE,
    SmsManager.RESULT_ERROR_LIMIT_EXCEEDED,
    SmsManager.RESULT_ERROR_NO_SERVICE,
    SmsManager.RESULT_ERROR_RADIO_OFF,
    SmsManager.RESULT_INTERNAL_ERROR,
    SmsManager.RESULT_INVALID_SMSC_ADDRESS,
    SmsManager.RESULT_MODEM_ERROR,
    SmsManager.RESULT_NO_MEMORY,
    SmsManager.RESULT_NO_RESOURCES,
    SmsManager.RESULT_RADIO_NOT_AVAILABLE,
    SmsManager.RESULT_SMS_BLOCKED_DURING_EMERGENCY,
    SmsManager.RESULT_SYSTEM_ERROR,
    SmsManager.RESULT_UNEXPECTED_EVENT_STOP_SENDING,
    SmsManager.RESULT_RIL_ACCESS_BARRED,
    SmsManager.RESULT_RIL_BLOCKED_DUE_TO_CALL,
    SmsManager.RESULT_RIL_INTERNAL_ERR,
    SmsManager.RESULT_RIL_INVALID_MODEM_STATE,
    SmsManager.RESULT_RIL_INVALID_SMSC_ADDRESS,
    SmsManager.RESULT_RIL_INVALID_STATE,
    SmsManager.RESULT_RIL_MODEM_ERR,
    SmsManager.RESULT_RIL_NETWORK_NOT_READY,
    SmsManager.RESULT_RIL_NO_MEMORY,
    SmsManager.RESULT_RIL_NO_RESOURCES,
    SmsManager.RESULT_RIL_OPERATION_NOT_ALLOWED,
    SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE,
    SmsManager.RESULT_RIL_REQUEST_RATE_LIMITED,
    SmsManager.RESULT_RIL_SIMULTANEOUS_SMS_AND_CALL_NOT_ALLOWED,
    SmsManager.RESULT_RIL_SIM_ABSENT,
    SmsManager.RESULT_RIL_SYSTEM_ERR
)

// This is 'cause I'm lazy and it's just a demo. If you use this function in a
// real app, you'll probably want to create a list of actual string literals.
internal fun errorCodeToString(value: Int): String {
    // This field doesn't exist on API levels < 30.
    if (value == RESULT_ERROR_NONE) return "RESULT_ERROR_NONE"

    val name = try {
        val errors: Map<Int?, String> =
            errorMap ?: SmsManager::class.java
                .declaredFields
                .filter { it.name.startsWith("RESULT_") }
                .associate { it.get(null) as? Int to it.name }
                .also { errorMap = it }
        errors[value]
    } catch (_: Exception) {
        null
    }
    return name ?: "Unknown error: $value"
}

private var errorMap: Map<Int?, String>? = null