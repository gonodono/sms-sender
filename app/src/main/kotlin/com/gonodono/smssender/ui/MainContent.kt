package com.gonodono.smssender.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gonodono.smssender.UiState

@Composable
internal fun MainContent(
    uiState: UiState.Active,
    queueMessages: () -> Unit,
    startSend: () -> Unit,
    cancelSend: () -> Unit,
    resetFailed: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        ColumnHeaders()

        HorizontalDivider(Modifier.padding(vertical = 10.dp))

        LazyColumn(Modifier.weight(1F)) {
            items(uiState.messages) { MessageItem(it) }
        }

        StatusText(uiState)

        ButtonPanel(
            uiState = uiState,
            queueMessages = queueMessages,
            startSend = startSend,
            cancelSend = cancelSend,
            resetFailed = resetFailed
        )
    }
}