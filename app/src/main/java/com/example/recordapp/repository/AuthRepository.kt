package com.example.recordapp.repository

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.recordapp.model.User
import com.example.recordapp.network.SupabaseClient
import com.example.recordapp.util.SignInHistoryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user authentication
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val supabaseClient: SupabaseClient,
    private val signInHistoryManager: SignInHistoryManager
) {
    
    private val TAG = "AuthRepository"
    
    // Current user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    init {
        // Check if user is logged in
        checkLoggedInUser()
    }
    
    /**
     * Check if a user is currently logged in
     */
    private fun checkLoggedInUser() {
        try {
            val supabaseUser = supabaseClient.getCurrentUser()
            supabaseUser?.let { userInfo ->
                // Convert Supabase UserInfo to our User model
            _currentUser.value = User(
                    id = userInfo.id,
                    email = userInfo.email ?: "",
                    password = "", // For security, we don't store the actual password in memory
                    name = userInfo.userMetadata?.get("name")?.toString() ?: "",
                    lastLoginTime = userInfo.lastSignInAt?.toEpochMilliseconds() ?: 0L
            )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking logged in user", e)
        }
    }
    
    /**
     * Register a new user
     */
    suspend fun registerUser(email: String, password: String, name: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Create user data map for Supabase
            val userData = mapOf(
                "name" to name,
                "created_at" to System.currentTimeMillis()
            )
            
            // Register user with Supabase
            val result = supabaseClient.signUp(email, password, userData)
            
            result.fold(
                onSuccess = { userInfo ->
                    // Convert Supabase UserInfo to our User model
            val user = User(
                        id = userInfo.id,
                        email = userInfo.email ?: email,
                        password = "", // For security, we don't store the actual password in memory
                name = name, 
                        creationTime = System.currentTimeMillis()
            )
            
                    // Update current user
                    _currentUser.value = user
                    
                    Result.success(user)
                },
                onFailure = { error ->
                    Log.e(TAG, "Error registering user with Supabase", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user", e)
            Result.failure(e)
        }
    }
    
    /**
     * Login a user
     */
    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Login with Supabase
            val result = supabaseClient.signIn(email, password)
            
            result.fold(
                onSuccess = { userInfo ->
                    // Get user metadata
                    val name = userInfo.userMetadata?.get("name")?.toString() ?: ""
            val loginTime = System.currentTimeMillis()
            
            // Create user object
            val user = User(
                        id = userInfo.id,
                        email = userInfo.email ?: email,
                        password = "", // For security, we don't store the actual password in memory
                name = name, 
                lastLoginTime = loginTime
            )
            
            // Update current user
            _currentUser.value = user

            // Save to sign-in history
            signInHistoryManager.saveSignInRecord(user.email, user.name)

                    Result.success(user)
                },
                onFailure = { error ->
                    Log.e(TAG, "Error logging in with Supabase", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in", e)
            Result.failure(e)
        }
    }
    
    /**
     * Logout the current user
     */
    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Logout with Supabase
            val result = supabaseClient.signOut()
            
            result.fold(
                onSuccess = {
            // Clear current user
            _currentUser.value = null
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Error logging out with Supabase", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error logging out", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if a user is logged in
     */
    fun isLoggedIn(): Boolean {
        return supabaseClient.isSignedIn()
    }
    
    /**
     * Get the current user
     */
    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        return@withContext _currentUser.value
    }

    /**
     * Get sign-in history
     */
    suspend fun getSignInHistory() = signInHistoryManager.getSignInHistory()

    /**
     * Get most recent sign-in
     */
    suspend fun getMostRecentSignIn() = signInHistoryManager.getMostRecentSignIn()

    /**
     * Clear sign-in history
     */
    suspend fun clearSignInHistory() = signInHistoryManager.clearSignInHistory()
    
    companion object {
        @Volatile
        private var instance: AuthRepository? = null
        
        fun getInstance(context: Context): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository(
                    context,
                    SupabaseClient.getInstance(),
                    SignInHistoryManager(context)
                ).also { instance = it }
            }
        }
    }
} 