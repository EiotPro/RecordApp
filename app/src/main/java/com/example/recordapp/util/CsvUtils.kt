package com.example.recordapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.recordapp.model.Expense
import com.example.recordapp.model.User
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Unified CSV utility class for generating and exporting CSV files
 * Combines functionality from CsvGenerator and CsvExporter
 */
class CsvUtils(private val context: Context) {
    companion object {
        private const val TAG = "CsvUtils"
        
        /**
         * Generate a CSV file containing all expenses
         * @param context Application context
         * @param expenses List of expenses to include in the CSV
         * @return Generated CSV file or null if generation failed
         */
        fun generateExpensesCsv(context: Context, expenses: List<Expense>): File? {
            try {
                Log.d(TAG, "Starting CSV generation for all expenses")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val csvFile = File(context.externalCacheDir, "RecordApp_Expenses_$timestamp.csv")
                val outputStream = FileOutputStream(csvFile)
                
                val generator = CsvUtils(context)
                
                // Define CSV headers - ensure Serial Number is kept prominent
                val headers = listOf(
                    "ID", "Timestamp", "Serial Number", "Amount", 
                    "Description", "Folder", "Image Path"
                )
                
                // Prepare data rows with direct original serialNumber handling
                val rows = expenses.map { expense ->
                    // Use the exact serialNumber from the expense
                    // This will be either user-entered or a previously generated random value
                    val serialNumber = expense.serialNumber
                    Log.d(TAG, "Processing expense with serial number: $serialNumber")
                    
                    listOf(
                        expense.id,
                        expense.getFormattedTimestamp(),
                        serialNumber,
                        expense.amount.toString(),
                        expense.description,
                        expense.folderName,
                        expense.imagePath?.toString() ?: ""
                    )
                }
                
                // Generate the CSV file
                val success = generator.generateCsv(outputStream, headers, rows)
                outputStream.close()
                
                return if (success) csvFile else null
            } catch (e: Exception) {
                Log.e(TAG, "Error generating CSV for all expenses", e)
                return null
            }
        }
        
        /**
         * Generate a CSV file for expenses in a specific folder
         * @param context Application context
         * @param expenses List of all expenses
         * @param folderName Name of the folder to filter expenses by
         * @return Generated CSV file or null if generation failed
         */
        fun generateCsvByFolder(context: Context, expenses: List<Expense>, folderName: String): File? {
            try {
                Log.d(TAG, "Starting CSV generation for folder: $folderName")
                
                // Filter expenses for the selected folder
                val folderExpenses = expenses.filter { it.folderName == folderName }
                if (folderExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses found in folder: $folderName")
                    return null
                }
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val csvFile = File(context.externalCacheDir, "RecordApp_Folder_${folderName}_$timestamp.csv")
                val outputStream = FileOutputStream(csvFile)
                
                val generator = CsvUtils(context)
                
                // Define CSV headers
                val headers = listOf(
                    "ID", "Timestamp", "Serial Number", "Amount", 
                    "Description", "Image Path"
                )
                
                // Prepare data rows
                val rows = folderExpenses.map { expense ->
                    // Use the exact serialNumber from the expense
                    // This will be either user-entered or a previously generated random value
                    val serialNumber = expense.serialNumber
                    Log.d(TAG, "Processing folder expense with serial number: $serialNumber")
                    
                    listOf(
                        expense.id,
                        expense.getFormattedTimestamp(),
                        serialNumber,
                        expense.amount.toString(),
                        expense.description,
                        expense.imagePath?.toString() ?: ""
                    )
                }
                
                // Generate the CSV file
                val success = generator.generateCsv(outputStream, headers, rows)
                outputStream.close()
                
                return if (success && csvFile.exists() && csvFile.length() > 0) {
                    Log.d(TAG, "CSV generated successfully: ${csvFile.absolutePath}")
                    csvFile
                } else {
                    Log.e(TAG, "CSV generation failed or produced empty file")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating CSV for folder: $folderName", e)
                return null
            }
        }
    }
    
    /**
     * Generate a CSV file for a list of expense data
     * @param outputStream The output stream to write the CSV data to
     * @param headers List of column headers
     * @param data List of rows, where each row is a list of string values
     * @return true if generation was successful, false otherwise
     */
    fun generateCsv(
        outputStream: OutputStream,
        headers: List<String>,
        data: List<List<String>>
    ): Boolean {
        return try {
            Log.d(TAG, "Starting CSV generation with ${data.size} records")
            
            val writer = CSVWriter(
                OutputStreamWriter(outputStream),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END
            )
            
            // Write headers
            writer.writeNext(headers.toTypedArray())
            
            // Write data rows
            data.forEach { row ->
                writer.writeNext(row.toTypedArray())
            }
            
            writer.flush()
            writer.close()
            
            Log.d(TAG, "CSV generation completed successfully")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error generating CSV file", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during CSV generation", e)
            false
        }
    }
    
