package com.gonodono.smssender

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
        setContent { if (hasPermissions) MainContent() else PermissionsError() }
    }
}

@Composable
private fun MainContent(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState(UiState.Loading)

    AnimatedVisibility(
        visible = uiState is UiState.Active,
        enter = fadeIn(), exit = fadeOut()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val state = uiState as UiState.Active

            val bold = TextStyle.Default.copy(fontWeight = FontWeight.Bold)
            CompositionLocalProvider(LocalTextStyle provides bold) {
                SimpleItem("ID", "Address", "Sent", "Delivery")
            }

            Divider(Modifier.padding(vertical = 6.dp))

            LazyColumn(Modifier.weight(1F)) {
                items(state.messages) { MessageItem(it) }
            }

            AnimatedVisibility(state.isSending) {
                Text("Sending…", fontSize = 20.sp, color = Color.Blue)
            }
            AnimatedVisibility(state.lastError != null) {
                Text(state.lastError ?: "", fontSize = 18.sp, color = Color.Red)
            }

            Divider(Modifier.padding(vertical = 16.dp))

            TextButton(
                onClick = viewModel::queueDemoMessagesAndSend,
                enabled = !state.isSending
            ) {
                Text("Queue messages & send")
            }
            AnimatedVisibility(state.failedCount > 0) {
                TextButton(
                    onClick = viewModel::resetFailedAndRetry,
                    enabled = !state.isSending
                ) {
                    Text("Reset ${state.failedCount} failed & retry")
                }
            }
        }
    }
    AnimatedVisibility(
        visible = uiState == UiState.Loading,
        enter = fadeIn(), exit = fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading…", fontSize = 30.sp)
        }
    }
}

@Composable
private fun PermissionsError() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No permissions",
            color = Color.Red,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
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