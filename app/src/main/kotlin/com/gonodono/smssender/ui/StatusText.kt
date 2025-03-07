package com.gonodono.smssender.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gonodono.smssender.UiState

@Composable
fun StatusText(uiState: UiState.Active) {
    val workerState = uiState.workerState
    val text = workerState.lastError ?: when {
        workerState.isCancelled -> "Cancelled"
        workerState.isSending -> "Sending…"
        else -> "Idle"
    }
    val color =
        if (workerState.lastError != null) MaterialTheme.colorScheme.error
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