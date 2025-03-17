package com.gonodono.smssender.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gonodono.smssender.model.DeliveryStatus
import com.gonodono.smssender.model.SendStatus
import kotlinx.coroutines.flow.Flow

// This DAO has everything necessary for normal use. However, the fake delivery
// reports require an additional feature, so the release build simply extends
// this base, but the debug version adds another query for the fake reports.

@Dao
interface BaseMessageDao {

    @get:Query(
        """
        SELECT * FROM messages
        """
    )
    val messages: Flow<List<MessageEntity>>

    @get:Query(
        """
        SELECT * FROM messages
        WHERE send_status='Queued'
        ORDER BY id ASC
        LIMIT 1
        """
    )
    val nextQueuedMessage: Flow<MessageEntity?>

    @Insert
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query(
        """
        UPDATE messages
        SET send_status=:sendStatus, send_error=:sendError
        WHERE id=:id
        """
    )
    suspend fun updateSendStatus(
        id: Int,
        sendStatus: SendStatus,
        sendError: String?
    )

    @Query(
        """
        UPDATE messages
        SET delivery_status=:deliveryStatus, delivery_error=:deliveryError
        WHERE id=:id
        """
    )
    suspend fun updateDeliveryStatus(
        id: Int,
        deliveryStatus: DeliveryStatus,
        deliveryError: String?
    )

    @Query(
        """
        UPDATE messages
        SET send_status='Queued', send_error=NULL,
            delivery_status=NULL, delivery_error=NULL
        WHERE send_status='Failed' OR delivery_status='Failed'
        """
    )
    suspend fun resetFailedToQueued()
}