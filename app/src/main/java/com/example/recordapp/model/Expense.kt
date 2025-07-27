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
    val timestamp: LocalDateTime = LocalDateTime.now(), // Creation timestamp
    val expenseDateTime: LocalDateTime = LocalDateTime.now(), // Actual expense date/time (editable)
    val serialNumber: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val folderName: String = "default",
    val receiptType: String = "",
    val displayOrder: Int = 0,
    val imageModifiedTimestamp: Long = System.currentTimeMillis() // Track when image was last modified
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
     * Format the expense date/time according to user settings
     * This is the actual date/time of the expense (editable by user)
     */
    fun getFormattedExpenseDateTime(context: Context, includeTime: Boolean = true): String {
        val settingsManager = SettingsManager.getInstance(context)
        return settingsManager.formatDate(expenseDateTime, includeTime)
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
    
    /**
     * Get display name for receipt type
     */
    fun getReceiptTypeDisplayName(): String {
        return when (receiptType) {
            "PHYSICAL_RECEIPT" -> "Physical Receipt"
            "DIGITAL_PAYMENT" -> "Digital Payment"
            "UPI_PAYMENT" -> "UPI Transaction"
            else -> ""
        }
    }
    
    /**
     * Check if the expense has a valid receipt type
     */
    fun hasReceiptType(): Boolean {
        return receiptType.isNotEmpty() && receiptType in listOf("PHYSICAL_RECEIPT", "DIGITAL_PAYMENT", "UPI_PAYMENT")
    }
    
    /**
     * Get a safe serial number that is never null
     * This is a utility method to ensure serialNumber is always a string, even if null somehow
     */
    fun getSafeSerialNumber(): String {
        return serialNumber
    }
    
    /**
     * Check if the expense has a serial number
     */
    fun hasSerialNumber(): Boolean {
        return serialNumber.isNotBlank()
    }
    
    /**
     * Get a serial number for export purposes
     * If serialNumber is blank, generates a random value
     * This ensures every expense has a serial number in exports
     * while preserving original serial numbers when they exist
     */
    fun getExportSerialNumber(prefix: String = "EXP-"): String {
        return if (serialNumber.isBlank()) {
            // Generate a truly random serial number only for blank entries
            prefix + UUID.randomUUID().toString().substring(0, 8)
        } else {
            // Preserve the original serial number exactly as entered
            serialNumber
        }
    }
}