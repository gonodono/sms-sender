package com.gonodono.smssender.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import com.gonodono.smssender.EMULATOR_PORT
import com.gonodono.smssender.data.SmsSenderDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class FakeDeliveryReporter : BroadcastReceiver() {

    @Inject
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var database: SmsSenderDatabase

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) return

        doAsync(scope) {
            // Assumes everything will decode correctly in testing
            val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val body = parts.joinToString("") { it.displayMessageBody }

            val message = database.messageDao
                .checkForFakeDeliveryReport(EMULATOR_PORT, body)
                ?: return@doAsync

            sendFakeDeliveryReport(
                context,
                message.id,
                message.address,
                Telephony.Sms.STATUS_COMPLETE  // or _FAILED or _PENDING
            )
        }
    }
}

@Suppress("SameParameterValue")
private fun sendFakeDeliveryReport(
    context: Context,
    messageId: Int,
    address: String,
    status: Int
) {
    context.sendBroadcast(
        createDeliveryIntent(context, messageId)
            .putExtra("pdu", createStatusReportPdu(address, status))
            .putExtra("format", "3gpp")
    )
}

// Naively reversed from https://android.googlesource.com/platform/frameworks/base/+/ff3030932afbbed8532d4af832ffe4d474e4bb8b/telephony/java/com/android/internal/telephony/gsm/SmsMessage.java#1264
private fun createStatusReportPdu(
    address: String,
    status: Int
): ByteArray = ByteArrayOutputStream().apply {
    // SMSC address; must be non-empty
    write(PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength("0"))
    write(0x02)  // TP-Message-Type-Indicator; 2 == STATUS-REPORT
    write(0x00)  // TP-Message-Reference; 0..255, invisible at SDK level

    val bcd = PhoneNumberUtils.networkPortionToCalledPartyBCD(address)
    val padded = (bcd[bcd.size - 1].toInt() and 0xf0) == 0xf0
    write((bcd.size - 1) * 2 - if (padded) 1 else 0)
    write(bcd)

    val current = System.currentTimeMillis()
    write(createTimestamp(current))  // TP-Service-Center-Time-Stamp
    write(createTimestamp(current + 1000)) // TP-Discharge-Time
    write(status)
}.toByteArray()

// Adapted from https://android.googlesource.com/platform/frameworks/base/+/ff3030932afbbed8532d4af832ffe4d474e4bb8b/telephony/java/com/android/internal/telephony/gsm/SmsMessage.java#804
private fun createTimestamp(date: Long): ByteArray {
    val zonedDateTime =
        Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault())
    val localDateTime = zonedDateTime.toLocalDateTime()
    var timezoneOffset = zonedDateTime.offset.totalSeconds / 60 / 15
    val negativeOffset = timezoneOffset < 0
    if (negativeOffset) timezoneOffset *= -1

    val timestamp = ByteArray(7)
    val year = localDateTime.year.let { it - if (it > 2000) 2000 else 1900 }
    timestamp[0] = year.toBigEndianByte()
    timestamp[1] = localDateTime.monthValue.toBigEndianByte()
    timestamp[2] = localDateTime.dayOfMonth.toBigEndianByte()
    timestamp[3] = localDateTime.hour.toBigEndianByte()
    timestamp[4] = localDateTime.minute.toBigEndianByte()
    timestamp[5] = localDateTime.second.toBigEndianByte()
    timestamp[6] = timezoneOffset.toBigEndianByte()
    // ** INDEX CORRECTED FROM 0 IN SOURCE **
    if (negativeOffset) timestamp[6] = (timestamp[6].toInt() or 0x08).toByte()
    return timestamp
}

private fun Int.toBigEndianByte(): Byte =
    (this % 10 and 0x0F shl 4 or (this / 10 and 0x0F)).toByte()