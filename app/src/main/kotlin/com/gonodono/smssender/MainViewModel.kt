package com.gonodono.smssender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonodono.smssender.data.Message
import com.gonodono.smssender.data.SendTask
import com.gonodono.smssender.repository.SmsSenderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val repository: SmsSenderRepository
) : ViewModel() {

    val uiState: Flow<UiState> = combine(
        repository.allMessages,
        repository.latestSendTask
    ) { messages, task ->
        UiState.Active(
            messages.map { it.toMessageInfo() },
            messages.count { it.isQueued },
            messages.count { it.isFailed },
            task?.state == SendTask.State.Running,
            task?.error
        )
    }

    fun queueDemoMessagesAndSend() {
        viewModelScope.launch {
            val messages = (ShortTexts + LongTexts).map { text ->
                Message(EMULATOR_PORT, text, Message.SendStatus.Queued)
            }
            repository.insertMessagesAndSend(messages)
        }
    }

    fun sendQueuedMessages() {
        repository.startImmediateSend()
    }

    fun resetFailedAndRetry() {
        viewModelScope.launch { repository.resetFailedAndRetry() }
    }

    fun tryCancelCurrentSend() {
        repository.tryCancelCurrentSend()
    }
}

internal sealed interface UiState {

    data object Loading : UiState

    data class Active(
        val messages: List<MessageInfo>,
        val queuedCount: Int,
        val failedCount: Int,
        val isSending: Boolean,
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

private val ShortTexts: List<String> = listOf("Hi!", "Hello!", "Howdy!")

private val LongTexts: List<String> = listOf(
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod" +
            " tempor incididunt ut labore et dolore magna aliqua. Ut enim ad" +
            " minim veniam, quis nostrud exercitation ullamco laboris nisi ut" +
            " aliquip ex ea commodo consequat.",
    "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum" +
            " dolore eu fugiat nulla pariatur. Excepteur sint occaecat" +
            " cupidatat non proident, sunt in culpa qui officia deserunt" +
            " mollit anim id est laborum.",
    "Sed ut perspiciatis unde omnis iste natus error sit voluptatem " +
            "accusantium doloremque laudantium, totam rem aperiam, eaque ipsa" +
            " quae ab illo inventore veritatis et quasi architecto beatae" +
            " vitae dicta sunt explicabo."
)

internal const val EMULATOR_PORT = "5554"