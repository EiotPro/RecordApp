package com.example.recordapp.model

import android.content.Context
import android.net.Uri
import com.example.recordapp.util.SettingsManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Data class representing an expense record with image and timestamp
 */
data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val imagePath: Uri? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val serialNumber: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val folderName: String = "default"
) {
    /**
     * Format the timestamp for display
     * 
     * Note: This method is kept for backward compatibility,
     * but newer code should use getFormattedDate with SettingsManager instead
     */
    fun getFormattedTimestamp(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return timestamp.format(formatter)
    }
    
    /**
     * Format the timestamp according to user settings
     */
    fun getFormattedDate(context: Context, includeTime: Boolean = true): String {
        val settingsManager = SettingsManager.getInstance(context)
        return settingsManager.formatDate(timestamp, includeTime)
    }
    
    /**
     * Format the amount as currency
     * 
     * Note: This method is kept for backward compatibility,
     * but newer code should use SettingsManager.formatAmount() instead
     */
    fun getFormattedAmount(): String {
        return "₹ %.2f".format(amount)
    }
} 