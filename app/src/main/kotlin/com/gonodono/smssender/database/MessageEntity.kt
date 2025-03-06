package com.gonodono.smssender.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gonodono.smssender.model.DeliveryStatus
import com.gonodono.smssender.model.Message
import com.gonodono.smssender.model.SendStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val address: String,
    val body: String,
    @ColumnInfo(name = "send_status")
    val sendStatus: SendStatus?,
    @ColumnInfo(name = "send_error")
    val sendError: String?,
    @ColumnInfo(name = "delivery_status")
    val deliveryStatus: DeliveryStatus?,
    @ColumnInfo(name = "delivery_error")
    val deliveryError: String?
)

fun MessageEntity.toModel(): Message =
    Message(
        address,
        body,
        sendStatus,
        sendError,
        deliveryStatus,
        deliveryError,
        id
    )