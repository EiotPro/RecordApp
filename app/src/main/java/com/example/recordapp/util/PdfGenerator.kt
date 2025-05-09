package com.example.recordapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.element.LineSeparator
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.io.image.ImageDataFactory
import com.example.recordapp.model.Expense
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GridSize(val columns: Int, val rows: Int, val displayName: String) {
    ONE_BY_ONE(1, 1, "Individual (1 per page)"),  // Individual images (one per page)
    TWO_BY_TWO(2, 2, "2×2 Grid (4 per page)"),    // 4 images per page
    TWO_BY_THREE(2, 3, "2×3 Grid (6 per page)")   // 6 images per page
}

class PdfGenerator(private val context: Context) {
    companion object {
        private const val TAG = "PdfGenerator"
        
        /**
         * Generate a PDF file containing all expenses
         */
        fun generatePdf(context: Context, expenses: List<Expense>): File? {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFile = File(context.externalCacheDir, "RecordApp_Expenses_$timestamp.pdf")
                val outputStream = FileOutputStream(pdfFile)
                
                val generator = PdfGenerator(context)
                val title = "RecordApp Expenses Report"
                val description = "Total expenses: ${expenses.size}"
                
                // Add all expenses to PDF
                val additionalInfo = mutableMapOf<String, String>()
                additionalInfo["Total Expenses"] = expenses.size.toString()
                additionalInfo["Total Amount"] = expenses.sumOf { it.amount }.toString()
                
                generator.generatePdf(outputStream, title, description, null, additionalInfo)
                outputStream.close()
                
                return pdfFile
            } catch (e: Exception) {
                Log.e(TAG, "Error generating PDF", e)
                return null
            }
        }
        
        /**
         * Generate a PDF file for a single expense
         */
        fun generateSingleExpensePdf(context: Context, expense: Expense): File? {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFile = File(context.externalCacheDir, "RecordApp_Expense_${expense.id}_$timestamp.pdf")
                val outputStream = FileOutputStream(pdfFile)
                
                val generator = PdfGenerator(context)
                val title = "Expense Record: ${expense.id}"
                val description = expense.description
                
                // Load image if available
                var bitmap: Bitmap? = null
                expense.imagePath?.let { uri ->
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        bitmap = BitmapFactory.decodeStream(inputStream)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading bitmap from URI", e)
                    }
                }
                
                // Add additional expense details
                val additionalInfo = mutableMapOf<String, String>()
                additionalInfo["Amount"] = expense.amount.toString()
                additionalInfo["Date"] = expense.getFormattedTimestamp()
                additionalInfo["Folder"] = expense.folderName
                if (expense.serialNumber.isNotEmpty()) {
                    additionalInfo["Serial Number"] = expense.serialNumber
                }
                
                generator.generatePdf(outputStream, title, description, bitmap, additionalInfo)
                outputStream.close()
                
