package com.example.recordapp.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import android.util.Log
import com.example.recordapp.model.Expense
import com.example.recordapp.model.GridSize
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Manager for app settings using SharedPreferences
 */
class SettingsManager private constructor(private val appContext: Context) {
    
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "SettingsManager"
        private const val PREFS_NAME = "RecordAppSettings"
        
        // Keys for SharedPreferences
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_DEFAULT_GRID = "default_grid"
        private const val KEY_PDF_QUALITY = "pdf_quality"
        private const val KEY_THEME = "theme"
        private const val KEY_STORAGE_LOCATION = "storage_location"
        private const val KEY_OCR_SENSITIVITY = "ocr_sensitivity"
        private const val KEY_EXPENSE_SORT = "expense_sort"
        
        // New setting keys
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_AUTO_BACKUP = "auto_backup"
        private const val KEY_BACKUP_FREQUENCY = "backup_frequency"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_ANIMATIONS_ENABLED = "animations_enabled"
        private const val KEY_BIOMETRIC_AUTH = "biometric_auth"
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_DEFAULT_FOLDER = "default_folder"
        private const val KEY_IMAGE_COMPRESSION = "image_compression"
        private const val KEY_BACKUP_CACHE_DIRECTORY = "backup_cache_directory"
        private const val KEY_CUSTOM_BACKUP_FOLDER_URI = "custom_backup_folder_uri"
        
        // Default values
        private const val DEFAULT_CURRENCY_SYMBOL = "₹"
        private const val DEFAULT_PDF_QUALITY = 90 // 0-100 scale
        private const val DEFAULT_THEME = "system"
        private const val DEFAULT_STORAGE_LOCATION = "downloads"
        private const val DEFAULT_OCR_SENSITIVITY = "medium"
        private const val DEFAULT_EXPENSE_SORT = "newest"
        
        // Default values for new settings
        private const val DEFAULT_NOTIFICATIONS_ENABLED = true
        private const val DEFAULT_AUTO_BACKUP = false
        private const val DEFAULT_BACKUP_FREQUENCY = "weekly"
        private const val DEFAULT_ACCENT_COLOR = "blue"
        private const val DEFAULT_TEXT_SIZE = "medium"
        private const val DEFAULT_ANIMATIONS_ENABLED = true
        private const val DEFAULT_BIOMETRIC_AUTH = false
        private const val DEFAULT_DATE_FORMAT = "dd/MM/yyyy"
        private const val DEFAULT_FOLDER = "default"
        private const val DEFAULT_IMAGE_COMPRESSION = 80
        private const val DEFAULT_BACKUP_CACHE_DIRECTORY = false
        private const val DEFAULT_CUSTOM_BACKUP_FOLDER_URI = ""
        
        @Volatile
        private var INSTANCE: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Currency symbol to use for displaying amounts
     */
    var currencySymbol: String
        get() = prefs.getString(KEY_CURRENCY_SYMBOL, DEFAULT_CURRENCY_SYMBOL) ?: DEFAULT_CURRENCY_SYMBOL
        set(value) = prefs.edit { putString(KEY_CURRENCY_SYMBOL, value) }
    
    /**
     * Default grid size for PDF exports
     */
    var defaultGridSize: String
        get() = prefs.getString(KEY_DEFAULT_GRID, GridSize.ONE_BY_ONE.name) ?: GridSize.ONE_BY_ONE.name
        set(value) = prefs.edit { putString(KEY_DEFAULT_GRID, value) }
    
    /**
     * PDF image quality (0-100)
     */
    var pdfQuality: Int
        get() = prefs.getInt(KEY_PDF_QUALITY, DEFAULT_PDF_QUALITY)
        set(value) = prefs.edit { putInt(KEY_PDF_QUALITY, value.coerceIn(10, 100)) }
    
    /**
     * App theme (light, dark, or system)
     */
    var appTheme: String
        get() = prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
        set(value) = prefs.edit { putString(KEY_THEME, value) }
    
    /**
     * Storage location for exported files
     */
    var storageLocation: String
        get() = prefs.getString(KEY_STORAGE_LOCATION, DEFAULT_STORAGE_LOCATION) ?: DEFAULT_STORAGE_LOCATION
        set(value) = prefs.edit { putString(KEY_STORAGE_LOCATION, value) }
    
    /**
     * OCR sensitivity level
     */
    var ocrSensitivity: String
        get() = prefs.getString(KEY_OCR_SENSITIVITY, DEFAULT_OCR_SENSITIVITY) ?: DEFAULT_OCR_SENSITIVITY
        set(value) = prefs.edit { putString(KEY_OCR_SENSITIVITY, value) }
    
    /**
     * Default sort order for expenses
     */
    var expenseSortOrder: String
        get() = prefs.getString(KEY_EXPENSE_SORT, DEFAULT_EXPENSE_SORT) ?: DEFAULT_EXPENSE_SORT
        set(value) = prefs.edit { putString(KEY_EXPENSE_SORT, value) }
    