    /**
     * Generate a folder CSV file for expenses with descriptions and serial numbers
     * @param outputStream The output stream to write the CSV data to
     * @param folderName The name of the folder
     * @param items Map of URIs to their expenses
     * @return true if generation was successful, false otherwise
     */
    fun generateFolderCsv(
        outputStream: OutputStream,
        folderName: String,
        items: Map<Uri, Expense>
    ): Boolean {
        val headers = listOf("Index", "Timestamp", "Serial Number", "Amount", "Description", "File URI")
        val rows = ArrayList<List<String>>()
        
        // Add timestamp row at the top
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        rows.add(listOf("", "Generated", timestamp, "", "", ""))
        rows.add(listOf("", "Folder", folderName, "", "", ""))
        rows.add(listOf("", "Total Items", items.size.toString(), "", "", ""))
        rows.add(listOf("", "", "", "", "", "")) // Empty row as separator
        
        // Add data rows
        items.entries.forEachIndexed { index, (uri, expense) ->
            // Use the exact serialNumber from the expense
            // This will be either user-entered or a previously generated random value
            val serialNumber = expense.serialNumber
            
            rows.add(listOf(
                (index + 1).toString(),
                expense.getFormattedTimestamp(),
                serialNumber,
                expense.amount.toString(),
                expense.description,
                uri.toString()
            ))
        }
        
        return generateCsv(outputStream, headers, rows)
    }

    /**
     * Generate a CSV file for a list of expenses sorted by folder
     * @param outputStream The output stream to write the CSV data to
     * @param expenses List of expenses to include in the CSV
     * @return true if generation was successful, false otherwise
     */
    fun generateGroupedCsv(
        outputStream: OutputStream,
        expenses: List<Expense>
    ): Boolean {
        // Make Serial Number more prominent in the headers
        val headers = listOf("Index", "Folder", "Timestamp", "Serial Number", "Amount", "Description", "Image Path")
        val rows = ArrayList<List<String>>()
        
        // Add timestamp row at the top
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        rows.add(listOf("", "Generated", timestamp, "", "", "", ""))
        rows.add(listOf("", "Total Expenses", expenses.size.toString(), "", "", "", ""))
        rows.add(listOf("", "", "", "", "", "", "")) // Empty row as separator
        
        // Group expenses by folder
        val groupedExpenses = expenses.groupBy { it.folderName }
        
        // Add data rows for each folder
        var runningIndex = 1
        groupedExpenses.forEach { (folderName, folderExpenses) ->
            // Add folder header
            rows.add(listOf("", "FOLDER", folderName, "", "", "", ""))
            
            // Add expenses for this folder
            folderExpenses.forEach { expense ->
                rows.add(listOf(
                    runningIndex.toString(),
                    folderName,
                    expense.getFormattedTimestamp(),
                    expense.serialNumber,
                    expense.amount.toString(),
                    expense.description,
                    expense.imagePath?.toString() ?: ""
                ))
                runningIndex++
            }
            
            // Add empty row as separator between folders
            rows.add(listOf("", "", "", "", "", "", ""))
        }
        
        return generateCsv(outputStream, headers, rows)
    }
    
    /**
     * Export users to CSV file
     * 
     * @param users List of users to export
     * @param filename Name for the export file
     * @return The created CSV file
     */
    fun exportUsers(users: List<User>, filename: String): File {
        val file = File(context.getExternalFilesDir(null), filename)
        
        CSVWriter(FileWriter(file)).use { writer ->
            // Write header
            val header = arrayOf(
                "ID", 
                "Email", 
                "Name", 
                "Created Date",
                "Last Login Date"
            )
            writer.writeNext(header)
            
            // Write data rows
            users.forEach { user ->
                val row = arrayOf(
                    user.id,
                    user.email,
                    user.name,
                    formatTimestamp(user.creationTime),
                    if (user.lastLoginTime > 0) formatTimestamp(user.lastLoginTime) else "Never"
                )
                writer.writeNext(row)
            }
        }
        
        return file
    }
    
    /**
     * Format timestamp as readable date
     */
    private fun formatTimestamp(timestamp: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
} 