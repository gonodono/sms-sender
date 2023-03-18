package com.gonodono.smssender

import android.Manifest.permission.RECEIVE_SMS
import android.Manifest.permission.SEND_SMS
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val request =
            registerForActivityResult(RequestMultiplePermissions()) { grants ->
                if (grants.all { it.value }) setUpUi()
            }

        super.onCreate(savedInstanceState)

        // RECEIVE_SMS is used only for the fake delivery report.
        val permissions = arrayOf(SEND_SMS, RECEIVE_SMS)
        if (permissions.any { checkSelfPermission(it) != PERMISSION_GRANTED }) {
            request.launch(permissions)
        } else {
            setUpUi()
        }
    }

    private fun setUpUi() {
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
                        Text(state.messages, Modifier.weight(1F))

                        val info = when {
                            state.isSending -> "Sending…"
                            else -> state.lastError
                        }

                        if (info != null) {
                            Text(info, Modifier.padding(10.dp), Color.Red)
                        }

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