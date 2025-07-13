package com.example.recordapp.model

/**
 * Enum representing different types of reports that can be generated
 */
enum class ReportType(val displayName: String) {
    USER_ACTIVITY("User Activity"),
    SYSTEM_PERFORMANCE("System Performance"),
    SECURITY_AUDIT("Security Audit"),
    DATA_USAGE("Data Usage"),
    LOGIN_HISTORY("Login History"),
    SYSTEM_LOGS("System Logs"),
    STORAGE_USAGE("Storage Usage")
}

/**
 * Enum representing export format options
 */
enum class ExportFormat(val displayName: String, val extension: String) {
    CSV("CSV Spreadsheet", "csv")
} 