package com.example.recordapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.recordapp.model.Expense
import com.example.recordapp.model.GridSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Utility class for generating ZIP exports containing PDF, CSV, and Grid exports
 */
object ZipUtils {
    private const val TAG = "ZipUtils"

    /**
     * Generates a ZIP file containing PDF, CSV, and Grid exports for the given expenses
     * 
     * @param context Application context
     * @param expenses List of expenses to export
     * @param folderName Optional folder name for filtering expenses (null means all expenses)
     * @return URI of the generated ZIP file
     */
    suspend fun generateZipExport(
        context: Context,
        expenses: List<Expense>,
        folderName: String? = null
    ): Uri = withContext(Dispatchers.IO) {
        // Create timestamp for filename
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        
        // Determine filename based on whether we're exporting a specific folder or all expenses
        val fileName = if (folderName != null && folderName != "All") {
            "RecordApp_Folder_${folderName}_$timestamp.zip"
        } else {
            "RecordApp_ALL_$timestamp.zip"
        }
        
        // Create temporary directory for exports
        val tempDir = File(context.cacheDir, "zip_exports_temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        } else {
            // Clean up any previous temp files
            tempDir.listFiles()?.forEach { it.delete() }
        }
        
        // Create subdirectories for each export type
        val pdfDir = File(tempDir, "PDFs")
        val csvDir = File(tempDir, "CSVs")
        val gridDir = File(tempDir, "Grid")
        
        pdfDir.mkdirs()
        csvDir.mkdirs()
        gridDir.mkdirs()
        
        // Get default grid size from settings
        val gridSize = GridSize.MAGAZINE_LAYOUT
        
        Log.d(TAG, "Generating PDF export for ZIP")
        // Generate PDF with images - using the same methods as folder export
        val pdfFile = if (folderName == null || folderName == "All") {
            // For "All" folder, use the special grid layout that doesn't filter by folder name
            PdfUtils.generateAllExpensesGridPdf(context, expenses, gridSize)
        } else {
            // For specific folders, use the grid layout
            PdfUtils.generateFolderGridPdf(context, expenses, folderName, gridSize)
        }
        
        Log.d(TAG, "Generating CSV export for ZIP")
        val csvFile = if (folderName == null || folderName == "All") {
            CsvUtils.generateExpensesCsv(context, expenses)
        } else {
            CsvUtils.generateCsvByFolder(context, expenses, folderName)
        }
        
        Log.d(TAG, "Generating Grid PDF export for ZIP")
        // Generate a list-style PDF for the grid folder
        val gridPdfFile = PdfUtils.generateImageGridPdf(context, expenses.filter { it.imagePath != null }, gridSize)
        
        // Create the ZIP file
        val zipFile = File(FileUtils.getOutputDirectory(context), fileName)
        
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
            // Add PDF export
            pdfFile?.let {
                addFileToZip(zipOut, it, "PDFs/${it.name}")
                Log.d(TAG, "Added PDF to ZIP: ${it.name}, size: ${it.length()} bytes")
            }
            
            // Add CSV export
            csvFile?.let {
                addFileToZip(zipOut, it, "CSVs/${it.name}")
                Log.d(TAG, "Added CSV to ZIP: ${it.name}, size: ${it.length()} bytes")
            }
            
            // Add Grid PDF export
            gridPdfFile?.let {
                addFileToZip(zipOut, it, "Grid/${it.name}")
                Log.d(TAG, "Added Grid PDF to ZIP: ${it.name}, size: ${it.length()} bytes")
            }
        }
        
        // Clean up temporary files
        pdfFile?.delete()
        csvFile?.delete()
        gridPdfFile?.delete()
        tempDir.deleteRecursively()
        
        Log.d(TAG, "ZIP file created at: ${zipFile.absolutePath}")
        
        // Return URI for the ZIP file
        FileUtils.getUriForFile(context, zipFile)
    }
    
    /**
     * Helper method to add a file to the ZIP archive
     */
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryPath: String) {
        if (file.exists()) {
            val entry = ZipEntry(entryPath)
            zipOut.putNextEntry(entry)
            file.inputStream().use { input ->
                input.copyTo(zipOut)
            }
            zipOut.closeEntry()
        }
    }
} 