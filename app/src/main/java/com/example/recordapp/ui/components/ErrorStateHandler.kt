package com.example.recordapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * State holder for different UI states
 */
sealed class UiState {
    object Success : UiState()
    object Loading : UiState()
    data class Error(val message: String) : UiState()
    object NoInternet : UiState()
    object Empty : UiState()
}

/**
 * A generic error state handler component to show different UI states
 */
@Composable
fun ErrorStateHandler(
    state: UiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    when (state) {
        is UiState.Error -> ErrorContent(
            title = "Error",
            message = state.message,
            icon = Icons.Filled.ErrorOutline,
            onRetry = onRetry,
            modifier = modifier
        )
        
        is UiState.NoInternet -> ErrorContent(
            title = "No Internet Connection",
            message = "Please check your internet connection and try again",
            icon = Icons.Filled.WifiOff,
            onRetry = onRetry,
            modifier = modifier
        )
        
        is UiState.Empty -> ErrorContent(
            title = "No Data",
            message = "There are no items to display",
            icon = Icons.Filled.Warning,
            showRetry = false,
            modifier = modifier
        )
        
        else -> { /* Success and Loading states handled elsewhere */ }
    }
}

@Composable
private fun ErrorContent(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    showRetry: Boolean = true,
    onRetry: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (showRetry) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
} 