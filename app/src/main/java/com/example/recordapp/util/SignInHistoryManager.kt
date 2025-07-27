package com.example.recordapp.util

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


/**
 * Manages sign-in history for quick and easy re-authentication
 * Stores email addresses and user names (NOT passwords) for security
 */
class SignInHistoryManager(
    private val context: Context
) {
    
    private val TAG = "SignInHistoryManager"
    
    companion object {
        private const val PREFS_NAME = "signin_history_prefs"
        private const val KEY_SIGNIN_HISTORY = "signin_history"
        private const val MAX_HISTORY_SIZE = 5 // Keep last 5 sign-ins
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Data class for storing sign-in history
     */
    @Serializable
    data class SignInRecord(
        val email: String,
        val userName: String,
        val lastSignInTime: Long,
        val signInCount: Int = 1
    )
    
    /**
     * Get encrypted shared preferences for storing sign-in history
     */
    private suspend fun getEncryptedPrefs(): EncryptedSharedPreferences? = withContext(Dispatchers.IO) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        } catch (e: Exception) {
            Log.e(TAG, "Error creating encrypted preferences", e)
            null
        }
    }
    
    /**
     * Save a successful sign-in to history
     */
    suspend fun saveSignInRecord(email: String, userName: String) = withContext(Dispatchers.IO) {
        try {
            val prefs = getEncryptedPrefs() ?: return@withContext
            val currentHistory = getSignInHistory().toMutableList()
            
            // Check if email already exists in history
            val existingIndex = currentHistory.indexOfFirst { it.email.equals(email, ignoreCase = true) }
            
            val newRecord = if (existingIndex >= 0) {
                // Update existing record
                val existing = currentHistory[existingIndex]
                currentHistory.removeAt(existingIndex)
                existing.copy(
                    userName = userName, // Update name in case it changed
                    lastSignInTime = System.currentTimeMillis(),
                    signInCount = existing.signInCount + 1
                )
            } else {
                // Create new record
                SignInRecord(
                    email = email,
                    userName = userName,
                    lastSignInTime = System.currentTimeMillis(),
                    signInCount = 1
                )
            }
            
            // Add to beginning of list (most recent first)
            currentHistory.add(0, newRecord)
            
            // Keep only the most recent entries
            if (currentHistory.size > MAX_HISTORY_SIZE) {
                currentHistory.removeAt(currentHistory.size - 1)
            }
            
            // Save updated history
            val historyJson = json.encodeToString(currentHistory)
            prefs.edit().putString(KEY_SIGNIN_HISTORY, historyJson).apply()
            
            Log.d(TAG, "Saved sign-in record for: $email")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving sign-in record", e)
        }
    }
    
    /**
     * Get sign-in history (most recent first)
     */
    suspend fun getSignInHistory(): List<SignInRecord> = withContext(Dispatchers.IO) {
        try {
            val prefs = getEncryptedPrefs() ?: return@withContext emptyList()
            val historyJson = prefs.getString(KEY_SIGNIN_HISTORY, null)
            
            if (historyJson != null) {
                json.decodeFromString<List<SignInRecord>>(historyJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sign-in history", e)
            emptyList()
        }
    }
    
    /**
     * Get the most recent sign-in record
     */
    suspend fun getMostRecentSignIn(): SignInRecord? = withContext(Dispatchers.IO) {
        getSignInHistory().firstOrNull()
    }
    
    /**
     * Remove a specific sign-in record
     */
    suspend fun removeSignInRecord(email: String) = withContext(Dispatchers.IO) {
        try {
            val prefs = getEncryptedPrefs() ?: return@withContext
            val currentHistory = getSignInHistory().toMutableList()
            
            currentHistory.removeAll { it.email.equals(email, ignoreCase = true) }
            
            val historyJson = json.encodeToString(currentHistory)
            prefs.edit().putString(KEY_SIGNIN_HISTORY, historyJson).apply()
            
            Log.d(TAG, "Removed sign-in record for: $email")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing sign-in record", e)
        }
    }
    
    /**
     * Clear all sign-in history
     */
    suspend fun clearSignInHistory() = withContext(Dispatchers.IO) {
        try {
            val prefs = getEncryptedPrefs() ?: return@withContext
            prefs.edit().remove(KEY_SIGNIN_HISTORY).apply()
            Log.d(TAG, "Cleared all sign-in history")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing sign-in history", e)
        }
    }
    
    /**
     * Check if there are any saved sign-in records
     */
    suspend fun hasSignInHistory(): Boolean = withContext(Dispatchers.IO) {
        getSignInHistory().isNotEmpty()
    }
}
