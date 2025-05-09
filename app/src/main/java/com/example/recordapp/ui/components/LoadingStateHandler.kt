package com.example.recordapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.recordapp.util.UiState

/**
 * A simple loading indicator component
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Float = 48f
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * A dialog loading indicator that blocks UI interaction
 */
@Composable
fun DialogLoadingIndicator(
    isShowing: Boolean,
    onDismissRequest: () -> Unit = {}
) {
    if (isShowing) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .size(64.dp)
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * A component to handle different loading states in the UI
 */
@Composable
fun LoadingStateHandler(
    state: UiState,
    isDialogMode: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    when (state) {
        is UiState.Loading -> {
            if (isDialogMode) {
                // Show content with a dialog overlay
                Box(modifier = modifier) {
                    content()
                    DialogLoadingIndicator(isShowing = true)
                }
            } else {
                // Show just the loading indicator
                LoadingIndicator(modifier = modifier)
            }
        }
        else -> {
            // For all other states, show the content
            content()
        }
    }
} 