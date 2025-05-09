package com.example.recordapp.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.recordapp.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository for user authentication
 */
class AuthRepository private constructor(context: Context) {
    
    private val sharedPreferences: SharedPreferences
    
    // Current user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    init {
        // Use EncryptedSharedPreferences for security
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        // Check if user is logged in
        checkLoggedInUser()
    }
    
    /**
     * Check if a user is currently logged in
     */
    private fun checkLoggedInUser() {
        val userId = sharedPreferences.getString(KEY_USER_ID, null) ?: return
        val email = sharedPreferences.getString(KEY_EMAIL, null) ?: return
        val name = sharedPreferences.getString(KEY_NAME, "") ?: ""
        
        // For security, we don't store the actual password in memory
        _currentUser.value = User(id = userId, email = email, password = "", name = name)
    }
    
    /**
     * Register a new user
     */
    suspend fun registerUser(email: String, password: String, name: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Check if email is already registered
            val existingEmail = sharedPreferences.getString("email_$email", null)
            if (existingEmail != null) {
                return@withContext Result.failure(Exception("Email already registered"))
            }
            
            // Create new user
            val userId = UUID.randomUUID().toString()
            val user = User(id = userId, email = email, password = password, name = name)
            
            // Store user data (except for memory representation)
            val editor = sharedPreferences.edit()
            editor.putString("email_$email", userId)
            editor.putString("user_$userId", email)
            editor.putString("password_$userId", password)
            editor.putString("name_$userId", name)
            editor.apply()
            
            return@withContext Result.success(user)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Login a user
     */
    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Check if email exists
            val userId = sharedPreferences.getString("email_$email", null)
                ?: return@withContext Result.failure(Exception("Email not registered"))
            
            // Check password
            val storedPassword = sharedPreferences.getString("password_$userId", null)
            if (storedPassword != password) {
                return@withContext Result.failure(Exception("Invalid password"))
            }
            
            // Get user name
            val name = sharedPreferences.getString("name_$userId", "") ?: ""
            
            // Create user object
            val user = User(id = userId, email = email, password = "", name = name)
            
            // Save login state
            val editor = sharedPreferences.edit()
            editor.putString(KEY_USER_ID, userId)
            editor.putString(KEY_EMAIL, email)
            editor.putString(KEY_NAME, name)
            editor.apply()
            
            // Update current user
            _currentUser.value = user
            
            return@withContext Result.success(user)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Logout the current user
     */
    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Clear login state
            val editor = sharedPreferences.edit()
            editor.remove(KEY_USER_ID)
            editor.remove(KEY_EMAIL)
            editor.remove(KEY_NAME)
            editor.apply()
            
            // Clear current user
            _currentUser.value = null
            
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Check if a user is logged in
     */
    fun isLoggedIn(): Boolean {
        return _currentUser.value != null
    }
    
    companion object {
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_EMAIL = "current_user_email"
        private const val KEY_NAME = "current_user_name"
        
        @Volatile
        private var instance: AuthRepository? = null
        
        fun getInstance(context: Context): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository(context.applicationContext).also { instance = it }
            }
        }
    }
} 