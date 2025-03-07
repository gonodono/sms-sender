package com.gonodono.smssender

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonodono.smssender.internal.hasSmsPermissions
import com.gonodono.smssender.model.Message
import com.gonodono.smssender.model.SendStatus
import com.gonodono.smssender.model.isFailed
import com.gonodono.smssender.model.isQueued
import com.gonodono.smssender.repository.SmsSenderRepository
import com.gonodono.smssender.work.SmsSendWorker
import com.gonodono.smssender.work.WorkerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SmsSenderRepository
) : ViewModel() {

    private val hasPermissions = MutableStateFlow(context.hasSmsPermissions())

    val uiState: Flow<UiState> = combine(
        hasPermissions,
        repository.messages,
        SmsSendWorker.workerState(context)
    ) { hasPermissions, messages, workerState ->
        if (hasPermissions) {
            val isIdle = !workerState.isSending
            val canSend = isIdle && messages.any { it.isQueued() }
            val canReset = messages.any { it.isFailed() }
            UiState.Active(messages, workerState, canSend, canReset)
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

sealed interface UiState {

    data object Initial : UiState

    data object NoPermissions : UiState

    data class Active(
        val messages: List<Message>,
        val workerState: WorkerState,
        val canSendQueued: Boolean,
        val canResetFailed: Boolean
    ) : UiState
}