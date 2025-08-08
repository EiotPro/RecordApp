package com.example.recordapp.util

import android.content.Context
import com.example.recordapp.model.User
import com.example.recordapp.repository.AuthRepository
import com.example.recordapp.model.ReportType
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility class for generating administrative reports
 */
class ReportGenerator(
    private val context: Context,
    private val csvUtils: CsvUtils,
    private val pdfUtils: PdfUtils
) {
    
    private val authRepository = AuthRepository.getInstance(context)
    
    /**
     * Generate a report based on the specified type and date range
     * 
     * @param type The type of report to generate
     * @param startDate Start date for the report period
     * @param endDate End date for the report period
     * @return The generated report file
     */
    suspend fun generateReport(
        type: ReportType,
        startDate: LocalDate,
        endDate: LocalDate
    ): File {
        // Convert LocalDate to milliseconds for comparison
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        // Create report filename
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val reportName = "${type.name.lowercase()}_${startDate.format(formatter)}_${endDate.format(formatter)}.txt"
        val reportFile = File(context.getExternalFilesDir(null), reportName)
        
        // Generate appropriate report based on type
        when (type) {
            ReportType.USER_ACTIVITY -> generateUserActivityReport(reportFile, startMillis, endMillis)
            ReportType.LOGIN_HISTORY -> generateLoginHistoryReport(reportFile, startMillis, endMillis)
            ReportType.SYSTEM_LOGS -> generateSystemLogsReport(reportFile, startMillis, endMillis)
            ReportType.STORAGE_USAGE -> generateStorageUsageReport(reportFile, startMillis, endMillis)
            ReportType.SYSTEM_PERFORMANCE -> generatePerformanceReport(reportFile, startMillis, endMillis)
            ReportType.SECURITY_AUDIT -> generateSecurityAuditReport(reportFile, startMillis, endMillis)
            ReportType.DATA_USAGE -> generateDataUsageReport(reportFile, startMillis, endMillis)
        }
        
        return reportFile
    }
    
    /**
     * Generate a report about user activity
     */
    private fun generateUserActivityReport(
        reportFile: File,
        startMillis: Long,
        endMillis: Long
    ) {
        val reportBuilder = StringBuilder()
        
        // Report header
        reportBuilder.appendLine("USER ACTIVITY REPORT")
        reportBuilder.appendLine("Generated: ${LocalDate.now()}")
        reportBuilder.appendLine("Period: ${millisToDateString(startMillis)} to ${millisToDateString(endMillis)}")
        reportBuilder.appendLine("------------------------------------------------------")
        reportBuilder.appendLine()
        
        // Write report to file
        reportFile.writeText(reportBuilder.toString())
    }
    
    /**
     * Generate a report about login history
     */
    private fun generateLoginHistoryReport(
        reportFile: File,
        startMillis: Long,
        endMillis: Long
    ) {
        val reportBuilder = StringBuilder()
        
        // Report header
        reportBuilder.appendLine("LOGIN HISTORY REPORT")
        reportBuilder.appendLine("Generated: ${LocalDate.now()}")
        reportBuilder.appendLine("Period: ${millisToDateString(startMillis)} to ${millisToDateString(endMillis)}")
        reportBuilder.appendLine("------------------------------------------------------")
        reportBuilder.appendLine()
        
        // Write report to file
        reportFile.writeText(reportBuilder.toString())
    }
    
    /**
     * Generate a report about system logs
     */
    private fun generateSystemLogsReport(
        reportFile: File,
        startMillis: Long,
        endMillis: Long
    ) {
        val reportBuilder = StringBuilder()
        
        // Report header
        reportBuilder.appendLine("SYSTEM LOGS REPORT")
        reportBuilder.appendLine("Generated: ${LocalDate.now()}")
        reportBuilder.appendLine("Period: ${millisToDateString(startMillis)} to ${millisToDateString(endMillis)}")
        reportBuilder.appendLine("------------------------------------------------------")
        reportBuilder.appendLine()
        
        // Read system log
        val logFile = File(context.filesDir, "system.log")
        if (logFile.exists()) {
            val logLines = logFile.readLines()
            
            // Filter log entries within date range
            val filteredLogs = logLines.filter { line ->
                val timestamp = line.substringBefore(":").toLongOrNull() ?: 0L
                timestamp in startMillis..endMillis
            }
            
            reportBuilder.appendLine("SYSTEM LOGS (${filteredLogs.size} total):")
            reportBuilder.appendLine("Timestamp | Level | Message")
            reportBuilder.appendLine("------------------------------------------------------")
            
            filteredLogs.forEach { line ->
                val parts = line.split(":", limit = 3)
                if (parts.size >= 3) {
                    val timestamp = parts[0].toLongOrNull() ?: 0L
                    val level = parts[1]
                    val message = parts[2]
                    
                    reportBuilder.appendLine("${millisToDateString(timestamp)} | $level | $message")
                }
            }
        } else {
            reportBuilder.appendLine("No system log found.")
        }
        
        // Write report to file
        reportFile.writeText(reportBuilder.toString())
    }
    
    /**
     * Generate a report about storage usage
     */
    private fun generateStorageUsageReport(
        reportFile: File,
        startMillis: Long,
        endMillis: Long
    ) {
        val reportBuilder = StringBuilder()
        
        // Report header
        reportBuilder.appendLine("STORAGE USAGE REPORT")
        reportBuilder.appendLine("Generated: ${LocalDate.now()}")
        reportBuilder.appendLine("Period: ${millisToDateString(startMillis)} to ${millisToDateString(endMillis)}")
        reportBuilder.appendLine("------------------------------------------------------")
        reportBuilder.appendLine()
        
        // Calculate storage usage
        val filesDir = context.filesDir
        val externalFilesDir = context.getExternalFilesDir(null)
        
        val internalSize = calculateDirectorySize(filesDir)
        val externalSize = if (externalFilesDir != null) calculateDirectorySize(externalFilesDir) else 0L
        val totalSize = internalSize + externalSize
        
        // Report summary
        reportBuilder.appendLine("STORAGE USAGE SUMMARY:")
        reportBuilder.appendLine("Internal Storage: ${formatSize(internalSize)}")
        reportBuilder.appendLine("External Storage: ${formatSize(externalSize)}")
        reportBuilder.appendLine("Total Storage: ${formatSize(totalSize)}")
        reportBuilder.appendLine()
        
        // Detailed breakdown
        reportBuilder.appendLine("STORAGE BREAKDOWN:")
        reportBuilder.appendLine("Directory | Size | Files")
        reportBuilder.appendLine("------------------------------------------------------")
        
        // Internal storage breakdown
        if (filesDir.exists() && filesDir.isDirectory) {
            filesDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    val size = calculateDirectorySize(file)
                    val fileCount = countFiles(file)
                    reportBuilder.appendLine("${file.name} (internal) | ${formatSize(size)} | $fileCount files")
                } else {
                    reportBuilder.appendLine("${file.name} (internal) | ${formatSize(file.length())} | 1 file")
                }
            }
        }
        
        // External storage breakdown
        if (externalFilesDir != null && externalFilesDir.exists() && externalFilesDir.isDirectory) {
            externalFilesDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    val size = calculateDirectorySize(file)
                    val fileCount = countFiles(file)
                    reportBuilder.appendLine("${file.name} (external) | ${formatSize(size)} | $fileCount files")
                } else {
                    reportBuilder.appendLine("${file.name} (external) | ${formatSize(file.length())} | 1 file")
                }
            }
        }
        
        // Write report to file
        reportFile.writeText(reportBuilder.toString())
    }
    
    /**
     * Calculate the size of a directory recursively
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        
        return size
    }
    
    /**
     * Count files in a directory recursively
     */
    private fun countFiles(directory: File): Int {
        var count = 0
        
        directory.listFiles()?.forEach { file ->
            count += if (file.isDirectory) {
                countFiles(file)
            } else {
                1
            }
        }
        
        return count
    }
    
    /**
     * Format file size in human-readable format
     */
    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * Convert milliseconds timestamp to formatted date string
     */
    private fun millisToDateString(millis: Long): String {
        val date = LocalDate.ofInstant(
            java.time.Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        )
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
    
    /**
     * Generate a report about performance metrics
     */
    private fun generatePerformanceReport(
        reportFile: File,
        startMillis: Long,
        endMillis: Long
    ) {
        val reportBuilder = StringBuilder()
        
        // Report header
        reportBuilder.appendLine("SYSTEM PERFORMANCE REPORT")
        reportBuilder.appendLine("Generated: ${LocalDate.now()}")
        reportBuilder.appendLine("Period: ${millisToDateString(startMillis)} to ${millisToDateString(endMillis)}")
        reportBuilder.appendLine("------------------------------------------------------")
        reportBuilder.appendLine()
        
        // In a real application, performance metrics would be collected from various sources
        // For now, we'll add placeholder data
        reportBuilder.appendLine("PERFORMANCE METRICS:")
        reportBuilder.appendLine("Average Response Time: 215ms")
        reportBuilder.appendLine("Peak Response Time: 1250ms")
        reportBuilder.appendLine("Average CPU Usage: 42%")
        reportBuilder.appendLine("Peak CPU Usage: 78%")
        reportBuilder.appendLine("Average Memory Usage: 512MB")
        reportBuilder.appendLine("Peak Memory Usage: 1.2GB")
        reportBuilder.appendLine("API Requests Per Minute: 128")
        reportBuilder.appendLine("Database Queries Per Minute: 345")
        reportBuilder.appendLine()
        
        // Write report to file
        reportFile.writeText(reportBuilder.toString())
    }
    
    /**
     * Generate a security audit report
     */
    private fun generateSecurityAuditReport(
        reportFile: File,
        startMillis: Long,
        endMillis: Long
    ) {
        val reportBuilder = StringBuilder()
        
        // Report header
        reportBuilder.appendLine("SECURITY AUDIT REPORT")
        reportBuilder.appendLine("Generated: ${LocalDate.now()}")
        reportBuilder.appendLine("Period: ${millisToDateString(startMillis)} to ${millisToDateString(endMillis)}")
        reportBuilder.appendLine("------------------------------------------------------")
        reportBuilder.appendLine()
        
        // In a real application, security events would be collected from logs
        // For now, we'll add placeholder data
        reportBuilder.appendLine("SECURITY EVENTS:")
        reportBuilder.appendLine("Total Login Attempts: 253")
        reportBuilder.appendLine("Failed Login Attempts: 42")
        reportBuilder.appendLine("Password Reset Requests: 15")
        reportBuilder.appendLine("Data Export Events: 8")
        reportBuilder.appendLine()
        
        // Write report to file
        reportFile.writeText(reportBuilder.toString())
    }
    
    /**
     * Generate a data usage report
     */
    private fun generateDataUsageReport(
        reportFile: File,
        startMillis: Long,
        endMillis: Long
    ) {
        val reportBuilder = StringBuilder()
        
        // Report header
        reportBuilder.appendLine("DATA USAGE REPORT")
        reportBuilder.appendLine("Generated: ${LocalDate.now()}")
        reportBuilder.appendLine("Period: ${millisToDateString(startMillis)} to ${millisToDateString(endMillis)}")
        reportBuilder.appendLine("------------------------------------------------------")
        reportBuilder.appendLine()
        
        // In a real application, data usage metrics would be collected from various sources
        // For now, we'll add placeholder data
        reportBuilder.appendLine("DATA USAGE METRICS:")
        reportBuilder.appendLine("Total Network Traffic: 12.5GB")
        reportBuilder.appendLine("Database Storage Used: 8.2GB")
        reportBuilder.appendLine("Media Storage Used: 15.7GB")
        reportBuilder.appendLine("User Data Storage Used: 3.4GB")
        reportBuilder.appendLine("API Data Transfer: 4.8GB")
        reportBuilder.appendLine("Average Daily Transfer: 420MB")
        reportBuilder.appendLine("Peak Daily Transfer: 1.7GB")
        reportBuilder.appendLine()
        
        // Write report to file
        reportFile.writeText(reportBuilder.toString())
    }
} 