package com.gonodono.smssender.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gonodono.smssender.sms.SmsPermissions

@Composable
fun PermissionsRequest(onGranted: () -> Unit) {
    var askedAndDenied by remember { mutableStateOf(false) }
    TextBox("Permissions ${if (askedAndDenied) "denied" else "required"}")

    val request = rememberLauncherForActivityResult(Contract) { grants ->
        if (grants.all { it.value }) onGranted() else askedAndDenied = true
    }
    LaunchedEffect(Unit) { request.launch(SmsPermissions) }
}

private val Contract = ActivityResultContracts.RequestMultiplePermissions()