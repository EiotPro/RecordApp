package com.example.recordapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.recordapp.model.Expense
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for generating CSV files from expense records
 */
class CsvGenerator(private val context: Context) {
    companion object {
        private const val TAG = "CsvGenerator"
        
        /**
         * Generate a CSV file containing all expenses
         * @param context Application context
         * @param expenses List of expenses to include in the CSV
         * @return Generated CSV file or null if generation failed
         */
        fun generateCsv(context: Context, expenses: List<Expense>): File? {
            try {
                Log.d(TAG, "Starting CSV generation for all expenses")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val csvFile = File(context.externalCacheDir, "RecordApp_Expenses_$timestamp.csv")
                val outputStream = FileOutputStream(csvFile)
                
                val generator = CsvGenerator(context)
                
                // Define CSV headers
                val headers = listOf(
                    "ID", "Timestamp", "Amount", "Serial Number", 
                    "Description", "Folder", "Image Path"
                )
                
                // Prepare data rows
                val rows = expenses.map { expense ->
                    listOf(
                        expense.id,
                        expense.getFormattedTimestamp(),
                        expense.amount.toString(),
                        expense.serialNumber,
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
                
                val generator = CsvGenerator(context)
                
                // Define CSV headers
                val headers = listOf(
                    "ID", "Timestamp", "Amount", "Serial Number", 
                    "Description", "Image Path"
                )
                
                // Prepare data rows
                val rows = folderExpenses.map { expense ->
                    listOf(
                        expense.id,
                        expense.getFormattedTimestamp(),
                        expense.amount.toString(),
                        expense.serialNumber,
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
     * Generate a CSV file for a folder of expenses with descriptions
     * @param outputStream The output stream to write the CSV data to
     * @param folderName The name of the folder
     * @param items Map of URIs to their descriptions
     * @return true if generation was successful, false otherwise
     */
    fun generateFolderCsv(
        outputStream: OutputStream,
        folderName: String,
        items: Map<Uri, String?>
    ): Boolean {
        val headers = listOf("Index", "Timestamp", "Description", "File URI")
        val rows = ArrayList<List<String>>()
        
        // Add timestamp row at the top
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        rows.add(listOf("", "Generated", timestamp, ""))
        rows.add(listOf("", "Folder", folderName, ""))
        rows.add(listOf("", "Total Items", items.size.toString(), ""))
        rows.add(listOf("")) // Empty row as separator
        
        // Add data rows
        items.entries.forEachIndexed { index, (uri, description) ->
            rows.add(listOf(
                (index + 1).toString(),
                timestamp,
                description ?: "",
                uri.toString()
            ))
        }
        
        return generateCsv(outputStream, headers, rows)
    }
} 