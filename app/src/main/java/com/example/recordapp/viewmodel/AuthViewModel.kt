package com.example.recordapp.viewmodel

import android.app.Application
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordapp.model.User
import com.example.recordapp.repository.AuthRepository
import com.example.recordapp.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication operations
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = AuthRepository.getInstance(application)
    
    // UI state
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    // Current user
    val currentUser = repository.currentUser
    
    init {
        // Check if user is already logged in
        _uiState.value = _uiState.value.copy(
            isLoggedIn = repository.isLoggedIn()
        )
    }
    
    /**
     * Register a new user
     */
    fun registerUser(email: String, password: String, confirmPassword: String, name: String) {
        // Validate inputs
        val validationResult = validateRegistrationInputs(email, password, confirmPassword)
        if (!validationResult.isValid) {
            _uiState.value = _uiState.value.copy(
                errorMessage = validationResult.errorMessage
            )
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
                
                val result = repository.registerUser(email, password, name)
                
                result.fold(
                    onSuccess = {
                        // Auto-login after registration
                        login(email, password)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Registration failed"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during registration", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Registration failed"
                )
            }
        }
    }
    
    /**
     * Login a user
     */
    fun login(email: String, password: String) {
        // Validate inputs
        val validationResult = validateLoginInputs(email, password)
        if (!validationResult.isValid) {
            _uiState.value = _uiState.value.copy(
                errorMessage = validationResult.errorMessage
            )
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
                
                val result = repository.login(email, password)
                
                result.fold(
                    onSuccess = { user ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Login failed"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during login", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Login failed"
                )
            }
        }
    }
    
    /**
     * Logout the current user
     */
    fun logout() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
                
                val result = repository.logout()
                
                result.fold(
                    onSuccess = {
                        // Reset permission state on logout
                        PermissionUtils.resetPermissionState(getApplication())
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Logout failed"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Logout failed"
                )
            }
        }
    }
    
    /**
     * Validate login inputs
     */
    private fun validateLoginInputs(email: String, password: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(false, "Email cannot be empty")
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult(false, "Invalid email format")
        }
        
        if (password.isBlank()) {
            return ValidationResult(false, "Password cannot be empty")
        }
        
        return ValidationResult(true)
    }
    
    /**
     * Validate registration inputs
     */
    private fun validateRegistrationInputs(
        email: String,
        password: String,
        confirmPassword: String
    ): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(false, "Email cannot be empty")
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult(false, "Invalid email format")
        }
        
        if (password.isBlank()) {
            return ValidationResult(false, "Password cannot be empty")
        }
        
        if (password.length < 6) {
            return ValidationResult(false, "Password must be at least 6 characters")
        }
        
        if (password != confirmPassword) {
            return ValidationResult(false, "Passwords do not match")
        }
        
        return ValidationResult(true)
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
}

/**
 * UI state for authentication
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
) 