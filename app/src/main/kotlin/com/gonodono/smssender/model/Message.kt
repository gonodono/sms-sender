package com.gonodono.smssender.model

import com.gonodono.smssender.database.MessageEntity

data class Message(
    val address: String,
    val body: String,
    val sendStatus: SendStatus? = null,
    val sendError: String? = null,
    val deliveryStatus: DeliveryStatus? = null,
    val deliveryError: String? = null,
    val id: Int = 0
)

fun Message.isQueued(): Boolean = sendStatus == SendStatus.Queued

fun Message.isFailed(): Boolean =
    sendStatus == SendStatus.Failed || deliveryStatus == DeliveryStatus.Failed

fun Message.toEntity(): MessageEntity =
    MessageEntity(
        id,
        address,
        body,
        sendStatus,
        sendError,
        deliveryStatus,
        deliveryError
    )