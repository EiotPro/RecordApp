package com.example.recordapp

import android.app.Application
import android.util.Log
import com.example.recordapp.data.AppDatabase
import com.example.recordapp.repository.ExpenseRepository
import com.example.recordapp.repository.AuthRepository
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.util.FileUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import java.util.concurrent.ConcurrentHashMap

/**
 * Custom Application class for initializing components
 */
class RecordApplication : Application() {
    
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
            
            // Initialize repositories
            ExpenseRepository.getInstance(this)
            AuthRepository.getInstance(this)
            
            // Initialize settings manager
            SettingsManager.getInstance(this)
            
            // Create default folder for images
            FileUtils.createFolderIfNotExists(this, "default")
            
            // Setup any additional error handlers or reporters here
            setupUncaughtExceptionHandler()
            
        } catch (e: Exception) {
            // Log startup errors
            Log.e(TAG, "Error during application startup", e)
            startupErrors["startup"] = e
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