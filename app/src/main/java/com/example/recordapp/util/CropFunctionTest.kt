package com.example.recordapp.util

import android.content.Context
import android.net.Uri
import android.util.Log

/**
 * Test class to verify UcropHelper works correctly
 */
object CropFunctionTest {
    private const val TAG = "CropFunctionTest"
    
    /**
     * Test function to verify UcropHelper imports and functions correctly
     */
    fun testUcropHelperFunctionality(context: Context, uri: Uri) {
        Log.d(TAG, "Testing UCrop Helper functionality")
        
        try {
            // Test if we can access the UcropHelper and UcropFileUtils classes
            val isAccessible = UcropHelper.isUriAccessible(context, uri)
            Log.d(TAG, "URI is accessible: $isAccessible")
            
            // Test preparing an image
            val preparedUri = UcropFileUtils.prepareImageForUCrop(context, uri)
            Log.d(TAG, "Image prepared successfully: $preparedUri")
            
            Log.d(TAG, "UCrop Helper test completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error testing UCrop Helper: ${e.message}", e)
        }
    }
} 