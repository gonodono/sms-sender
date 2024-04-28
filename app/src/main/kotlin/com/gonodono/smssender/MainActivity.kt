package com.gonodono.smssender

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gonodono.smssender.sms.SmsPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val contract = ActivityResultContracts.RequestMultiplePermissions()
        val request = registerForActivityResult(contract) { grants ->
            setUpUi(grants.all { it.value })
        }

        super.onCreate(savedInstanceState)

        val permissions = SmsPermissions
        if (permissions.any { checkSelfPermission(it) != PERMISSION_GRANTED }) {
            request.launch(permissions)
        } else {
            setUpUi(true)
        }
    }

    private fun setUpUi(hasPermissions: Boolean) {
        if (!hasPermissions) {
            setContent {
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
            return
        }

        val viewModel: MainViewModel by viewModels()
        var uiState: UiState by mutableStateOf(UiState.Loading)

        lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(lifecycle)
                .onEach { uiState = it }
                .collect()
        }

        setContent {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                when (val state = uiState) {
                    is UiState.Active -> {
                        Text(
                            text = state.messages,
                            modifier = Modifier.weight(1F)
                        )

                        val info = when {
                            state.isSending -> "Sending…"
                            else -> state.lastError
                        }
                        if (info != null) Text(
                            text = info,
                            modifier = Modifier.padding(10.dp),
                            color = Color.Red
                        )

                        TextButton({ viewModel.queueDemoMessagesAndSend() }) {
                            Text("Queue messages & send")
                        }

                        TextButton({ viewModel.resetFailedAndRetry() }) {
                            Text("Reset failed & retry")
                        }
                    }

                    else -> Text("Loading…")
                }
            }
        }
    }
}