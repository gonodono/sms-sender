package com.gonodono.smssender.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    val address: String,
    val body: String,
    @ColumnInfo(name = "send_status")
    val sendStatus: SendStatus? = null,
    @ColumnInfo(name = "send_error")
    val sendError: String? = null,
    @ColumnInfo(name = "delivery_status")
    val deliveryStatus: DeliveryStatus? = null,
    @ColumnInfo(name = "delivery_error")
    val deliveryError: String? = null,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)