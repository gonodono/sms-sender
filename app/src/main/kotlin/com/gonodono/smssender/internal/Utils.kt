package com.gonodono.smssender.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import com.gonodono.smssender.sms.SmsPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

internal fun Context.hasSmsPermissions(): Boolean =
    SmsPermissions.all { checkSelfPermission(it) == PERMISSION_GRANTED }

internal fun BroadcastReceiver.doAsync(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    onError: (suspend (Exception) -> Unit)? = null,
    block: suspend CoroutineScope.(BroadcastReceiver.PendingResult) -> Unit
) {
    val scope = CoroutineScope(coroutineContext)
    val pendingResult = goAsync()
    scope.launch {
        try {
            try {
                coroutineScope { block(pendingResult) }
            } catch (_: CancellationException) {
                // No rethrow. scope is cancelled anyway.
            } catch (e: Exception) {
                onError?.invoke(e)
            } finally {
                scope.cancel()
            }
        } finally {
            pendingResult.finish()
        }
    }
}

internal fun Throwable.nameAndMessage(): String =
    "${javaClass.simpleName}: $message"