package com.example.recordapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Helper class for image cropping operations
 */
object ImageCropperHelper {
    private const val TAG = "ImageCropperHelper"
    
    /**
     * Create a destination URI for the cropped image
     */
    fun createCropDestinationUri(context: Context): Uri {
        val timestamp = System.currentTimeMillis()
        val destinationFile = File(context.cacheDir, "cropped_image_$timestamp.jpg")
        // Ensure the file can be accessed by the FileProvider
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            destinationFile
        )
    }
    
    /**
     * Configure UCrop options with default settings
     */
    fun getDefaultCropOptions(compressionQuality: Int = FileUtils.DEFAULT_COMPRESSION_QUALITY): UCropWrapper.Options {
        return UCropWrapper.Options().apply {
            setCompressionQuality(compressionQuality)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)
            setToolbarTitle("Crop Image")
            
            // Use material colors
            setToolbarColor(android.graphics.Color.parseColor("#E0F2F1"))
            setStatusBarColor(android.graphics.Color.parseColor("#00796B"))
            setToolbarWidgetColor(android.graphics.Color.parseColor("#004D40"))
            setActiveControlsWidgetColor(android.graphics.Color.parseColor("#009688"))
        }
    }
    
    /**
     * Save a cropped image to the specified folder
     */
    fun saveCroppedImage(context: Context, croppedImageUri: Uri, folderName: String): Uri? {
        try {
            Log.d(TAG, "Saving cropped image to folder: $folderName")
            // Use FileUtils to save the image to the specified folder
            return FileUtils.saveImageToFolder(context, croppedImageUri, folderName)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cropped image: ${e.message}", e)
            return null
        }
    }
} 