                return pdfFile
            } catch (e: Exception) {
                Log.e(TAG, "Error generating single expense PDF", e)
                return null
            }
        }
        
        /**
         * Generate a PDF file for expenses in a specific folder
         */
        fun generatePdfByFolder(context: Context, expenses: List<Expense>, folderName: String): File? {
            try {
                Log.d(TAG, "Generating PDF for folder: $folderName")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFile = File(context.externalCacheDir, "RecordApp_Folder_${folderName}_$timestamp.pdf")
                val outputStream = FileOutputStream(pdfFile)
                
                // Filter expenses for the specific folder
                val folderExpenses = expenses.filter { it.folderName == folderName }
                if (folderExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses found in folder: $folderName")
                    return null
                }
                
                // Collect image URIs and descriptions
                val imageUris = folderExpenses.mapNotNull { it.imagePath }
                val descriptions = folderExpenses.mapNotNull { it.imagePath }
                    .associateWith { uri -> 
                        folderExpenses.find { it.imagePath == uri }?.description
                    }
                
                val generator = PdfGenerator(context)
                generator.generateFolderPdf(outputStream, folderName, imageUris, descriptions)
                outputStream.close()
                
                return pdfFile
            } catch (e: Exception) {
                Log.e(TAG, "Error generating folder PDF", e)
                return null
            }
        }
        
        /**
         * Generate a PDF file with images in a grid layout
         */
        fun generateImageGridPdf(context: Context, expenses: List<Expense>, gridSize: GridSize): File? {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFile = File(context.externalCacheDir, "RecordApp_ImageGrid_$timestamp.pdf")
                val outputStream = FileOutputStream(pdfFile)
                
                // Collect image URIs from expenses
                val imageUris = expenses.mapNotNull { it.imagePath }
                if (imageUris.isEmpty()) {
                    Log.e(TAG, "No images found in expenses")
                    return null
                }
                
                val generator = PdfGenerator(context)
                generator.generateImageGridPdf(outputStream, "All Images", imageUris, gridSize)
                outputStream.close()
                
                return pdfFile
            } catch (e: Exception) {
                Log.e(TAG, "Error generating image grid PDF", e)
                return null
            }
        }
    }

    // Generate a PDF file with text and image
    fun generatePdf(
        outputStream: OutputStream,
        title: String,
        description: String?,
        imageBitmap: Bitmap?,
        additionalText: Map<String, String>? = null
    ): Boolean {
        return try {
            val document = createDocument(outputStream)
            
            // Add title
            document.add(
                Paragraph(title)
                    .setFontSize(18f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
            )
            
            // Add timestamp
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            document.add(
                Paragraph("Generated: ${dateFormat.format(Date())}")
                    .setFontSize(10f)
                    .setItalic()
                    .setTextAlignment(TextAlignment.RIGHT)
            )

            // Add separator
            document.add(
                LineSeparator(SolidLine(1f))
                    .setMarginTop(10f)
                    .setMarginBottom(10f)
            )
            
            // Add description if present
            if (!description.isNullOrBlank()) {
                document.add(
                    Paragraph("Description: $description")
                        .setFontSize(12f)
                        .setMarginBottom(10f)
                )
            }
            
            // Add image if present
            if (imageBitmap != null) {
                val imgData = convertBitmapToImageData(imageBitmap)
                val image = Image(imgData)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setWidth(UnitValue.createPercentValue(80f))
                
                document.add(image)
            }
            
            // Add additional text data if present
            if (!additionalText.isNullOrEmpty()) {
                document.add(
                    Paragraph("Additional Information:")
                        .setFontSize(14f)
                        .setBold()
                        .setMarginTop(15f)
                        .setMarginBottom(5f)
                )
                
                val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                
                additionalText.forEach { (key, value) ->
                    table.addCell(
                        Cell().add(
                            Paragraph(key)
                                .setFontSize(11f)
                                .setBold()
                        )
                    )
                    table.addCell(
                        Cell().add(
                            Paragraph(value)
                                .setFontSize(11f)
                        )
                    )
                }
                
                document.add(table)
            }
            
            document.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF", e)
            false
        }
    }
    
    // Generate a PDF with multiple images in a folder
    fun generateFolderPdf(
        outputStream: OutputStream,
        folderName: String,
        imageUris: List<Uri>,
        descriptions: Map<Uri, String?>
    ): Boolean {
        return try {
            val document = createDocument(outputStream)
            
            // Add title
            document.add(
                Paragraph("Folder: $folderName")
                    .setFontSize(18f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
            )
            
            // Add timestamp
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            document.add(
                Paragraph("Generated: ${dateFormat.format(Date())}")
                    .setFontSize(10f)
                    .setItalic()
                    .setTextAlignment(TextAlignment.RIGHT)
            )
            
            // Add separator
            document.add(
                LineSeparator(SolidLine(1f))
                    .setMarginTop(10f)
                    .setMarginBottom(10f)
            )
            
            // Add number of records
            document.add(
                Paragraph("Total Records: ${imageUris.size}")
                    .setFontSize(12f)
                    .setMarginBottom(15f)
            )
            
            // Add each image and its description
            imageUris.forEach { uri ->
                try {
                    // Add separator between records
                    document.add(
                        LineSeparator(SolidLine(0.5f))
                            .setMarginTop(20f)
                            .setMarginBottom(10f)
                    )
                    
                    // Load and add the image
                    val bitmap = loadBitmapFromUri(uri)
                    if (bitmap != null) {
                        val imgData = convertBitmapToImageData(bitmap)
                        val image = Image(imgData)
                            .setHorizontalAlignment(HorizontalAlignment.CENTER)
                            .setWidth(UnitValue.createPercentValue(80f))
                        
                        document.add(image)
                    }
                    
                    // Add description if available
                    val description = descriptions[uri]
                    if (!description.isNullOrBlank()) {
                        document.add(
                            Paragraph("Description: $description")
                                .setFontSize(12f)
                                .setMarginTop(5f)
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding image to PDF", e)
                }
            }
            
            document.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error generating folder PDF", e)
            false
        }
    }
    
    // Generate a grid-based PDF with multiple images
    fun generateImageGridPdf(
        outputStream: OutputStream,
        folderName: String,
        imageUris: List<Uri>,
        gridSize: GridSize
    ): Boolean {
        return try {
            val document = createDocument(outputStream)
            
            // Add title
            document.add(
                Paragraph("Image Grid: $folderName")
                    .setFontSize(18f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
            )
            
            // Add timestamp
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            document.add(
                Paragraph("Generated: ${dateFormat.format(Date())}")
                    .setFontSize(10f)
                    .setItalic()
                    .setTextAlignment(TextAlignment.RIGHT)
            )
            
            // Add separator
            document.add(
                LineSeparator(SolidLine(1f))
                    .setMarginTop(10f)
                    .setMarginBottom(15f)
            )
            
            // Calculate cells per page and create table
            val cellsPerPage = gridSize.columns * gridSize.rows
            var currentIndex = 0
            
            while (currentIndex < imageUris.size) {
                // Create a new table for each page
                val table = Table(
                    UnitValue.createPercentArray(
                        FloatArray(gridSize.columns) { 100f / gridSize.columns }
                    )
                )
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setMarginBottom(10f)
                
                // Add images to the table grid
                for (row in 0 until gridSize.rows) {
                    for (col in 0 until gridSize.columns) {
                        if (currentIndex < imageUris.size) {
                            val uri = imageUris[currentIndex]
                            try {
                                val bitmap = loadBitmapFromUri(uri)
                                if (bitmap != null) {
                                    val imgData = convertBitmapToImageData(bitmap)
                                    val image = Image(imgData)
                                        .setHorizontalAlignment(HorizontalAlignment.CENTER)
                                        .setWidth(UnitValue.createPercentValue(90f))
                                    
                                    table.addCell(
                                        Cell()
                                            .add(image)
                                            .setHorizontalAlignment(HorizontalAlignment.CENTER)
                                            .setPadding(5f)
                                    )
                                } else {
                                    table.addCell(
                                        Cell()
                                            .add(Paragraph("Image not available"))
                                            .setFontColor(ColorConstants.RED)
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error adding image to grid", e)
                                table.addCell(
                                    Cell()
                                        .add(Paragraph("Error loading image"))
                                        .setFontColor(ColorConstants.RED)
                                )
                            }
                            
                            currentIndex++
                        } else {
                            // Empty cell for padding
                            table.addCell(Cell().add(Paragraph("")))
                        }
                    }
                }
                
                document.add(table)
                
                // Add a page break if not the last page
                if (currentIndex < imageUris.size) {
                    document.add(AreaBreak())
                }
            }
            
            document.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error generating grid PDF", e)
            false
        }
    }
    
    // Private helper methods
    private fun createDocument(outputStream: OutputStream): Document {
        val pdfWriter = PdfWriter(outputStream)
        val pdfDocument = PdfDocument(pdfWriter)
        pdfDocument.defaultPageSize = PageSize.A4
        return Document(pdfDocument)
    }
    
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI", e)
            null
        }
    }
    
    private fun convertBitmapToImageData(bitmap: Bitmap): com.itextpdf.io.image.ImageData {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        return ImageDataFactory.create(byteArrayOutputStream.toByteArray())
    }
}