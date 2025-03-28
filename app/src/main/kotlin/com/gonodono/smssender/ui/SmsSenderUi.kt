package com.gonodono.smssender.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonodono.smssender.MainViewModel
import com.gonodono.smssender.UiState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SmsSenderUi(viewModel: MainViewModel = viewModel()) =
    Surface(
        color = Color.Transparent,
        contentColor = contentColorFor(MaterialTheme.colorScheme.surface)
    ) {
        val uiState by viewModel.uiState.collectAsState(UiState.Initial)
        val transition = updateTransition(uiState, "Content")
        transition.Crossfade(
            modifier = Modifier.safeDrawingPadding(),
            contentKey = { it::class }
        ) { state ->
            when (state) {
                UiState.Initial -> TextBox("Loading…")

                UiState.NoPermissions -> {
                    PermissionsRequest(viewModel::setPermissionsGranted)
                }
                is UiState.Active -> {
                    MainContent(
                        uiState = state,
                        queueMessages = viewModel::queueMessages,
                        startSend = viewModel::startSend,
                        cancelSend = viewModel::cancelSend,
                        resetFailed = viewModel::resetFailed
                    )
                }
            }
        }
    }

@Composable
fun TextBox(message: String) =
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(message, fontSize = 26.sp)
    }