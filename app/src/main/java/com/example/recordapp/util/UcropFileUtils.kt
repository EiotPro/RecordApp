package com.example.recordapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class to help UCrop library with file operations
 * This is needed because UCrop tries to use absolute file paths
 * instead of content:// URIs in some cases
 */
object UcropFileUtils {
    private const val TAG = "UcropFileUtils"

    /**
     * Ensure a directory exists and has proper permissions
     */
    fun ensureDirectoryExists(directory: File): Boolean {
        // Create directory if it doesn't exist
        if (!directory.exists()) {
            val success = directory.mkdirs()
            if (!success && !directory.exists()) {
                Log.e(TAG, "Failed to create directory: ${directory.absolutePath}")
                return false
            }
        }
        
        // Set permissions - required for UCrop to access files directly
        @Suppress("SetWorldReadable", "SetWorldWritable")
        try {
            directory.setReadable(true, false)
            directory.setWritable(true, false)
        } catch (e: Exception) {
            Log.w(TAG, "Could not set directory permissions (this may be normal on newer Android versions): ${e.message}")
            // This is not fatal - UCrop may still work through content URIs
        }
        
        return directory.exists() && directory.isDirectory
    }
    
    /**
     * Get or create the appropriate working directory for UCrop
     */
    fun getUCropWorkingDir(context: Context): File {
        // First try external cache directory
        val externalCache = context.externalCacheDir
        if (externalCache != null) {
            val dir = File(externalCache, "crop_cache")
            if (ensureDirectoryExists(dir)) {
                return dir
            }
        }
        
        // Fall back to internal cache
        val dir = File(context.cacheDir, "crop_cache")
        ensureDirectoryExists(dir)
        return dir
    }
    
    /**
     * Convert a content URI to a file that UCrop can use directly
     */
    fun prepareImageForUCrop(context: Context, sourceUri: Uri): Uri {
        try {
            // Check if we already have a file URI
            val scheme = sourceUri.scheme
            if (scheme == "file") {
                val file = File(sourceUri.path ?: "")
                if (file.exists()) {
                    // Set permissions to ensure UCrop can read it directly
                    @Suppress("SetWorldReadable", "SetWorldWritable")
                    try {
                        file.setReadable(true, false)
                        file.setWritable(true, false)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not set file permissions (this may be normal on newer Android versions): ${e.message}")
                        // This is not fatal - UCrop may still work through content URIs
                    }
                    return sourceUri
                }
            }
            
            // Get destination directory
            val destDir = getUCropWorkingDir(context)
            
            // Create a new file
            val fileName = "source_${System.currentTimeMillis()}.jpg"
            val destFile = File(destDir, fileName)
            
            // Copy content
            context.contentResolver.openInputStream(sourceUri).use { input ->
                if (input == null) {
                    throw IOException("Failed to open input stream")
                }
                
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Set permissions - required for UCrop library
            @Suppress("SetWorldReadable", "SetWorldWritable")
            try {
                destFile.setReadable(true, false)
                destFile.setWritable(true, false)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set file permissions (this may be normal on newer Android versions): ${e.message}")
                // This is not fatal - UCrop may still work through content URIs
            }
            
            // CRITICAL FIX: Return file URI directly for UCrop compatibility
            // UCrop has issues with content URIs on some Android versions
            return Uri.fromFile(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing image for UCrop: ${e.message}", e)
            return sourceUri // Return original as fallback
        }
    }
} 