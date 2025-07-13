package com.example.recordapp.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.os.Environment
import android.util.Log

/**
 * Utility class for handling permissions
 */
object PermissionUtils {
    private const val TAG = "PermissionUtils"
    private const val PREFS_NAME = "RecordAppPermissions"
    private const val KEY_STORAGE_PERMISSION_GRANTED = "storage_permission_granted"

    /**
     * Check if storage permissions are granted
     */
    fun hasStoragePermissions(context: Context): Boolean {
        // Check if permission is granted based on Android version
        val isPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+, check if we have all-files access permission
            Environment.isExternalStorageManager()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+)
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 (API 29)
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 9 and below (API 28 and below)
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        // If permission is granted, save the state
        if (isPermissionGranted) {
            savePermissionGranted(context, true)
        }
        
        return isPermissionGranted
    }
    
    /**
     * Save the permission granted state to SharedPreferences
     */
    private fun savePermissionGranted(context: Context, granted: Boolean) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_STORAGE_PERMISSION_GRANTED, granted).apply()
            Log.d(TAG, "Storage permission state saved: $granted")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving permission state", e)
        }
    }
    
    /**
     * Check if permission was previously granted
     */
    fun wasPermissionPreviouslyGranted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_STORAGE_PERMISSION_GRANTED, false)
    }
    
    /**
     * Reset permission state when logging out
     */
    fun resetPermissionState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_STORAGE_PERMISSION_GRANTED).apply()
            Log.d(TAG, "Storage permission state reset")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting permission state", e)
        }
    }

    /**
     * Get required permissions based on Android version
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()

        // Storage permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Android 9 and below
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            // Android 10-12
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        return permissions.toTypedArray()
    }
    
    /**
     * Get an intent to open settings for the app to request MANAGE_EXTERNAL_STORAGE
     * for Android 11+ if needed
     */
    fun getManageExternalStorageIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
} 