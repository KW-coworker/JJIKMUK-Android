package com.coworker.jjikmuk.feature.auth.photo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Home stays interactive while the signup photo syncs; only failures show a retry snackbar. */
@Composable
fun SignupPhotoSyncHost(viewModel: PendingSignupPhotoViewModel, content: @Composable () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.open() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        if (snackbar.showSnackbar(message, actionLabel = "재시도", withDismissAction = true) == SnackbarResult.ActionPerformed)
            viewModel.retry()
    }
    Box(Modifier.fillMaxSize()) {
        content()
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp))
    }
}
