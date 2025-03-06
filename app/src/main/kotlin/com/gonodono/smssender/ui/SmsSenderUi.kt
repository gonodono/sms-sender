package com.gonodono.smssender.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonodono.smssender.MainViewModel
import com.gonodono.smssender.UiState

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun SmsSenderUi(viewModel: MainViewModel = viewModel()) = Surface {
    val uiState by viewModel.uiState.collectAsState(UiState.Initial)
    val transition = updateTransition(uiState, "Content")
    transition.Crossfade(
        modifier = Modifier.safeDrawingPadding(),
        contentKey = { it::class }
    ) { state ->
        when (state) {
            UiState.Initial -> {
                TextBox("Loading…")
            }
            UiState.NoPermissions -> {
                PermissionsRequest { viewModel.setPermissionsGranted() }
            }
            is UiState.Active -> {
                MainContent(
                    uiState = state,
                    queueMessages = { viewModel.queueMessages() },
                    startSend = { viewModel.startSend() },
                    cancelSend = { viewModel.cancelSend() },
                    resetFailed = { viewModel.resetFailed() }
                )
            }
        }
    }
}