    /**
     * Enable or disable notifications
     */
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, DEFAULT_NOTIFICATIONS_ENABLED)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, value) }
    
    /**
     * Enable or disable automatic backups
     */
    var autoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP, DEFAULT_AUTO_BACKUP)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_BACKUP, value) }
    
    /**
     * Backup frequency (daily, weekly, monthly)
     */
    var backupFrequency: String
        get() = prefs.getString(KEY_BACKUP_FREQUENCY, DEFAULT_BACKUP_FREQUENCY) ?: DEFAULT_BACKUP_FREQUENCY
        set(value) = prefs.edit { putString(KEY_BACKUP_FREQUENCY, value) }
    
    /**
     * Accent color for UI elements
     */
    var accentColor: String
        get() = prefs.getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR) ?: DEFAULT_ACCENT_COLOR
        set(value) = prefs.edit { putString(KEY_ACCENT_COLOR, value) }
    
    /**
     * Text size (small, medium, large)
     */
    var textSize: String
        get() = prefs.getString(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE) ?: DEFAULT_TEXT_SIZE
        set(value) = prefs.edit { putString(KEY_TEXT_SIZE, value) }
    
    /**
     * Enable or disable animations
     */
    var animationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANIMATIONS_ENABLED, DEFAULT_ANIMATIONS_ENABLED)
        set(value) = prefs.edit { putBoolean(KEY_ANIMATIONS_ENABLED, value) }
    
    /**
     * Enable or disable biometric authentication
     */
    var biometricAuthEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_AUTH, DEFAULT_BIOMETRIC_AUTH)
        set(value) = prefs.edit { putBoolean(KEY_BIOMETRIC_AUTH, value) }
    
    /**
     * Date format pattern (always dd/MM/yyyy)
     */
    var dateFormat: String
        get() = prefs.getString(KEY_DATE_FORMAT, DEFAULT_DATE_FORMAT) ?: DEFAULT_DATE_FORMAT
        set(value) = prefs.edit { putString(KEY_DATE_FORMAT, value) }
    
    /**
     * Default folder for new expenses
     */
    var defaultFolder: String
        get() = prefs.getString(KEY_DEFAULT_FOLDER, DEFAULT_FOLDER) ?: DEFAULT_FOLDER
        set(value) = prefs.edit { putString(KEY_DEFAULT_FOLDER, value) }
    
    /**
     * Image compression quality (0-100)
     */
    var imageCompression: Int
        get() = prefs.getInt(KEY_IMAGE_COMPRESSION, DEFAULT_IMAGE_COMPRESSION)
        set(value) = prefs.edit { putInt(KEY_IMAGE_COMPRESSION, value.coerceIn(10, 100)) }
    
    /**
     * Whether to use cache directory for backups
     */
    var useBackupCacheDirectory: Boolean
        get() = prefs.getBoolean(KEY_BACKUP_CACHE_DIRECTORY, DEFAULT_BACKUP_CACHE_DIRECTORY)
        set(value) = prefs.edit { putBoolean(KEY_BACKUP_CACHE_DIRECTORY, value) }
    
    /**
     * Custom backup folder URI string
     */
    var customBackupFolderUri: String
        get() = prefs.getString(KEY_CUSTOM_BACKUP_FOLDER_URI, DEFAULT_CUSTOM_BACKUP_FOLDER_URI) ?: DEFAULT_CUSTOM_BACKUP_FOLDER_URI
        set(value) = prefs.edit { putString(KEY_CUSTOM_BACKUP_FOLDER_URI, value) }
    
    /**
     * Clear all preferences
     */
    fun clearAll() {
        prefs.edit { clear() }
    }
    
    /**
     * Format amount according to user's currency preference
     */
    fun formatAmount(amount: Double): String {
        // Add space between currency symbol and amount for better readability
        return if (currencySymbol.isEmpty()) {
            "%.2f".format(amount)
        } else {
            "$currencySymbol %.2f".format(amount)
        }
    }
    
    /**
     * Format date according to fixed DD/MM/YYYY format
     * Provides both date-only and date-time formatting options
     */
    fun formatDate(dateTime: LocalDateTime, includeTime: Boolean = false): String {
        // Always use DD/MM/YYYY format
        val pattern = if (includeTime) "dd/MM/yyyy HH:mm:ss" else "dd/MM/yyyy"
        
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return dateTime.format(formatter)
    }
    
    /**
     * Apply sort preferences to a list of expenses
     */
    fun applySortPreference(expenses: List<Expense>): List<Expense> {
        return when (expenseSortOrder) {
            "newest" -> expenses.sortedByDescending { it.timestamp }
            "oldest" -> expenses.sortedBy { it.timestamp }
            "amount_high" -> expenses.sortedByDescending { it.amount }
            "amount_low" -> expenses.sortedBy { it.amount }
            else -> expenses.sortedByDescending { it.timestamp } // Default to newest first
        }
    }
    
    /**
     * Reload settings from SharedPreferences
     * This is useful after settings have been restored from backup
     */
    fun reload() {
        synchronized(this) {
            // Force a commit of any pending changes
            prefs.edit().apply()
            
            // Log reload operation
            Log.d(TAG, "Settings reloaded from SharedPreferences")
            
            // Note: We can't reassign the prefs val, but SharedPreferences reload is not necessary
            // since SharedPreferences itself is managed by the Android system
        }
    }
} 