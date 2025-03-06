package com.gonodono.smssender.sms

import android.Manifest.permission.RECEIVE_SMS
import android.Manifest.permission.SEND_SMS
import android.os.Build
import com.gonodono.smssender.BuildConfig

internal val SmsPermissions =
    if (Build.VERSION.SDK_INT <= BuildConfig.FAKE_DELIVERY_REPORTS_MAX_SDK) {
        arrayOf(SEND_SMS, RECEIVE_SMS)
    } else {
        arrayOf(SEND_SMS)
    }