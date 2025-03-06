package com.gonodono.smssender.internal

import android.content.BroadcastReceiver
import android.content.Intent
import android.util.Log
import com.gonodono.smssender.BuildConfig

internal const val TAG = "sms-sender"

internal fun log(message: String, cause: Throwable? = null) {
    if (BuildConfig.DEBUG) Log.d(TAG, message, cause)
}

internal fun BroadcastReceiver.logInvalidBroadcast(intent: Intent) =
    log("Invalid broadcast to ${javaClass.simpleName}: $intent")