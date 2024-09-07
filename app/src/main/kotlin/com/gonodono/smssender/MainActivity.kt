package com.gonodono.smssender

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonodono.smssender.sms.SmsPermissions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val contract = ActivityResultContracts.RequestMultiplePermissions()
        val request = registerForActivityResult(contract) { grants ->
            setUpUi(grants.all { it.value })
        }

        super.onCreate(savedInstanceState)

        val requestPermissions = SmsPermissions.any {
            checkSelfPermission(it) != PERMISSION_GRANTED
        }
        when {
            requestPermissions -> request.launch(SmsPermissions)
            else -> setUpUi(true)
        }
    }

    private fun setUpUi(hasPermissions: Boolean) {
        setContent { if (hasPermissions) Content() else PermissionsError() }
    }
}

@Composable
private fun Content(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState(UiState.Loading)
    AnimatedContent(
        targetState = uiState is UiState.Active,
        label = "Content"
    ) { isActive ->
        when {
            isActive -> MainContent(uiState as UiState.Active, viewModel)
            else -> LoadingMessage()
        }
    }
}

@Composable
private fun MainContent(uiState: UiState.Active, viewModel: MainViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val bold = TextStyle.Default.copy(fontWeight = FontWeight.Bold)
        CompositionLocalProvider(LocalTextStyle provides bold) {
            SimpleItem("ID", "Address", "Sent", "Delivery")
        }

        Divider(Modifier.padding(vertical = 6.dp))

        LazyColumn(Modifier.weight(1F)) {
            items(uiState.messages) { MessageItem(it) }
        }

        InfoText(uiState)

        ButtonPanel(uiState, viewModel)
    }
}

@Composable
private fun InfoText(uiState: UiState.Active) {
    val text = when (val error = uiState.lastError) {
        null -> if (uiState.isSending) "Sending…" else "Idle"
        else -> error
    }
    val color = when {
        uiState.lastError != null -> Color.Red
        else -> Color.Unspecified
    }
    Text(
        text = text,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
        color = color,
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()
            .border(0.dp, Color.Black)
            .padding(4.dp)
    )
}

@Composable
private fun ButtonPanel(uiState: UiState.Active, viewModel: MainViewModel) {
    when {
        uiState.isSending -> {
            OutlinedButton(viewModel::tryCancelCurrentSend) {
                Text("Cancel current send")
            }
        }
        else -> {
            if (uiState.queuedCount > 0) {
                OutlinedButton(viewModel::sendQueuedMessages) {
                    Text("Send ${uiState.queuedCount} queued messages")
                }
            } else {
                OutlinedButton(viewModel::queueDemoMessagesAndSend) {
                    Text("Queue messages & send")
                }
            }
            if (uiState.failedCount > 0) {
                Spacer(Modifier.size(10.dp))
                OutlinedButton(viewModel::resetFailedAndRetry) {
                    Text("Reset ${uiState.failedCount} failed & retry all")
                }
            }
        }
    }
}

@Composable
private fun LoadingMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Loading…", fontSize = 30.sp)
    }
}

@Composable
private fun PermissionsError() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "No permissions", color = Color.Red, fontSize = 30.sp)
    }
}

@Composable
private fun SimpleItem(vararg texts: String) {
    Row { texts.forEach { Text(it, Modifier.weight(1F)) } }
}

@Composable
private fun MessageItem(info: MessageInfo) {
    SimpleItem(info.id, info.address, info.sendStatus, info.deliveryStatus)
}