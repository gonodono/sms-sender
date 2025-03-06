package com.gonodono.smssender

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonodono.smssender.database.DeliveryStatus
import com.gonodono.smssender.database.Message
import com.gonodono.smssender.database.SendStatus
import com.gonodono.smssender.internal.ExampleData
import com.gonodono.smssender.internal.hasSmsPermissions
import com.gonodono.smssender.repository.SmsSenderRepository
import com.gonodono.smssender.work.SmsSendWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SmsSenderRepository
) : ViewModel() {

    private val hasPermissions = MutableStateFlow(context.hasSmsPermissions())

    val uiState: Flow<UiState> = combine(
        hasPermissions,
        repository.allMessages,
        SmsSendWorker.workInfos(context)
    ) { hasPermissions, messages, workInfos ->
        if (hasPermissions) {
            UiState.Active(
                messages.map { it.toMessageInfo() },
                messages.count { it.isQueued },
                messages.count { it.isFailed },
                SmsSendWorker.isSending(workInfos),
                SmsSendWorker.isCancelled(workInfos),
                SmsSendWorker.lastError(workInfos)
            )
        } else {
            UiState.NoPermissions
        }
    }

    fun setPermissionsGranted() {
        hasPermissions.value = true
    }

    fun queueMessages() {
        viewModelScope.launch {
            val messages = ExampleData.AllTexts.map { text ->
                // Ensures unique texts. This is really only so that the fake
                // delivery report lookups work correctly with prior failures.
                val body = "$text - ${System.currentTimeMillis()}"
                Message(ExampleData.ADDRESS, body, SendStatus.Queued)
            }
            repository.insertMessages(messages)
        }
    }

    fun startSend() = repository.startSend()

    fun cancelSend() = repository.cancelSend()

    fun resetFailed() {
        viewModelScope.launch { repository.resetFailed() }
    }
}

internal sealed interface UiState {

    data object Initial : UiState

    data object NoPermissions : UiState

    data class Active(
        val messages: List<MessageInfo>,
        val queuedCount: Int,
        val failedCount: Int,
        val isSending: Boolean,
        val isCancelled: Boolean,
        val lastError: String?
    ) : UiState
}

internal class MessageInfo(
    val id: String,
    val address: String,
    val sendStatus: String,
    val deliveryStatus: String
)

private fun Message.toMessageInfo() =
    MessageInfo(
        id.toString(),
        address,
        sendStatus.toString(),
        deliveryStatus.toString()
    )

private val Message.isQueued: Boolean
    get() = sendStatus == SendStatus.Queued

private val Message.isFailed: Boolean
    get() = sendStatus == SendStatus.Failed ||
            deliveryStatus == DeliveryStatus.Failed