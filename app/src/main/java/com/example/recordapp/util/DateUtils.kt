package com.example.recordapp.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Centralized date formatting utilities to eliminate duplicate date formatting code
 * across the application. All date formatting should use these methods for consistency.
 */
object DateUtils {
    
    // Standard formatters
    private val DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    
    /**
     * Format timestamp as readable date
     * @param timestamp Unix timestamp in milliseconds
     * @param includeTime Whether to include time in the output
     * @return Formatted date string
     */
    fun formatTimestamp(timestamp: Long, includeTime: Boolean = false): String {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        return if (includeTime) {
            dateTime.format(DATE_TIME_FORMATTER)
        } else {
            dateTime.format(DATE_ONLY_FORMATTER)
        }
    }
    
    /**
     * Format LocalDateTime as readable date
     * @param dateTime LocalDateTime to format
     * @param includeTime Whether to include time in the output
     * @return Formatted date string
     */
    fun formatDateTime(dateTime: LocalDateTime, includeTime: Boolean = false): String {
        return if (includeTime) {
            dateTime.format(DATE_TIME_FORMATTER)
        } else {
            dateTime.format(DATE_ONLY_FORMATTER)
        }
    }
    
    /**
     * Get formatted date for file names (safe for file systems)
     * @param dateTime Optional LocalDateTime, defaults to current time
     * @return File-safe formatted date string
     */
    fun getFormattedDateForFileName(dateTime: LocalDateTime = LocalDateTime.now()): String {
        return dateTime.format(FILE_NAME_FORMATTER)
    }
    
    /**
     * Get timestamp string for unique file naming
     * @param date Optional Date, defaults to current time
     * @return Timestamp string suitable for file names
     */
    fun getTimestampForFileName(date: Date = Date()): String {
        val dateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
        return dateTime.format(TIMESTAMP_FORMATTER)
    }
    
    /**
     * Get current timestamp as formatted string
     * @param includeTime Whether to include time
     * @return Current timestamp as formatted string
     */
    fun getCurrentFormattedDate(includeTime: Boolean = false): String {
        return formatDateTime(LocalDateTime.now(), includeTime)
    }
}
