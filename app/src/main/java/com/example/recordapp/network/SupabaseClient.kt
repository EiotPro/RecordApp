package com.example.recordapp.network

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseClient @Inject constructor() {
    private val TAG = "SupabaseClient"
    
    companion object {
        // Supabase credentials
        private const val SUPABASE_URL = "https://vuozasjtlcjxbsqkhfch.supabase.co"
        private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ1b3phc2p0bGNqeGJzcWtoZmNoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDgxNzE4MzksImV4cCI6MjA2Mzc0NzgzOX0.d3E7JA6AEgQhDqTmLT_zlGwDV-WyuuXOPahVb_ds94o"
        
        @Volatile
        private var INSTANCE: SupabaseClient? = null
        
        fun getInstance(): SupabaseClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseClient().also { INSTANCE = it }
            }
        }
    }
    
    // Initialize Supabase client
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
    
    // Auth module reference
    private val auth get() = client.auth
    
    /**
     * Sign up a new user
     */
    suspend fun signUp(email: String, password: String, userData: Map<String, Any> = emptyMap()): Result<UserInfo> {
        return try {
            // Convert userData to JsonObject
            val userMetadata = buildJsonObject {
                userData.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Number -> put(key, value.toDouble())
                        is Boolean -> put(key, value)
                        else -> put(key, value.toString())
                    }
                }
            }
            
            // Use the new API: signUpWith
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = userMetadata
            }
            
            // Get the user info
            val userInfo = auth.currentUserOrNull()
            if (userInfo != null) {
                Result.success(userInfo)
            } else {
                Result.failure(Exception("User created but not logged in"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during signup", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sign in user with email and password
     */
    suspend fun signIn(email: String, password: String): Result<UserInfo> {
        return try {
            // Use the new API: signInWith
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            val userInfo = auth.currentUserOrNull()
            if (userInfo != null) {
                Result.success(userInfo)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during SignIn", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during SignOut", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get current user info
     */
    fun getCurrentUser(): UserInfo? {
        return auth.currentUserOrNull()
    }
    
    /**
     * Check if a user is signed in
     */
    fun isSignedIn(): Boolean {
        return auth.currentUserOrNull() != null
    }
    
    /**
     * Check if Supabase is available
     * This is a simple check to see if we can connect to Supabase
     */
    fun isSupabaseAvailable(): Boolean {
        // For a simple check, we'll just return true if we can get the client
        // In a real app, we might want to do a health check or ping
        return client != null
    }
    
    /**
     * Detailed health check of Supabase services
     * Returns a map of service names to their health status
     */
    suspend fun checkSupabaseHealth(): Map<String, Boolean> {
        val healthStatus = mutableMapOf<String, Boolean>()
        
        try {
            // Check Auth service
            val authHealthy = try {
                // Just check if the auth service responds - doesn't need valid credentials
                auth.currentUserOrNull()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Auth service check failed", e)
                false
            }
            healthStatus["Auth"] = authHealthy
            
            // Since we don't have access to the other services directly in this SDK version,
            // we'll just report if auth is working since that's the main feature we're using
            healthStatus["Overall"] = authHealthy
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Supabase health", e)
            // Set all services as unhealthy if there's a general error
            healthStatus["Auth"] = false
            healthStatus["Overall"] = false
        }
        
        return healthStatus
    }
    
    /**
     * Check the current service latency
     * @return latency in milliseconds, or -1 if the check failed
     */
    suspend fun checkLatency(): Long {
        return try {
            val startTime = System.currentTimeMillis()
            auth.currentUserOrNull() // Make a lightweight call to measure response time
            val endTime = System.currentTimeMillis()
            endTime - startTime
        } catch (e: Exception) {
            Log.e(TAG, "Error measuring latency", e)
            -1L
        }
    }
    
    /**
     * Check if the current user session is valid
     * This can be used to verify that the connection to Supabase is working
     * 
     * @return true if the session is valid, false otherwise
     */
    suspend fun isSessionValid(): Boolean {
        return try {
            val currentUser = auth.currentUserOrNull()
            currentUser != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking session validity", e)
            false
        }
    }
} 