package com.example.recordapp

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.recordapp.data.AppDatabase
import com.example.recordapp.network.SupabaseClient
import com.example.recordapp.repository.ExpenseRepository
import com.example.recordapp.repository.AuthRepository
import com.example.recordapp.util.BackupModule
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.util.FileUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import dagger.hilt.android.HiltAndroidApp
import javax.crypto.AEADBadTagException
import javax.inject.Inject

/**
 * Main application class with Hilt dependency injection
 */
@HiltAndroidApp
class RecordApplication : Application(), Configuration.Provider {
    
    // Injecting dependencies with Hilt
    @Inject
    lateinit var supabaseClient: SupabaseClient
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    // Application scope for coroutines
    private val applicationScope = CoroutineScope(Dispatchers.Default)
    
    companion object {
        private const val TAG = "RecordApplication"
        
        // Global state for tracking startup errors
        private val startupErrors = ConcurrentHashMap<String, Throwable>()
        
        // Global exception handler for uncaught exceptions
        val globalExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Uncaught exception in coroutine", throwable)
            startupErrors["global"] = throwable
        }
        
        // Method to check if there were any startup errors
        fun hasStartupErrors(): Boolean = startupErrors.isNotEmpty()
        
        // Method to get all startup errors
        fun getStartupErrors(): Map<String, Throwable> = startupErrors.toMap()
        
        // Method to clear startup errors
        fun clearStartupErrors() = startupErrors.clear()
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize database
            AppDatabase.getInstance(this)
            
            // Initialize repositories that don't use Hilt yet
            ExpenseRepository.getInstance(this)
            
            // Initialize Auth Repository with better error handling
            initializeAuthRepository()
            
            // Initialize settings manager
            SettingsManager.getInstance(this)
            
            // Create default folder for images
            FileUtils.createFolderIfNotExists(this, "default")
            
            // Setup any additional error handlers or reporters here
            setupUncaughtExceptionHandler()
            
            // Schedule backups based on user preferences
            scheduleBackups()
            
        } catch (e: Exception) {
            // Log startup errors
            Log.e(TAG, "Error during application startup", e)
            startupErrors["startup"] = e
        }
    }
    
    /**
     * Initialize the Auth Repository with better error handling for crypto errors
     */
    private fun initializeAuthRepository() {
        try {
            // Hilt already initialized the AuthRepository
            // Just check if it's available
            if (!::authRepository.isInitialized) {
                Log.w(TAG, "AuthRepository not initialized by Hilt, using manual initialization")
            AuthRepository.getInstance(this)
            }
        } catch (e: Exception) {
            // Check if this is a cryptography-related error
            val rootCause = findRootCause(e)
            if (rootCause is AEADBadTagException || 
                rootCause.toString().contains("KeyStore") || 
                rootCause.toString().contains("crypto")) {
                
                Log.w(TAG, "Encryption error detected. This might be due to restored data from backup " +
                      "or migration from a version with different encryption keys.", e)
                
                // Clear encrypted shared preferences to start fresh
                try {
                    getSharedPreferences("auth_prefs", MODE_PRIVATE)
                        .edit().clear().apply()
                    
                    // Try again after clearing
                    Log.d(TAG, "Retrying AuthRepository initialization after clearing preferences")
                    AuthRepository.getInstance(this)
                } catch (retryEx: Exception) {
                    Log.e(TAG, "Failed to initialize AuthRepository even after clearing preferences", retryEx)
                    startupErrors["auth_repository"] = retryEx
                }
            } else {
                // Not a crypto error, just log it
                Log.e(TAG, "Error initializing AuthRepository", e)
                startupErrors["auth_repository"] = e
            }
        }
    }
    
    /**
     * Find the root cause of an exception by traversing the cause chain
     */
    private fun findRootCause(throwable: Throwable): Throwable {
        var cause = throwable
        while (cause.cause != null && cause.cause != cause) {
            cause = cause.cause!!
        }
        return cause
    }
    
    /**
     * Configure WorkManager
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setExecutor(Executors.newFixedThreadPool(2))
            .build()
    
    /**
     * Schedule backups based on user preferences
     */
    private fun scheduleBackups() {
        applicationScope.launch {
            try {
                val settings = SettingsManager.getInstance(applicationContext)
                val isEnabled = settings.autoBackupEnabled
                val frequency = settings.backupFrequency
                
                Log.d(TAG, "Initializing backup scheduler with enabled=$isEnabled, frequency=$frequency")
                
                // Schedule backups based on settings
                BackupModule.scheduleBackup(
                    context = applicationContext,
                    isEnabled = isEnabled,
                    frequency = frequency
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling backups", e)
                startupErrors["backup_scheduler"] = e
            }
        }
    }
    
    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log the exception
            Log.e(TAG, "Uncaught exception in thread: ${thread.name}", throwable)
            
            // You could report to a crash reporting service here
            
            // Call the default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
} 