package com.gonodono.smssender.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gonodono.smssender.UiState
import com.gonodono.smssender.model.isFailed
import com.gonodono.smssender.model.isQueued

@Composable
internal fun TextBox(message: String) =
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(message, fontSize = 26.sp)
    }

@Composable
internal fun StatusText(uiState: UiState.Active) {
    val text = uiState.lastError ?: when {
        uiState.isCancelled -> "Cancelled"
        uiState.isSending -> "Sending…"
        else -> "Idle"
    }
    val color =
        if (uiState.lastError != null) MaterialTheme.colorScheme.error
        else Color.Unspecified
    Text(
        text = text,
        color = color,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = Modifier
            .padding(vertical = 20.dp)
            .fillMaxWidth()
            .border(1.dp, LocalContentColor.current)
            .padding(10.dp)
    )
}

@Composable
internal fun ButtonPanel(
    uiState: UiState.Active,
    queueMessages: () -> Unit,
    startSend: () -> Unit,
    cancelSend: () -> Unit,
    resetFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Button(queueMessages, true, "Queue messages")

            val canSend = !uiState.isSending &&
                    uiState.messages.any { it.isQueued() }
            Button(startSend, canSend, "Start send")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            val hasFailed = uiState.messages.any { it.isFailed() }
            Button(resetFailed, hasFailed, "Reset failed")

            Button(cancelSend, uiState.isSending, "Cancel send")
        }
    }
}

@Composable
private fun RowScope.Button(
    onClick: () -> Unit,
    enabled: Boolean,
    text: String
) =
    OutlinedButton(onClick, Modifier.weight(1F), enabled) {
        Text(text, overflow = TextOverflow.Ellipsis, maxLines = 1)
    }