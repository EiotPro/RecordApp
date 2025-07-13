package com.example.recordapp.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Utility that monitors memory usage to prevent OOM crashes during heavy image loading.
 * Provides callbacks for adaptive behavior during low memory conditions.
 */
object MemoryMonitorUtil {
    private val _memoryState = MutableStateFlow(MemoryState.NORMAL)
    val memoryState: StateFlow<MemoryState> = _memoryState
    
    // Thresholds for memory conditions
    private const val LOW_MEMORY_THRESHOLD = 0.2f // 20% of memory left
    private const val CRITICAL_MEMORY_THRESHOLD = 0.15f // 15% of memory left
    
    /**
     * Memory state levels for determining image loading strategy
     */
    enum class MemoryState {
        NORMAL,    // Normal memory usage, load images at full quality
        LOW,       // Low memory, implement more aggressive caching and lower quality images
        CRITICAL   // Critical memory state, clear caches, stop preloading, use minimal image quality
    }
    
    /**
     * Check current memory state and update the memory state flow
     */
    fun checkMemoryState(context: Context): MemoryState {
        val memInfo = getMemoryInfo(context)
        val availPercent = memInfo.availMem.toFloat() / memInfo.totalMem
        
        val newState = when {
            availPercent < CRITICAL_MEMORY_THRESHOLD -> {
                // Take actions to free up memory
                try {
                    // Try to reset the ImageLoader singleton if it exists
                    Class.forName("com.example.recordapp.util.AppImageLoader")
                        ?.let { 
                            val method = it.getDeclaredMethod("resetInstance")
                            method.invoke(null)
                        }
                } catch (e: Exception) {
                    // Ignore if resetInstance method doesn't exist
                }
                
                // Request garbage collection
                System.gc()
                MemoryState.CRITICAL
            }
            availPercent < LOW_MEMORY_THRESHOLD -> {
                MemoryState.LOW
            }
            else -> {
                MemoryState.NORMAL
            }
        }
        
        if (newState != _memoryState.value) {
            _memoryState.value = newState
        }
        
        return newState
    }
    
    /**
     * Get memory info for the device
     */
    private fun getMemoryInfo(context: Context): ActivityManager.MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }
    
    /**
     * Get available memory in MB
     */
    fun getAvailableMemoryMB(context: Context): Int {
        val memoryInfo = getMemoryInfo(context)
        return (memoryInfo.availMem / (1024 * 1024)).toInt()
    }
    
    /**
     * Get total memory in MB
     */
    fun getTotalMemoryMB(context: Context): Int {
        val memoryInfo = getMemoryInfo(context)
        return (memoryInfo.totalMem / (1024 * 1024)).toInt()
    }
    
    /**
     * Check if the device is in a low memory condition
     */
    fun isLowMemory(context: Context): Boolean {
        return getMemoryInfo(context).lowMemory
    }
    
    /**
     * Get current process memory usage in MB
     */
    fun getProcessMemoryMB(context: Context): Int {
        // Use Debug.MemoryInfo instead of ActivityManager.RunningAppProcessInfo
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        // totalPss is in KB, convert to MB
        return (memoryInfo.totalPss / 1024)
    }
    
    /**
     * Adaptive image quality based on memory conditions
     * Returns a quality multiplier between 0.5 and 1.0
     */
    fun getAdaptiveQuality(context: Context): Float {
        return when (checkMemoryState(context)) {
            MemoryState.NORMAL -> 1.0f
            MemoryState.LOW -> 0.75f
            MemoryState.CRITICAL -> 0.5f
        }
    }
    
    /**
     * Attempt to free memory when the app is under memory pressure
     */
    fun attemptMemoryCleanup() {
        try {
            // Run garbage collector
            System.gc()
            
            // Wait a bit to let GC finish
            Thread.sleep(100)
            
            // Try to reset the ImageLoader singleton if it exists
            Class.forName("com.example.recordapp.util.AppImageLoader")
                .getMethod("resetInstance")
                .invoke(null)
            
            // Clear any bitmap caches
            // ... more cleanup logic ...
        } catch (e: Exception) {
            // Ignore exceptions during cleanup
        }
    }
} 