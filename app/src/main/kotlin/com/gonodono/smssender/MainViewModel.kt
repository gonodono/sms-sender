package com.gonodono.smssender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonodono.smssender.data.Message
import com.gonodono.smssender.data.SendTask
import com.gonodono.smssender.repository.SmsSenderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed interface UiState {
    data object Loading : UiState
    data class Active(
        val messages: String,
        val isSending: Boolean,
        val lastError: String?
    ) : UiState
}

@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val repository: SmsSenderRepository
) : ViewModel() {

    val uiState = combine(
        repository.allMessages,
        repository.latestSendTask
    ) { messages, task ->
        UiState.Active(
            itemize(messages),
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

    fun resetFailedAndRetry() {
        viewModelScope.launch { repository.resetFailedAndRetry() }
    }
}

private fun itemize(messages: List<Message>) =
    messages.joinToString("\n\n") { it.toDebugString() }

private fun Message.toDebugString() =
    "#%d: to=%s, ss=%s, ds=%s".format(id, address, sendStatus, deliveryStatus)

private val ShortTexts = arrayOf("Hi!", "Hello!", "Howdy!")

private val LongTexts = arrayOf(
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