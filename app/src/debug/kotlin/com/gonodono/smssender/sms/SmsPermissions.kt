package com.gonodono.smssender.sms

import android.Manifest

internal val SmsPermissions = arrayOf(
    Manifest.permission.SEND_SMS,
    Manifest.permission.RECEIVE_SMS
)