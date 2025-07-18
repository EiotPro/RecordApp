package com.example.recordapp.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.yalantis.ucrop.UCrop
import com.example.recordapp.util.UcropFileUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for UCrop operations - completely rewritten to address ENOENT errors
 */
object UcropHelper {
    private const val TAG = "UcropHelper"
    
    /**
     * Create UCrop options with optimal settings
     * Used internally by startCrop method
     */
    private fun getOptions(): UCrop.Options {
        return UCrop.Options().apply {
            // Image quality settings
            setCompressionQuality(90)
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            
            // UI controls
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)
            setShowCropGrid(true)
            setShowCropFrame(true)
            setCircleDimmedLayer(false)
            
            // UI theming
            setToolbarTitle("Crop Image")
            setToolbarColor("#E0F2F1".toColorInt())
            setStatusBarColor("#00796B".toColorInt())
            setToolbarWidgetColor("#004D40".toColorInt()) 
            setActiveControlsWidgetColor("#009688".toColorInt())
            
            // No logo needed
            setLogoColor(android.graphics.Color.TRANSPARENT)
        }
    }
    
    /**
     * Create a destination URI for the cropped image using external cache directory
     */
    private fun createDestinationUri(context: Context): Uri {
        try {
            // IMPORTANT: Use externalCacheDir which is world-readable by the UCrop library
            // Internal files dir won't work because the UCrop library cannot access it directly
            val cachePath = context.externalCacheDir
            if (cachePath == null) {
                Log.e(TAG, "External cache directory is null, falling back to internal cache")
                return createFallbackUri(context)
            }
            
            // Create a subfolder in the cache directory specifically for UCrop
            val cropDir = File(cachePath, "crop_cache")
            if (!cropDir.exists()) {
                val created = cropDir.mkdirs()
                Log.d(TAG, "Created crop directory in external cache: $created")
                
                // Verify directory was created
                if (!created && !cropDir.exists()) {
                    Log.e(TAG, "Failed to create crop directory in external cache")
                    return createFallbackUri(context)
                }
            }
            
            // Set read/write permissions for the directory
            // SECURITY NOTE: We need to make this world-readable/writable because 
            // UCrop library accesses these files directly by path
            // This is necessary to avoid ENOENT errors, but limited to cache directories only
            @Suppress("SetWorldReadable", "SetWorldWritable")
            try {
                cropDir.setReadable(true, false) // World readable
                cropDir.setWritable(true, false) // World writable
            } catch (e: Exception) {
                Log.e(TAG, "Error setting directory permissions: ${e.message}")
            }
            
            // Generate unique filename with timestamp and milliseconds
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "crop_${timestamp}_${System.currentTimeMillis()}.jpg"
            
            // Create the destination file
            val destFile = File(cropDir, fileName)
            
            // Create empty file to ensure path exists
            try {
                val created = destFile.createNewFile()
                Log.d(TAG, "Created destination file: $created at ${destFile.absolutePath}")
                
                // Set permissions on the file
                // Required for UCrop library to access the file directly
                @Suppress("SetWorldReadable", "SetWorldWritable")
                destFile.setReadable(true, false)
                destFile.setWritable(true, false)
                
                // Verify file creation
                if (!created && !destFile.exists()) {
                    Log.e(TAG, "Failed to create destination file")
                    return createFallbackUri(context)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to create destination file: ${e.message}")
                return createFallbackUri(context)
            }
            
            Log.d(TAG, "Using destination file at: ${destFile.absolutePath}")
            
            // Convert to content URI via FileProvider
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                destFile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating destination URI: ${e.message}", e)
            return createFallbackUri(context)
        }
    }
    
    /**
     * Create a fallback URI in the app's cache directory
     */
    private fun createFallbackUri(context: Context): Uri {
        try {
            Log.d(TAG, "Using fallback URI creation in cache directory")
            
            // Try the internal cache directory as last resort
            val tempFile = File.createTempFile("crop_", ".jpg", context.cacheDir)
            
            // Set permissions - required for UCrop to access the file
            @Suppress("SetWorldReadable", "SetWorldWritable")
            tempFile.setReadable(true, false)
            tempFile.setWritable(true, false)
            
            // Verify file was created
            if (!tempFile.exists()) {
                throw IOException("Failed to create temporary file")
            }
            
            Log.d(TAG, "Created fallback file at: ${tempFile.absolutePath}")
            
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            )
        } catch (fallbackException: Exception) {
            Log.e(TAG, "Fallback URI creation failed", fallbackException)
            throw IllegalStateException("Failed to create any valid destination URI", fallbackException)
        }
    }
    
    /**
     * Helper method to create a URI in the specified directory
     * 
     * @deprecated This method is no longer used as we've switched to a different approach
     * but kept for reference in case we need to revert changes.
     */
    @Deprecated("No longer used with the new implementation")
    private fun createUriInDirectory(context: Context, directory: File): Uri {
        // Generate unique filename
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "crop_${timestamp}_${System.currentTimeMillis()}.jpg"
        
        // Create the destination file
        val destFile = File(directory, fileName)
        
        // Verify path exists and create empty file
        if (!destFile.parentFile!!.exists()) {
            destFile.parentFile!!.mkdirs()
        }
        
        // Create empty file to ensure path exists
        try {
            val created = destFile.createNewFile()
            Log.d(TAG, "Created destination file: $created at ${destFile.absolutePath}")
            
            // Verify file creation
            if (!created && !destFile.exists()) {
                throw IOException("Failed to create destination file")
            }
            
            // Test file is writable
            if (!destFile.canWrite()) {
                Log.w(TAG, "Destination file is not writable: ${destFile.absolutePath}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create destination file: ${e.message}")
            throw e
        }
        
        Log.d(TAG, "Created destination file at: ${destFile.absolutePath}")
        
        // Convert to content URI via FileProvider
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            destFile
        )
    }
    
    /**
     * Start a new UCrop operation with the given source URI
     * This is used by ImageCaptureDialog to start cropping
     */
    @Suppress("unused") // Used in ImageCaptureDialog.kt
    fun startCrop(context: Context, sourceUri: Uri): Intent {
        try {
            // Use the new utility class to prepare source image
            val localSourceUri = UcropFileUtils.prepareImageForUCrop(context, sourceUri)
            
            // Create destination URI in external cache directory
            val destinationUri = createDestinationUri(context)
            
            Log.d(TAG, "Starting crop with source: $localSourceUri and destination: $destinationUri")
            
            // Log detailed information about source and destination
            try {
                val srcScheme = localSourceUri.scheme
                val srcPath = localSourceUri.path
                Log.d(TAG, "Source URI details - scheme: $srcScheme, path: $srcPath")
                
                val destScheme = destinationUri.scheme
                val destPath = destinationUri.path
                Log.d(TAG, "Destination URI details - scheme: $destScheme, path: $destPath")
            } catch (e: Exception) {
                Log.e(TAG, "Error logging URI details", e)
            }
            
            // Create the UCrop object with source and destination
            val uCrop = UCrop.of(localSourceUri, destinationUri)
                .withOptions(getOptions())
            
            // Return the intent with permissions
            return uCrop.getIntent(context).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                
                // Add extras to track file paths for debugging
                putExtra("SOURCE_PATH", localSourceUri.toString())
                putExtra("DEST_PATH", destinationUri.toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating UCrop intent: ${e.message}", e)
            throw IllegalStateException("Failed to create crop intent: ${e.message}", e)
        }
    }
    
    /**
     * Create a local copy of the source URI in the external cache directory
     * This solves potential issues with content:// URIs
     * 
     * @deprecated This method is no longer used as we've switched to UcropFileUtils
     * but kept for reference in case we need to revert changes.
     */
    @Deprecated("Replaced by UcropFileUtils.prepareImageForUCrop")
    private fun copyUriToLocalCache(context: Context, sourceUri: Uri): Uri {
        try {
            // First check if URI is accessible
            if (!isUriAccessible(context, sourceUri)) {
                Log.e(TAG, "Source URI is not accessible: $sourceUri")
                throw IOException("Source URI is not accessible")
            }
            
            // Use external cache directory which is world-readable
            val cachePath = context.externalCacheDir
            if (cachePath == null) {
                Log.e(TAG, "External cache directory is null, falling back to internal cache")
                // Use internal cache directory as fallback
                val fallbackDir = File(context.cacheDir, "source_images")
                if (!fallbackDir.exists()) {
                    fallbackDir.mkdirs()
                }
                
                if (!fallbackDir.exists()) {
                    throw IOException("Failed to create any directory for source images")
                }
                
                return copyToDirectory(context, sourceUri, fallbackDir, true)
            }
            
            // Create temporary file in external cache directory
            val tempDir = File(cachePath, "source_images")
            if (!tempDir.exists()) {
                val created = tempDir.mkdirs()
                Log.d(TAG, "Created source images directory in external cache: $created")
                
                // If directory creation failed, try internal cache directory as fallback
                if (!created && !tempDir.exists()) {
                    Log.e(TAG, "Failed to create source images directory in external cache")
                    
                    // Use internal cache directory as fallback
                    val fallbackDir = File(context.cacheDir, "source_images")
                    fallbackDir.mkdirs()
                    
                    if (!fallbackDir.exists()) {
                        throw IOException("Failed to create any directory for source images")
                    }
                    
                    return copyToDirectory(context, sourceUri, fallbackDir, true)
                }
            }
            
            // Set directory permissions - required for UCrop library access
            // SECURITY NOTE: This is necessary for the UCrop library which accesses files directly
            // The directory is in the app's cache, isolated from other apps
            @Suppress("SetWorldReadable", "SetWorldWritable")
            try {
                tempDir.setReadable(true, false)
                tempDir.setWritable(true, false)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting directory permissions: ${e.message}")
            }
            
            return copyToDirectory(context, sourceUri, tempDir, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to local cache: ${e.message}", e)
            // Just return the original URI if copy fails
            return sourceUri
        }
    }
    
    /**
     * Helper method to copy a URI to a specific directory
     * 
     * @param isInternal True if the directory is in internal storage (need special permission handling)
     */
    private fun copyToDirectory(context: Context, sourceUri: Uri, directory: File, isInternal: Boolean = false): Uri {
        val fileName = "source_${System.currentTimeMillis()}.jpg"
        val destFile = File(directory, fileName)
        
        // Copy the content
        context.contentResolver.openInputStream(sourceUri).use { input ->
            if (input == null) {
                throw IOException("Failed to open input stream")
            }
            
            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(4 * 1024) // 4K buffer
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }
        
        // Set file permissions - very important for UCrop to be able to read the file directly
        // SECURITY NOTE: This is necessary for the UCrop library which accesses files directly
        @Suppress("SetWorldReadable", "SetWorldWritable")
        try {
            destFile.setReadable(true, false) // World readable
            destFile.setWritable(true, false) // World writable
        } catch (e: Exception) {
            Log.e(TAG, "Error setting file permissions: ${e.message}")
        }
        
        Log.d(TAG, "Copied source URI to local file: ${destFile.absolutePath} (isInternal: $isInternal)")
        
        // Convert to content URI
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            destFile
        )
    }
    
    /**
     * Check if a URI is accessible
     */
    fun isUriAccessible(context: Context, uri: Uri?): Boolean {
        if (uri == null) return false
        
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                // Successfully opened input stream
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "URI is not accessible: $uri", e)
            false
        }
    }
    
    /**
     * Save a cropped image to a specific folder
     * This is used by ImageCaptureDialog to save cropped images
     */
    @Suppress("unused") // Used in ImageCaptureDialog.kt
    fun saveCroppedImage(context: Context, croppedImageUri: Uri, folderName: String): Uri? {
        try {
            Log.d(TAG, "Saving cropped image to folder: $folderName")
            
            if (!isUriAccessible(context, croppedImageUri)) {
                Log.e(TAG, "Cropped image URI is not accessible: $croppedImageUri")
                return null
            }
            
            // Save the image to the specified folder
            val savedUri = com.example.recordapp.util.FileUtils.saveImageToFolder(context, croppedImageUri, folderName)
            
            // Try to clean up temporary files regardless of result
            try {
                cleanupTempFiles(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up temp files: ${e.message}")
                // Non-fatal
            }
            
            if (savedUri != null) {
                Log.d(TAG, "Successfully saved cropped image to: $savedUri")
            } else {
                Log.e(TAG, "Failed to save cropped image to folder: $folderName")
            }
            
            return savedUri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cropped image: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Clean up temporary files created during cropping
     */
    private fun cleanupTempFiles(context: Context) {
        // Get all storage locations
        val externalCache = context.externalCacheDir
        val internalCache = context.cacheDir
        val internalFiles = context.filesDir
        
        try {
            // Clean internal files directories
            if (internalFiles != null) {
                val sourceDir = File(internalFiles, "source_images")
                if (sourceDir.exists() && sourceDir.isDirectory) {
                    sourceDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.startsWith("source_")) {
                            val deleted = file.delete()
                            Log.d(TAG, "Deleted source file from internal files: ${file.name}, success: $deleted")
                        }
                    }
                }
                
                val cropDir = File(internalFiles, "crop_images")
                if (cropDir.exists() && cropDir.isDirectory) {
                    cropDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.startsWith("crop_")) {
                            val deleted = file.delete()
                            Log.d(TAG, "Deleted crop file from internal files: ${file.name}, success: $deleted")
                        }
                    }
                }
            }
            
            // Clean external cache first
            if (externalCache != null) {
                val sourceDir = File(externalCache, "source_images")
                if (sourceDir.exists() && sourceDir.isDirectory) {
                    sourceDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.startsWith("source_")) {
                            file.delete()
                        }
                    }
                }
                
                val cropDir = File(externalCache, "crop_cache")
                if (cropDir.exists() && cropDir.isDirectory) {
                    cropDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.startsWith("crop_")) {
                            file.delete()
                        }
                    }
                }
            }
            
            // Clean internal cache
            if (internalCache != null) {
                val sourceDir = File(internalCache, "source_images")
                if (sourceDir.exists() && sourceDir.isDirectory) {
                    sourceDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.startsWith("source_")) {
                            file.delete()
                        }
                    }
                }
                
                val cropDir = File(internalCache, "crop_cache")
                if (cropDir.exists() && cropDir.isDirectory) {
                    cropDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.startsWith("crop_")) {
                            file.delete()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
    
    /**
     * Extract error from UCrop result intent
     * This is used by ImageCaptureDialog to display error messages
     */
    @Suppress("unused") // Used in ImageCaptureDialog.kt
    fun getError(intent: Intent?): Throwable? {
        if (intent == null) return null
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra("com.yalantis.ucrop.Error", Throwable::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra("com.yalantis.ucrop.Error") as? Throwable
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting UCrop error: ${e.message}", e)
            null
        }
    }
} 