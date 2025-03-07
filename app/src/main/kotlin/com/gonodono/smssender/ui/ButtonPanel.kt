package com.gonodono.smssender.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gonodono.smssender.UiState

@Composable
fun ButtonPanel(
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
            Button(startSend, uiState.canSendQueued, "Start send")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Button(resetFailed, uiState.canResetFailed, "Reset failed")
            Button(cancelSend, uiState.workerState.isSending, "Cancel send")
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