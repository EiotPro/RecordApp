package com.example.recordapp.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordapp.RecordApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Extension function to launch a coroutine safely from a ViewModel
 * 
 * @param context Additional coroutine context
 * @param onError Lambda to execute on error (optional)
 * @param block The coroutine code to execute
 */
fun ViewModel.launchSafely(
    context: CoroutineContext? = null,
    onError: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit
) = viewModelScope.launch(RecordApplication.globalExceptionHandler) {
    try {
        if (context != null) {
            withContext(context) { block() }
        } else {
            block()
        }
    } catch (e: Exception) {
        onError(e)
    }
}

/**
 * Extension function to handle UI state safely
 * 
 * @param initialState Initial UI state
 * @param onError Error handler function
 * @param block Lambda to execute that returns the success state
 * @return The final UI state
 */
suspend fun <T> handleUiState(
    initialState: UiState = UiState.Loading,
    onError: (Throwable) -> UiState.Error = { UiState.Error(it.message ?: "Unknown error") },
    block: suspend () -> T
): UiState {
    return try {
        val result = block()
        when (result) {
            null -> UiState.Empty
            is List<*> -> if (result.isEmpty()) UiState.Empty else UiState.Success
            else -> UiState.Success
        }
    } catch (e: Exception) {
        onError(e)
    }
}

/**
 * Maps a throwable to a user-friendly error message
 * 
 * @param throwable The exception to map
 * @return User-friendly error message
 */
fun mapErrorToUserMessage(throwable: Throwable): String {
    return when (throwable) {
        is java.net.UnknownHostException,
        is java.net.ConnectException,
        is java.net.SocketTimeoutException -> "Network connection error. Please check your internet connection."
        is java.io.IOException -> "Data access error. Please try again."
        is NullPointerException -> "Application error. Data is missing or corrupted."
        is IllegalArgumentException -> "Invalid data provided. Please check your inputs."
        is SecurityException -> "Permission denied. Please check app permissions."
        is OutOfMemoryError -> "Application memory error. Please restart the app."
        else -> "An unexpected error occurred. Please try again."
    }
} 