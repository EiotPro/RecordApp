package com.example.recordapp.util

/**
 * Sealed class to represent different UI states in the application
 */
sealed class UiState {
    /**
     * Loading state - data is being fetched or processed
     */
    object Loading : UiState()
    
    /**
     * Success state - operation completed successfully
     */
    object Success : UiState()
    
    /**
     * Empty state - operation successful but no data available
     */
    object Empty : UiState()
    
    /**
     * Error state - operation failed with an error
     */
    data class Error(val message: String) : UiState()
} 