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
import com.example.recordapp.model.GridSize
import com.example.recordapp.model.User
import com.example.recordapp.util.SettingsManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine

/**
 * Unified PDF utility class for generating and exporting PDF documents
 * Combines functionality from PdfGenerator and PdfExporter
 */
class PdfUtils(private val context: Context) {
    companion object {
        private const val TAG = "PdfUtils"
        
        /**
         * Helper method to calculate appropriate sample size for bitmap decoding
         */
        private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
            var sampleSize = 1
            if (width > maxDimension || height > maxDimension) {
                val halfWidth = width / 2
                val halfHeight = height / 2
                
                while ((halfWidth / sampleSize) >= maxDimension || 
                      (halfHeight / sampleSize) >= maxDimension) {
                    sampleSize *= 2
                }
            }
            return sampleSize
        }
        
        /**
         * Helper method to calculate optimal image dimensions for the page
         */
        private fun calculateOptimalImageDimensions(bitmap: Bitmap, pageWidth: Float, pageHeight: Float, 
                                                   maxWidthPercent: Float, maxHeightPercent: Float): Pair<Float, Float> {
            val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val maxWidth = pageWidth * (maxWidthPercent / 100f)
            val maxHeight = pageHeight * (maxHeightPercent / 100f)
            
            var finalWidth = maxWidth
            var finalHeight = finalWidth / imageAspectRatio
            
            if (finalHeight > maxHeight) {
                finalHeight = maxHeight
                finalWidth = finalHeight * imageAspectRatio
            }
            
            return Pair(finalWidth, finalHeight)
        }

        /**
         * Helper method to determine if image should be landscape or portrait
         */
        private fun isImageLandscape(bitmap: Bitmap): Boolean {
            return bitmap.width > bitmap.height
        }
        
        /**
         * Generate a PDF file containing all expenses
         */
        fun generateExpensesPdf(context: Context, expenses: List<Expense>): File? {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFile = File(context.externalCacheDir, "RecordApp_Expenses_$timestamp.pdf")
                val outputStream = FileOutputStream(pdfFile)
                
                val generator = PdfUtils(context)
                val title = "RecordApp Expenses Report"
                val description = "Total expenses: ${expenses.size}"
                
                // Add all expenses to PDF
                val additionalInfo = mutableMapOf<String, String>()
                additionalInfo["Total Expenses"] = expenses.size.toString()
                additionalInfo["Total Amount"] = expenses.sumOf { it.amount }.toString()
                
                // Add expense details table
                val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 25f, 20f, 15f, 30f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                
                // Add table headers
                arrayOf("No.", "Date", "Serial Number", "Amount", "Description").forEach { header ->
                    table.addHeaderCell(
                        Cell().add(
                            Paragraph(header)
                                .setFontSize(11f)
                                .setBold()
                        ).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    )
                }
                
                // Add expense rows
                expenses.forEachIndexed { index, expense ->
                    // Index
                    table.addCell(
                        Cell().add(
                            Paragraph((index + 1).toString())
                                .setFontSize(10f)
                        )
                    )
                    
                    // Date
                    table.addCell(
                        Cell().add(
                            Paragraph(expense.getFormattedTimestamp())
                                .setFontSize(10f)
                        )
                    )
                    
                    // Serial Number
                    table.addCell(
                        Cell().add(
                            Paragraph(expense.serialNumber)
                                .setFontSize(10f)
                        )
                    )
                    
                    // Amount
                    table.addCell(
                        Cell().add(
                            Paragraph(expense.getFormattedAmount())
                                .setFontSize(10f)
                        )
                    )
                    
                    // Description
                    table.addCell(
                        Cell().add(
                            Paragraph(expense.description)
                                .setFontSize(10f)
                        )
                    )
                }
                
                generator.generatePdf(outputStream, title, description, null, additionalInfo, table)
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
                
                val generator = PdfUtils(context)
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
                additionalInfo["Creation Date"] = expense.getFormattedTimestamp()
                additionalInfo["Expense Date & Time"] = expense.getFormattedExpenseDateTime(context)
                additionalInfo["Folder"] = expense.folderName
                // Use the exact serial number without any transformation
                additionalInfo["Serial Number"] = expense.serialNumber
                
                generator.generatePdf(outputStream, title, description, bitmap, additionalInfo)
                outputStream.close()
                
                return pdfFile
            } catch (e: Exception) {
                Log.e(TAG, "Error generating single expense PDF", e)
                return null
            }
        }
        
        /**
         * Generate a PDF with images arranged in a grid layout
         */
        fun generateImageGridPdf(context: Context, expenses: List<Expense>, gridSize: GridSize): File? {
            try {
                Log.d(TAG, "Generating PDF with image grid, grid size: ${gridSize.name}")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFile = File(context.externalCacheDir, "RecordApp_ImageGrid_$timestamp.pdf")
                val outputStream = FileOutputStream(pdfFile)
                
                // Get settings
                val settings = SettingsManager.getInstance(context)
                val compressionQuality = settings.pdfQuality
                
                // Create PDF writer and document
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument, PageSize.A4)
                
                // Get page dimensions for calculations
                val pageWidth = document.pdfDocument.defaultPageSize.width
                val pageHeight = document.pdfDocument.defaultPageSize.height
                
                // Add title
                val title = "RecordApp Image Grid"
                document.add(
                    Paragraph(title)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(16f)
                        .setBold()
                )
                
                // Add generation info
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val generationInfo = "Generated on: ${dateFormatter.format(Date())}"
                document.add(
                    Paragraph(generationInfo)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10f)
                        .setItalic()
                )
                
                document.add(LineSeparator(SolidLine(1f)).setMarginTop(5f).setMarginBottom(10f))
                
                // Filter expenses that have images
                val expensesWithImages = expenses.filter { it.imagePath != null }
                
                // Sort by display order
                val sortedExpenses = expensesWithImages.sortedWith(compareBy { it.displayOrder })
                
                // Define grid columns and rows based on grid size
                val cols = when(gridSize) {
                    GridSize.ONE_BY_ONE -> 1
                    GridSize.TWO_BY_TWO -> 2
                    GridSize.TWO_BY_THREE -> 2
                    GridSize.TWO_BY_FOUR -> 2
                    GridSize.MAGAZINE_LAYOUT -> 3
                }
                
                val rows = when(gridSize) {
                    GridSize.ONE_BY_ONE -> 1
                    GridSize.TWO_BY_TWO -> 2
                    GridSize.TWO_BY_THREE -> 3
                    GridSize.TWO_BY_FOUR -> 4
                    GridSize.MAGAZINE_LAYOUT -> 3
                }
                
                // Calculate cell dimensions with proper padding
                val cellPadding = 10f
                val cellWidth = (pageWidth / cols) - (2 * cellPadding)
                val cellHeight = (pageHeight / (rows + 0.5f)) - (2 * cellPadding) // Account for header
                
                // For single images per page, we'll handle them differently
                if (gridSize == GridSize.ONE_BY_ONE) {
                    processIndividualImages(document, sortedExpenses, context, compressionQuality, pageWidth, pageHeight)
                } else {
                    // Process grid layout
                    processGridLayout(document, sortedExpenses, context, compressionQuality, 
                                      cols, rows, cellWidth, cellHeight, cellPadding, gridSize)
                }
                
                // Close the document
                document.close()
                outputStream.close()
                
                return pdfFile
            } catch (e: Exception) {
                Log.e(TAG, "Error generating image grid PDF", e)
                return null
            }
        }
        
        /**
         * Process individual images for one-per-page layout
         */
        private fun processIndividualImages(
            document: Document, 
            expenses: List<Expense>, 
            context: Context,
            compressionQuality: Int,
            pageWidth: Float,
            pageHeight: Float
        ) {
            // Add each image on its own page with optimal sizing
            expenses.forEachIndexed { index, expense ->
                try {
                    // Add page break after first page
                    if (index > 0) {
                        document.add(AreaBreak())
                    }
                    
                    // Add page header
                    document.add(
                        Paragraph("Image ${index + 1} of ${expenses.size}")
                            .setTextAlignment(TextAlignment.RIGHT)
                            .setFontSize(10f)
                            .setMarginBottom(5f)
                    )
                    
                    expense.imagePath?.let { uri ->
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        
                        if (bitmap != null) {
                            // Determine optimal page orientation based on image aspect ratio
                            val isLandscape = isImageLandscape(bitmap)
                            
                            // Calculate optimal dimensions (90% of page width or height)
                            val (imageWidth, imageHeight) = calculateOptimalImageDimensions(
                                bitmap, pageWidth, pageHeight, 90f, 70f)
                            
                            // Convert bitmap to byte array with proper compression
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, stream)
                            val byteArray = stream.toByteArray()
                            
                            // Create image with proper sizing
                            val image = Image(ImageDataFactory.create(byteArray))
                                .setWidth(imageWidth)
                                .setHeight(imageHeight)
                                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                            
                            // Add image centered on page
                            document.add(image)
                            
                            // Add metadata below image
                            document.add(
                                Paragraph("Index Number: ${index + 1}")
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setFontSize(10f)
                                    .setMarginTop(10f)
                            )
                            document.add(
                                Paragraph("Expense Date and Time: ${expense.getFormattedExpenseDateTime(context)}")
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setFontSize(10f)
                            )
                             
                             // Add amount below date
                             run {
                                 val settingsManager = SettingsManager.getInstance(context)
                                 val amountText = settingsManager.formatAmount(expense.amount)
                                 document.add(
                                     Paragraph("Amount: $amountText")
                                         .setTextAlignment(TextAlignment.CENTER)
                                         .setFontSize(10f)
                                 )
                             }
                            
                            if (expense.description.isNotBlank()) {
                                document.add(
                                    Paragraph("Description: ${expense.description}")
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setFontSize(10f)
                                )
                            }
                            
                            if (expense.serialNumber.isNotBlank()) {
                                document.add(
                                    Paragraph("Serial #: ${expense.serialNumber}")
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setFontSize(10f)
                                )
                            }
                            
                            // Clean up resources
                            bitmap.recycle()
                        }
                        inputStream?.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing individual image: ${e.message}")
                }
            }
        }
        
        /**
         * Process images in grid layout
         */
        private fun processGridLayout(
            document: Document,
            expenses: List<Expense>,
            context: Context,
            compressionQuality: Int,
            cols: Int,
            rows: Int,
            cellWidth: Float,
            cellHeight: Float,
            cellPadding: Float,
            gridSize: GridSize = GridSize.TWO_BY_TWO
        ) {
            // For magazine layout, use specialized function
            if (gridSize == GridSize.MAGAZINE_LAYOUT) {
                processMagazineLayout(document, expenses, context, compressionQuality)
                return
            }
            
            // Create a table with fixed dimensions for better alignment
            var table = Table(UnitValue.createPointArray(FloatArray(cols) { cellWidth }))
                .setFixedLayout()
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginTop(10f)
                .setMarginBottom(10f)
            
            // Add images to the grid
            var currentCell = 0
            var currentRow = 0
            var imagesAdded = 0
            var currentPage = 1
            
            // Add page header
            document.add(
                Paragraph("Page $currentPage")
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFontSize(10f)
                    .setMarginBottom(5f)
            )
            
            for (expense in expenses) {
                try {
                    expense.imagePath?.let { uri ->
                        val inputStream = context.contentResolver.openInputStream(uri)
                        
                        // Use BitmapFactory.Options to sample down large images
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeStream(inputStream, null, options)
                        inputStream?.close()
                        
                        // Calculate sample size based on image dimensions
                        val maxDimension = 1200
                        val sampleSize = calculateSampleSize(
                            options.outWidth,
                            options.outHeight,
                            maxDimension
                        )
                        
                        // Reopen the stream and decode with sample size
                        val newInputStream = context.contentResolver.openInputStream(uri)
                        val decodingOptions = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                        }
                        val bitmap = BitmapFactory.decodeStream(newInputStream, null, decodingOptions)
                        
                        if (bitmap != null) {
                            try {
                                // Calculate image dimensions to fit cell while preserving aspect ratio
                                val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                                val availableCellWidth = cellWidth - (2 * cellPadding)
                                val availableCellHeight = cellHeight - (2 * cellPadding) - 70f // Reserve extra space for captions (date, amount, description, serial)
                                
                                var finalImageWidth = availableCellWidth
                                var finalImageHeight = finalImageWidth / imageAspectRatio
                                
                                if (finalImageHeight > availableCellHeight) {
                                    finalImageHeight = availableCellHeight
                                    finalImageWidth = finalImageHeight * imageAspectRatio
                                }
                                
                                // Convert bitmap to byte array with proper compression
                                val stream = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, stream)
                                val byteArray = stream.toByteArray()
                                
                                // Create image with proper sizing
                                val image = Image(ImageDataFactory.create(byteArray))
                                    .setWidth(finalImageWidth)
                                    .setHeight(finalImageHeight)
                                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                                
                                // Create cell with proper spacing
                                val cell = Cell()
                                    .setPadding(cellPadding)
                                    .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                                
                                // Add image
                                cell.add(image)

                                // Add date and amount captions
                                val settingsManager = SettingsManager.getInstance(context)
                                val dateText = expense.getFormattedExpenseDateTime(context, includeTime = false)
                                val amountText = settingsManager.formatAmount(expense.amount)
                                cell.add(
                                    Paragraph("Expense Date and Time: $dateText")
                                        .setFontSize(7f)
                                        .setTextAlignment(TextAlignment.CENTER)
                                )
                                cell.add(
                                    Paragraph("Amount: $amountText")
                                        .setFontSize(7f)
                                        .setTextAlignment(TextAlignment.CENTER)
                                )
                                
                                // Add metadata caption
                                if (expense.description.isNotBlank()) {
                                    cell.add(
                                        Paragraph(expense.description)
                                            .setFontSize(8f)
                                            .setTextAlignment(TextAlignment.CENTER)
                                    )
                                }
                                
                                // Add serial number if available
                                if (expense.serialNumber.isNotBlank()) {
                                    cell.add(
                                        Paragraph(expense.serialNumber)
                                            .setFontSize(7f)
                                            .setTextAlignment(TextAlignment.CENTER)
                                            .setItalic()
                                    )
                                }
                                
                                // Add cell to table
                                table.addCell(cell)
                                imagesAdded++
                                currentCell++
                                
                                // Start a new row if needed
                                if (currentCell >= cols) {
                                    currentCell = 0
                                    currentRow++
                                    
                                    // Add a page break after completing a grid
                                    if (currentRow >= rows && imagesAdded < expenses.size) {
                                        document.add(table)
                                        document.add(AreaBreak())
                                        
                                        // Create a new table for the next page
                                        table = Table(UnitValue.createPointArray(FloatArray(cols) { cellWidth }))
                                            .setFixedLayout()
                                            .setHorizontalAlignment(HorizontalAlignment.CENTER)
                                            .setMarginTop(10f)
                                            .setMarginBottom(10f)
                                        
                                        currentRow = 0
                                        currentPage++
                                        
                                        // Add page header
                                        document.add(
                                            Paragraph("Page $currentPage")
                                                .setTextAlignment(TextAlignment.RIGHT)
                                                .setFontSize(10f)
                                                .setMarginBottom(5f)
                                        )
                                    }
                                }
                            } finally {
                                // Clean up resources
                                bitmap.recycle()
                            }
                        }
                        newInputStream?.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing image for grid: ${e.message}")
                    // Continue with next image
                }
            }
            
            // Add empty cells to complete the grid if needed
            while (currentCell > 0 && currentCell < cols) {
                table.addCell(Cell().setPadding(cellPadding))
                currentCell++
            }
            
            // Add the final table to the document
            document.add(table)
            
            // Add footer
            document.add(LineSeparator(SolidLine(0.5f)).setMarginTop(10f).setMarginBottom(5f))
            document.add(
                Paragraph("RecordApp - Generated PDF Report")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(8f)
                    .setItalic()
            )
        }

        /**
         * Process images in magazine-style layout with varying cell sizes
         */
        private fun processMagazineLayout(
            document: Document,
            expenses: List<Expense>,
            context: Context,
            compressionQuality: Int
        ) {
            if (expenses.isEmpty()) return
            
            val pageWidth = document.pdfDocument.defaultPageSize.width
            val pageHeight = document.pdfDocument.defaultPageSize.height
            val usableHeight = pageHeight - 100f // Reserve space for headers/footers
            
            // Create pages with magazine-style layouts
            var currentPage = 1
            var imagesProcessed = 0
            
            while (imagesProcessed < expenses.size) {
                if (currentPage > 1) {
                    document.add(AreaBreak())
                }
                
                // Add page header
                document.add(
                    Paragraph("Page $currentPage - Magazine Layout")
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontSize(10f)
                        .setMarginBottom(5f)
                )
                
                // For each page, apply one of the magazine layouts
                val layoutType = (currentPage - 1) % 2 // Alternate between layout styles
                
                when (layoutType) {
                    0 -> imagesProcessed = createMagazineLayoutStyle1(document, expenses, context, compressionQuality, imagesProcessed, pageWidth, usableHeight)
                    1 -> imagesProcessed = createMagazineLayoutStyle2(document, expenses, context, compressionQuality, imagesProcessed, pageWidth, usableHeight)
                }
                
                currentPage++
            }
        }
        
        /**
         * Create magazine layout style 1 (based on the first reference image)
         * Features a 2x2 grid with equal-sized cells
         */
        private fun createMagazineLayoutStyle1(
            document: Document,
            expenses: List<Expense>,
            context: Context,
            compressionQuality: Int,
            startIndex: Int,
            pageWidth: Float,
            pageHeight: Float
        ): Int {
            val cellPadding = 15f
            val cols = 2
            val rows = 2
            
            // Calculate cell dimensions
            val cellWidth = (pageWidth / cols) - (2 * cellPadding)
            val cellHeight = (pageHeight / rows) - (2 * cellPadding)
            
            // Create outer table for the layout
            val table = Table(UnitValue.createPointArray(FloatArray(cols) { cellWidth }))
                .setFixedLayout()
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setBorder(null)
                .setMarginTop(10f)
                .setMarginBottom(10f)
            
            var imagesAdded = 0
            var nextIndex = startIndex
            
            // Add cells to the table
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    if (nextIndex < expenses.size) {
                        val cell = createImageCell(expenses[nextIndex], nextIndex + 1, context, compressionQuality, cellWidth, cellHeight, cellPadding)
                        table.addCell(cell)
                        nextIndex++
                        imagesAdded++
                    } else {
                        // Add empty cell to maintain layout
                        table.addCell(Cell().setPadding(cellPadding))
                    }
                }
            }
            
            document.add(table)
            return startIndex + imagesAdded
        }
        
        /**
         * Create magazine layout style 2 (based on the third reference image)
         * Features varying cell sizes in a magazine-style layout
         */
        private fun createMagazineLayoutStyle2(
            document: Document,
            expenses: List<Expense>,
            context: Context,
            compressionQuality: Int,
            startIndex: Int,
            pageWidth: Float,
            pageHeight: Float
        ): Int {
            val cellPadding = 15f
            var nextIndex = startIndex
            var imagesAdded = 0
            
            // Calculate dimensions for the complex layout
            val firstRowHeight = pageHeight * 0.5f
            val secondRowHeight = pageHeight * 0.5f
            
            // First row: large image on left (2/3 width) and two smaller images stacked on right (1/3 width)
            val firstRowTable = Table(UnitValue.createPointArray(floatArrayOf(pageWidth * 0.65f, pageWidth * 0.35f)))
                .setFixedLayout()
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setBorder(null)
            
            // Left large cell
            if (nextIndex < expenses.size) {
                val largeCell = createImageCell(expenses[nextIndex], nextIndex + 1, context, compressionQuality, 
                                                pageWidth * 0.65f - (2 * cellPadding), 
                                                firstRowHeight - (2 * cellPadding), cellPadding)
                firstRowTable.addCell(largeCell)
                nextIndex++
                imagesAdded++
            } else {
                firstRowTable.addCell(Cell().setPadding(cellPadding))
            }
            
            // Right column with two cells stacked vertically
            val rightColumnCell = Cell().setBorder(null)
            
            // Create nested table for the right column
            val rightColumnTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                .setFixedLayout()
                .setBorder(null)
            
            // Top right cell
            if (nextIndex < expenses.size) {
                val topRightCell = createImageCell(expenses[nextIndex], nextIndex + 1, context, compressionQuality, 
                                                  pageWidth * 0.35f - (2 * cellPadding), 
                                                  firstRowHeight/2 - (2 * cellPadding), cellPadding)
                rightColumnTable.addCell(topRightCell)
                nextIndex++
                imagesAdded++
            } else {
                rightColumnTable.addCell(Cell().setPadding(cellPadding))
            }
            
            // Bottom right cell
            if (nextIndex < expenses.size) {
                val bottomRightCell = createImageCell(expenses[nextIndex], nextIndex + 1, context, compressionQuality, 
                                                     pageWidth * 0.35f - (2 * cellPadding), 
                                                     firstRowHeight/2 - (2 * cellPadding), cellPadding)
                rightColumnTable.addCell(bottomRightCell)
                nextIndex++
                imagesAdded++
            } else {
                rightColumnTable.addCell(Cell().setPadding(cellPadding))
            }
            
            rightColumnCell.add(rightColumnTable)
            firstRowTable.addCell(rightColumnCell)
            document.add(firstRowTable)
            
            // Second row: three equal cells in a row (or two equal and two small)
            val secondRowTable = Table(UnitValue.createPointArray(floatArrayOf(
                pageWidth * 0.33f, pageWidth * 0.33f, pageWidth * 0.34f
            )))
                .setFixedLayout()
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setBorder(null)
                .setMarginTop(cellPadding)
            
            // Add the three remaining cells
            for (i in 0 until 3) {
                if (nextIndex < expenses.size) {
                    val cell = createImageCell(expenses[nextIndex], nextIndex + 1, context, compressionQuality, 
                                              (pageWidth / 3) - (2 * cellPadding), 
                                              secondRowHeight - (3 * cellPadding), cellPadding)
                    secondRowTable.addCell(cell)
                    nextIndex++
                    imagesAdded++
                } else {
                    secondRowTable.addCell(Cell().setPadding(cellPadding))
                }
            }
            
            document.add(secondRowTable)
            return startIndex + imagesAdded
        }
        
        /**
         * Helper method to create an image cell with proper scaling
         */
        private fun createImageCell(
            expense: Expense,
            indexNumber: Int,
            context: Context,
            compressionQuality: Int,
            cellWidth: Float,
            cellHeight: Float,
            cellPadding: Float
        ): Cell {
            val cell = Cell()
                .setPadding(cellPadding)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
            
            try {
                expense.imagePath?.let { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    
                    // Use BitmapFactory.Options to sample down large images
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream?.close()
                    
                    // Calculate sample size based on image dimensions
                    val maxDimension = 1200
                    val sampleSize = calculateSampleSize(
                        options.outWidth,
                        options.outHeight,
                        maxDimension
                    )
                    
                    // Reopen the stream and decode with sample size
                    val newInputStream = context.contentResolver.openInputStream(uri)
                    val decodingOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }
                    val bitmap = BitmapFactory.decodeStream(newInputStream, null, decodingOptions)
                    
                    if (bitmap != null) {
                        try {
                            // Calculate image dimensions to fit cell while preserving aspect ratio
                            val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                            val availableCellWidth = cellWidth - (2 * cellPadding)
                            val availableCellHeight = cellHeight - (2 * cellPadding) - 60f // Reserve extra space for captions (date, amount, description, serial)
                            
                            var finalImageWidth = availableCellWidth
                            var finalImageHeight = finalImageWidth / imageAspectRatio
                            
                            if (finalImageHeight > availableCellHeight) {
                                finalImageHeight = availableCellHeight
                                finalImageWidth = finalImageHeight * imageAspectRatio
                            }
                            
                            // Convert bitmap to byte array with proper compression
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, stream)
                            val byteArray = stream.toByteArray()
                            
                            // Create image with proper sizing
                            val image = Image(ImageDataFactory.create(byteArray))
                                .setWidth(finalImageWidth)
                                .setHeight(finalImageHeight)
                                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                            
                            // Add image
                            cell.add(image)

                            // Add date and amount captions under image
                            val settingsManager = SettingsManager.getInstance(context)
                            val dateText = expense.getFormattedExpenseDateTime(context, includeTime = false)
                            val amountText = settingsManager.formatAmount(expense.amount)
                            cell.add(
                                Paragraph("Expense Date and Time: $dateText")
                                    .setFontSize(7f)
                                    .setTextAlignment(TextAlignment.CENTER)
                            )
                            cell.add(
                                Paragraph("Amount: $amountText")
                                    .setFontSize(7f)
                                    .setTextAlignment(TextAlignment.CENTER)
                            )
                            
                            // Add metadata caption
                            if (expense.description.isNotBlank()) {
                                cell.add(
                                    Paragraph(expense.description)
                                        .setFontSize(8f)
                                        .setTextAlignment(TextAlignment.CENTER)
                                )
                            }
                            
                            // Add serial number if available
                            if (expense.serialNumber.isNotBlank()) {
                                cell.add(
                                    Paragraph(expense.serialNumber)
                                        .setFontSize(7f)
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setItalic()
                                )
                            }

                            // Add index number below image
                            cell.add(
                                Paragraph("Index Number: $indexNumber")
                                    .setFontSize(7f)
                                    .setTextAlignment(TextAlignment.CENTER)
                            )
                        } finally {
                            // Clean up resources
                            bitmap.recycle()
                        }
                    }
                    newInputStream?.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image for cell: ${e.message}")
                cell.add(Paragraph("Image error").setFontSize(8f))
            }
            
            return cell
        }

        /**
         * Generate a PDF file for expenses in a specific folder with grid layout
         */
        fun generateFolderGridPdf(context: Context, expenses: List<Expense>, folderName: String, gridSize: GridSize): File? {
            try {
                Log.d(TAG, "Generating Grid PDF for folder: $folderName with grid size: ${gridSize.name}")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFile = File(context.externalCacheDir, "RecordApp_Folder_${folderName}_$timestamp.pdf")
                val outputStream = FileOutputStream(pdfFile)
                
                // Get settings
                val settings = SettingsManager.getInstance(context)
                val compressionQuality = settings.pdfQuality
                
                // Create PDF writer and document
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument, PageSize.A4)
                
                // Get page dimensions for calculations
                val pageWidth = document.pdfDocument.defaultPageSize.width
                val pageHeight = document.pdfDocument.defaultPageSize.height
                
                // Helper function to add a row to the summary table
                fun addSummaryRow(table: Table, label: String, value: String) {
                    table.addCell(
                        Cell().add(
                            Paragraph(label)
                                .setFontSize(11f)
                                .setBold()
                        ).setBorder(null)
                    )
                    
                    table.addCell(
                        Cell().add(
                            Paragraph(value)
                                .setFontSize(11f)
                        ).setBorder(null)
                    )
                }
                
                // Filter expenses for the specific folder
                val folderExpenses = expenses.filter { it.folderName == folderName }
                if (folderExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses found in folder: $folderName")
                    return null
                }
                
                // Filter expenses that have images
                val expensesWithImages = folderExpenses.filter { it.imagePath != null }
                
                // Sort by display order
                val sortedExpenses = expensesWithImages.sortedWith(compareBy { it.displayOrder })
                
                // Add title with better styling
                document.add(
                    Paragraph("Expense Report: $folderName")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(18f)
                        .setBold()
                )
                
                // Add generation info
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val generationInfo = "Generated on: ${dateFormatter.format(Date())}"
                document.add(
                    Paragraph(generationInfo)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontSize(10f)
                        .setItalic()
                )
                
                document.add(LineSeparator(SolidLine(1f)).setMarginTop(5f).setMarginBottom(10f))
                
                // Add summary section
                val totalCount = expensesWithImages.size
                val totalAmount = folderExpenses.sumOf { it.amount }
                val currencySymbol = settings.currencySymbol
                
                // Create a summary table
                val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setBorder(null)
                
                // Add summary data
                addSummaryRow(summaryTable, "Folder", folderName)
                addSummaryRow(summaryTable, "Total Expenses", totalCount.toString())
                addSummaryRow(summaryTable, "Total Amount", "$currencySymbol ${totalAmount}")
                
                document.add(summaryTable)
                document.add(Paragraph("\n"))
                document.add(LineSeparator(SolidLine(0.5f)).setMarginTop(5f).setMarginBottom(10f))
                
                // Add a detailed list table for this folder
                val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(8f, 20f, 15f, 15f, 42f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                
                // Add table headers
                arrayOf("No.", "Date", "Serial No.", "Amount", "Description").forEach { header ->
                    detailsTable.addHeaderCell(
                        Cell().add(
                            Paragraph(header)
                                .setFontSize(10f)
                                .setBold()
                        ).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    )
                }
                
                // Add expense rows
                sortedExpenses.forEachIndexed { index, expense ->
                    // Index
                    detailsTable.addCell(
                        Cell().add(
                            Paragraph((index + 1).toString())
                                .setFontSize(9f)
                        )
                    )
                    
                    // Date
                    detailsTable.addCell(
                        Cell().add(
                            Paragraph(expense.getFormattedTimestamp())
                                .setFontSize(9f)
                        )
                    )
                    
                    // Serial Number
                    detailsTable.addCell(
                        Cell().add(
                            Paragraph(expense.serialNumber)
                                .setFontSize(9f)
                        )
                    )
                    
                    // Amount
                    detailsTable.addCell(
                        Cell().add(
                            Paragraph("$currencySymbol ${expense.amount}")
                                .setFontSize(9f)
                        )
                    )
                    
                    // Description
                    detailsTable.addCell(
                        Cell().add(
                            Paragraph(expense.description)
                                .setFontSize(9f)
                        )
                    )
                }
                
                document.add(detailsTable)
                document.add(Paragraph("\n"))
                document.add(LineSeparator(DottedLine(1f)).setMarginTop(5f).setMarginBottom(10f))
                document.add(Paragraph("Expense Images").setFontSize(14f).setBold().setTextAlignment(TextAlignment.CENTER))
                document.add(Paragraph("\n"))
                
                // Define grid columns and rows based on grid size
                val cols = when(gridSize) {
                    GridSize.ONE_BY_ONE -> 1
                    GridSize.TWO_BY_TWO -> 2
                    GridSize.TWO_BY_THREE -> 2
                    GridSize.TWO_BY_FOUR -> 2
                    GridSize.MAGAZINE_LAYOUT -> 3
                }
                
                val rows = when(gridSize) {
                    GridSize.ONE_BY_ONE -> 1
                    GridSize.TWO_BY_TWO -> 2
                    GridSize.TWO_BY_THREE -> 3
                    GridSize.TWO_BY_FOUR -> 4
                    GridSize.MAGAZINE_LAYOUT -> 3
                }
                
                // Calculate cell dimensions with proper padding
                val cellPadding = 10f
                val cellWidth = (pageWidth / cols) - (2 * cellPadding)
                val cellHeight = (pageHeight / (rows + 1)) - (2 * cellPadding) // Account for header and summary
                
                // For single images per page, we'll handle them differently
                if (gridSize == GridSize.ONE_BY_ONE) {
                    processIndividualImages(document, sortedExpenses, context, compressionQuality, pageWidth, pageHeight)
                } else {
                    // Process grid layout
                    processGridLayout(document, sortedExpenses, context, compressionQuality, 
                                     cols, rows, cellWidth, cellHeight, cellPadding, gridSize)
                }
                
                // Add footer
                document.add(LineSeparator(SolidLine(1f)).setMarginTop(10f).setMarginBottom(5f))
                document.add(
                    Paragraph("RecordApp - Generated PDF Report")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8f)
                        .setItalic()
                )
                
                // Close document
                document.close()
                outputStream.close()
                
                return pdfFile
            } catch (e: Exception) {
                Log.e(TAG, "Error generating folder grid PDF", e)
                return null
            }
        }

        /**
         * Generate a PDF file for expenses in a specific folder with list layout
         */
        fun generateFolderListPdf(context: Context, expenses: List<Expense>, folderName: String): File? {
            try {
                Log.d(TAG, "Generating List PDF for folder: $folderName")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFile = File(context.externalCacheDir, "RecordApp_List_${folderName}_$timestamp.pdf")
                val outputStream = FileOutputStream(pdfFile)
                
                // Filter expenses for the specific folder
                val folderExpenses = expenses.filter { it.folderName == folderName }
                if (folderExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses found in folder: $folderName")
                    return null
                }
                
                // Filter expenses with images and sort by display order
                val sortedExpenses = folderExpenses
                    .filter { it.imagePath != null }
                    .sortedWith(compareBy { it.displayOrder })
                
                if (sortedExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses with images found in folder: $folderName")
                    return null
                }
                
                // Create PDF document
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument, PageSize.A4)
                
                // Get settings
                val settings = SettingsManager.getInstance(context)
                val currencySymbol = settings.currencySymbol
                val compressionQuality = settings.pdfQuality
                
                // Add title with better styling
                document.add(
                    Paragraph("Detailed Expense List: $folderName")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(18f)
                        .setBold()
                )
                
                // Add generation info
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val generationInfo = "Generated on: ${dateFormatter.format(Date())}"
                document.add(
                    Paragraph(generationInfo)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontSize(10f)
                        .setItalic()
                )
                
                document.add(LineSeparator(SolidLine(1f)).setMarginTop(5f).setMarginBottom(10f))
                
                // Add summary section
                val totalCount = sortedExpenses.size
                val totalAmount = folderExpenses.sumOf { it.amount }
                
                // Create a summary table
                val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setBorder(null)
                
                // Helper function to add a row to the summary table
                fun addSummaryRow(table: Table, label: String, value: String) {
                    table.addCell(
                        Cell().add(
                            Paragraph(label)
                                .setFontSize(11f)
                                .setBold()
                        ).setBorder(null)
                    )
                    
                    table.addCell(
                        Cell().add(
                            Paragraph(value)
                                .setFontSize(11f)
                        ).setBorder(null)
                    )
                }
                
                // Add summary data
                addSummaryRow(summaryTable, "Folder", folderName)
                addSummaryRow(summaryTable, "Total Expenses", totalCount.toString())
                addSummaryRow(summaryTable, "Total Amount", "$currencySymbol ${totalAmount}")
                
                document.add(summaryTable)
                document.add(LineSeparator(SolidLine(0.5f)).setMarginTop(10f).setMarginBottom(10f))
                
                // Add expenses in detailed list format
                sortedExpenses.forEachIndexed { index, expense ->
                    try {
                        // Add item header with index
                        document.add(
                            Paragraph("Item #${index + 1}")
                                .setFontSize(14f)
                                .setBold()
                                .setTextAlignment(TextAlignment.LEFT)
                        )
                        
                        // Create a two-column layout for each expense
                        val expenseTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                            .setWidth(UnitValue.createPercentValue(100f))
                        
                        // Add image to left column if available
                        expense.imagePath?.let { uri ->
                            try {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                
                                // Use BitmapFactory.Options to sample down large images
                                val options = BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }
                                BitmapFactory.decodeStream(inputStream, null, options)
                                inputStream?.close()
                                
                                // Calculate sample size based on image dimensions
                                val maxDimension = 1200 // Reduced for better performance
                                val sampleSize = calculateSampleSize(
                                    options.outWidth,
                                    options.outHeight,
                                    maxDimension
                                )
                                
                                // Reopen the stream and decode with sample size
                                val newInputStream = context.contentResolver.openInputStream(uri)
                                val decodingOptions = BitmapFactory.Options().apply {
                                    inSampleSize = sampleSize
                                }
                                val bitmap = BitmapFactory.decodeStream(newInputStream, null, decodingOptions)
                                
                                if (bitmap != null) {
                                    try {
                                        // Convert bitmap to byte array for PDF with compression
                                        val stream = ByteArrayOutputStream()
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, stream)
                                        val byteArray = stream.toByteArray()
                                        
                                        val image = Image(ImageDataFactory.create(byteArray))
                                            .setWidth(UnitValue.createPercentValue(90f))
                                            .setHorizontalAlignment(HorizontalAlignment.CENTER)
                                        
                                        // Add image cell spanning 1 row
                                        expenseTable.addCell(
                                            Cell(3, 1).add(image)
                                                .setPadding(5f)
                                        )
                                    } finally {
                                        // Clean up resources
                                        bitmap.recycle()
                                    }
                                }
                                
                                // Clean up resources
                                newInputStream?.close()
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing image for list: ${e.message}")
                                // Add empty cell if there's an error
                                expenseTable.addCell(
                                    Cell(3, 1).add(Paragraph("Image Error"))
                                        .setPadding(5f)
                                )
                            }
                        }
                        
                        // Details table for the right side
                        val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                            .setWidth(UnitValue.createPercentValue(100f))
                        
                        // Helper function to add a detail row to a table
                        fun addDetailRow(table: Table, label: String, value: String) {
                            table.addCell(
                                Cell().add(
                                    Paragraph(label)
                                        .setFontSize(10f)
                                        .setBold()
                                ).setBorder(null)
                            )
                            
                            table.addCell(
                                Cell().add(
                                    Paragraph(value)
                                        .setFontSize(10f)
                                ).setBorder(null)
                            )
                        }
                        
                        // Add expense details
                        addDetailRow(detailsTable, "Serial Number:", expense.serialNumber)
                        addDetailRow(detailsTable, "Amount:", "$currencySymbol ${expense.amount}")
                        addDetailRow(detailsTable, "Date:", expense.getFormattedTimestamp())
                        
                        if (expense.description.isNotBlank()) {
                            addDetailRow(detailsTable, "Description:", expense.description)
                        }
                        
                        if (expense.receiptType.isNotBlank()) {
                            addDetailRow(detailsTable, "Receipt Type:", expense.getReceiptTypeDisplayName())
                        }
                        
                        // Add details cell
                        expenseTable.addCell(
                            Cell().add(detailsTable)
                                .setPadding(5f)
                        )
                        
                        // Add the expense table to the document
                        document.add(expenseTable)
                        
                        // Add separator after each item except the last one
                        if (index < sortedExpenses.size - 1) {
                            document.add(LineSeparator(SolidLine(0.5f))
                                .setMarginTop(10f)
                                .setMarginBottom(10f)
                            )
                        }
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing expense for list PDF: ${e.message}")
                    }
                }
                
                // Add page numbering footer to last page
                document.add(
                    Paragraph("Page ${pdfDocument.numberOfPages} of ${pdfDocument.numberOfPages}")
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontSize(9f)
                        .setItalic()
                )
                
                // Add footer
                document.add(LineSeparator(SolidLine(1f)).setMarginTop(10f).setMarginBottom(5f))
                document.add(
                    Paragraph("RecordApp - Detailed List Report")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8f)
                        .setItalic()
                )
                
                // Close document
                document.close()
                outputStream.close()
                
                return pdfFile
            } catch (e: Exception) {
                Log.e(TAG, "Error generating folder list PDF", e)
                return null
            }
        }

        

        /**
         * Generate a PDF with all expenses (for the "All" folder view) with grid layout
         * This special version doesn't filter by folder name
         */
        fun generateAllExpensesGridPdf(context: Context, expenses: List<Expense>, gridSize: GridSize): File? {
            try {
                Log.d(TAG, "Generating Grid PDF for All expenses with grid size: ${gridSize.name}")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val pdfFile = File(context.externalCacheDir, "RecordApp_AllExpenses_Grid_$timestamp.pdf")
                val outputStream = FileOutputStream(pdfFile)
                
                // Get settings
                val settings = SettingsManager.getInstance(context)
                val compressionQuality = settings.pdfQuality
                
                // Create PDF writer and document
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument, PageSize.A4)
                
                // Get page dimensions for calculations
                val pageWidth = document.pdfDocument.defaultPageSize.width
                val pageHeight = document.pdfDocument.defaultPageSize.height
                
                // Helper function to add a row to the summary table
                fun addSummaryRow(table: Table, label: String, value: String) {
                    table.addCell(
                        Cell().add(
                            Paragraph(label)
                                .setFontSize(11f)
                                .setBold()
                        ).setBorder(null)
                    )
                    
                    table.addCell(
                        Cell().add(
                            Paragraph(value)
                                .setFontSize(11f)
                        ).setBorder(null)
                    )
                }
                
                // Filter expenses that have images
                val expensesWithImages = expenses.filter { it.imagePath != null }
                
                // Check if we have any expenses with images
                if (expensesWithImages.isEmpty()) {
                    Log.e(TAG, "No expenses with images found in All expenses")
                    return null
                }
                
                // Log the count of expenses with images
                Log.d(TAG, "Processing ${expensesWithImages.size} expenses with images out of ${expenses.size} total expenses")
                
                // Group expenses by folder
                val expensesByFolder = expensesWithImages.groupBy { it.folderName }
                
                // Add title with better styling
                document.add(
                    Paragraph("All Expenses Report")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(18f)
                        .setBold()
                )
                
                // Add generation info
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val generationInfo = "Generated on: ${dateFormatter.format(Date())}"
                document.add(
                    Paragraph(generationInfo)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontSize(10f)
                        .setItalic()
                )
                
                document.add(LineSeparator(SolidLine(1f)).setMarginTop(5f).setMarginBottom(10f))
                
                // Add summary section
                val totalCount = expensesWithImages.size
                val totalAmount = expenses.sumOf { it.amount }
                val currencySymbol = settings.currencySymbol
                
                // Create a summary table
                val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setBorder(null)
                
                // Add summary data
                addSummaryRow(summaryTable, "Total Expenses", totalCount.toString())
                addSummaryRow(summaryTable, "Total Amount", "$currencySymbol ${totalAmount}")
                
                document.add(summaryTable)
                document.add(Paragraph("\n"))
                document.add(LineSeparator(SolidLine(0.5f)).setMarginTop(5f).setMarginBottom(10f))
                
                // Define grid columns and rows based on grid size
                val cols = when(gridSize) {
                    GridSize.ONE_BY_ONE -> 1
                    GridSize.TWO_BY_TWO -> 2
                    GridSize.TWO_BY_THREE -> 2
                    GridSize.TWO_BY_FOUR -> 2
                    GridSize.MAGAZINE_LAYOUT -> 3
                }
                
                val rows = when(gridSize) {
                    GridSize.ONE_BY_ONE -> 1
                    GridSize.TWO_BY_TWO -> 2
                    GridSize.TWO_BY_THREE -> 3
                    GridSize.TWO_BY_FOUR -> 4
                    GridSize.MAGAZINE_LAYOUT -> 3
                }
                
                // Calculate cell dimensions with proper padding
                val cellPadding = 10f
                val cellWidth = (pageWidth / cols) - (2 * cellPadding)
                val cellHeight = (pageHeight / (rows + 1)) - (2 * cellPadding) // Account for header and summary
                
                // Process each folder separately with its own heading and details
                expensesByFolder.forEach { (folderName, folderExpenses) ->
                    // Sort by display order
                    val sortedFolderExpenses = folderExpenses.sortedWith(compareBy { it.displayOrder })
                    
                    // Calculate folder-specific metrics
                    val folderTotalCount = sortedFolderExpenses.size
                    val folderTotalAmount = sortedFolderExpenses.sumOf { it.amount }
                    
                    // Add folder heading with details
                    document.add(
                        Paragraph("\nFolder: $folderName")
                            .setTextAlignment(TextAlignment.LEFT)
                            .setFontSize(14f)
                            .setBold()
                    )
                    
                    // Add folder summary
                    document.add(
                        Paragraph("Expenses: $folderTotalCount | Total Amount: $currencySymbol $folderTotalAmount")
                            .setTextAlignment(TextAlignment.LEFT)
                            .setFontSize(11f)
                            .setItalic()
                    )
                    
                    document.add(LineSeparator(DottedLine(1f)).setMarginTop(5f).setMarginBottom(10f))
                    
                    // Add a detailed list table for this folder
                    val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(8f, 20f, 15f, 15f, 42f)))
                        .setWidth(UnitValue.createPercentValue(100f))
                    
                    // Add table headers
                    arrayOf("No.", "Date", "Serial No.", "Amount", "Description").forEach { header ->
                        detailsTable.addHeaderCell(
                            Cell().add(
                                Paragraph(header)
                                    .setFontSize(10f)
                                    .setBold()
                            ).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        )
                    }
                    
                    // Add expense rows
                    sortedFolderExpenses.forEachIndexed { index, expense ->
                        // Index
                        detailsTable.addCell(
                            Cell().add(
                                Paragraph((index + 1).toString())
                                    .setFontSize(9f)
                            )
                        )
                        
                        // Date
                        detailsTable.addCell(
                            Cell().add(
                                Paragraph(expense.getFormattedTimestamp())
                                    .setFontSize(9f)
                            )
                        )
                        
                        // Serial Number
                        detailsTable.addCell(
                            Cell().add(
                                Paragraph(expense.serialNumber)
                                    .setFontSize(9f)
                            )
                        )
                        
                        // Amount
                        detailsTable.addCell(
                            Cell().add(
                                Paragraph("$currencySymbol ${expense.amount}")
                                    .setFontSize(9f)
                            )
                        )
                        
                        // Description
                        detailsTable.addCell(
                            Cell().add(
                                Paragraph(expense.description)
                                    .setFontSize(9f)
                            )
                        )
                    }
                    
                    document.add(detailsTable)
                    document.add(Paragraph("\n"))
                    
                    // For single images per page, we'll handle them differently
                    if (gridSize == GridSize.ONE_BY_ONE) {
                        processIndividualImages(document, sortedFolderExpenses, context, compressionQuality, pageWidth, pageHeight)
                    } else {
                        // Process grid layout
                        processGridLayout(document, sortedFolderExpenses, context, compressionQuality, 
                                         cols, rows, cellWidth, cellHeight, cellPadding, gridSize)
                    }
                    
                    // Add a page break after each folder except the last one
                    if (folderName != expensesByFolder.keys.last()) {
                        document.add(AreaBreak())
                    }
                }
                
                // Add footer
                document.add(LineSeparator(SolidLine(1f)).setMarginTop(10f).setMarginBottom(5f))
                document.add(
                    Paragraph("RecordApp - Generated PDF Report")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8f)
                        .setItalic()
                )
                
                // Close document
                document.close()
                outputStream.close()
                
                return pdfFile
            } catch (e: Exception) {
                Log.e(TAG, "Error generating All expenses grid PDF", e)
                return null
            }
        }

        /**
         * Helper function to add a row to a summary table
         */
        private fun addSummaryRow(table: Table, label: String, value: String) {
            table.addCell(
                Cell().add(
                    Paragraph(label)
                        .setFontSize(11f)
                        .setBold()
                ).setBorder(null)
            )
            
            table.addCell(
                Cell().add(
                    Paragraph(value)
                        .setFontSize(11f)
                ).setBorder(null)
            )
        }
    }
    
    /**
     * Generate a PDF document with the given content
     */
    fun generatePdf(
        outputStream: OutputStream,
        title: String,
        description: String,
        image: Bitmap? = null,
        additionalInfo: Map<String, String> = emptyMap(),
        table: Table? = null
    ) {
        // Create PDF document
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
        
        // Add description if provided
        if (description.isNotEmpty()) {
            document.add(
                Paragraph(description)
                    .setFontSize(12f)
                    .setMarginBottom(15f)
            )
        }
        
        // Add image if provided
        image?.let {
            try {
                val stream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val imageData = ImageDataFactory.create(stream.toByteArray())
                val pdfImage = Image(imageData)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setWidth(UnitValue.createPercentValue(80f))
                document.add(pdfImage)
                document.add(Paragraph("\n"))
            } catch (e: Exception) {
                Log.e(TAG, "Error adding image to PDF", e)
            }
        }
        
        // Add additional information if provided
        if (additionalInfo.isNotEmpty()) {
            val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                .setWidth(UnitValue.createPercentValue(100f))
            
            additionalInfo.forEach { (key, value) ->
                infoTable.addCell(
                    Cell().add(
                        Paragraph(key)
                            .setFontSize(11f)
                            .setBold()
                    ).setBackgroundColor(ColorConstants.LIGHT_GRAY)
                )
                
                infoTable.addCell(
                    Cell().add(
                        Paragraph(value)
                            .setFontSize(11f)
                    )
                )
            }
            
            document.add(infoTable)
            document.add(Paragraph("\n"))
        }
        
        // Add table if provided
        table?.let {
            document.add(it)
        }
        
        // Close the document
        document.close()
    }
    
    /**
     * Generate a PDF document for a folder of images
     */

        // Create PDF document
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
        
        // Add folder information
        document.add(
            Paragraph("Total images: ${imageUris.size}")
                .setFontSize(12f)
                .setMarginBottom(15f)
        )
        
        // Add each image with its description and serial number
        imageUris.forEachIndexed { index, uri ->
            try {
                // Add page break for all but the first image
                if (index > 0) {
                    document.add(AreaBreak())
                }
                
                // Add image index
                document.add(
                    Paragraph("Image ${index + 1} of ${imageUris.size}")
                        .setFontSize(14f)
                        .setBold()
                )
                
                // Add serial number if available
                val serialNumber = serialNumbers[uri]
                if (!serialNumber.isNullOrEmpty()) {
                    document.add(
                        Paragraph("Serial Number: $serialNumber")
                            .setFontSize(12f)
                    )
                }
                
                // Add description if available
                val description = descriptions[uri]
                if (!description.isNullOrEmpty()) {
                    document.add(
                        Paragraph("Description: $description")
                            .setFontSize(12f)
                    )
                }
                
                document.add(Paragraph("\n"))
                
                // Add image
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val imageData = ImageDataFactory.create(stream.toByteArray())
                val pdfImage = Image(imageData)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setWidth(UnitValue.createPercentValue(90f))
                document.add(pdfImage)
                
                // Clean up
                bitmap.recycle()
                inputStream?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error adding image to folder PDF", e)
                document.add(
                    Paragraph("Error loading image: ${e.message}")
                        .setFontSize(12f)
                        .setItalic()
                )
            }
        }
        
        // Close the document
        document.close()
    }
    
    /**
     * Create a PDF document with standard page setup
     */
    fun createDocument(outputStream: OutputStream): Document {
        val pdfWriter = PdfWriter(outputStream)
        val pdfDocument = PdfDocument(pdfWriter)
        // Set basic PDF document properties
        pdfDocument.documentInfo.apply {
            title = "RecordApp Export"
            author = "RecordApp"
            subject = "Expenses Report"
            keywords = "RecordApp, Expenses, PDF"
            creator = "RecordApp"
        }
        return Document(pdfDocument, PageSize.A4)
    }
    
    /**
     * Export users to PDF file
     * 
     * @param users List of users to export
     * @param filename Name for the export file
     * @return The created PDF file
     */
    fun exportUsers(users: List<User>, filename: String): File {
        val file = File(context.getExternalFilesDir(null), filename)
        
        // Create PDF document
        PdfWriter(file).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                // Set document info for this export too
                pdfDoc.documentInfo.apply {
                    title = "RecordApp Users Export"
                    author = "RecordApp"
                    subject = "Users Report"
                    keywords = "RecordApp, Users, PDF"
                    creator = "RecordApp"
                }
                Document(pdfDoc, PageSize.A4).use { document ->
                    // Add title
                    val title = Paragraph("User Management Report")
                        .setFontSize(18f)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                    document.add(title)
                    
                    // Add generation timestamp
                    val timestamp = Paragraph("Generated on: ${formatCurrentTimestamp()}")
                        .setFontSize(10f)
                        .setItalic()
                        .setTextAlignment(TextAlignment.RIGHT)
                    document.add(timestamp)
                    
                    document.add(Paragraph("\n"))
                    
                    // Summary section
                    val totalUsers = users.size
                    
                    val summary = Paragraph()
                        .add("Total Users: $totalUsers\n")
                        .setFontSize(12f)
                    document.add(summary)
                    
                    document.add(Paragraph("\n"))
                    
                    // Create user table
                    val table = Table(UnitValue.createPercentArray(floatArrayOf(15f, 30f, 25f, 30f)))
                        .useAllAvailableWidth()
                    
                    // Add header row
                    addTableHeader(table)
                    
                    // Add data rows
                    users.forEach { user ->
                        addUserRow(table, user)
                    }
                    
                    document.add(table)
                    
                    // Add footer
                    val footer = Paragraph("RecordApp - Confidential")
                        .setFontSize(8f)
                        .setItalic()
                        .setTextAlignment(TextAlignment.CENTER)
                    document.add(footer)
                }
            }
        }
        
        return file
    }
    
    /**
     * Add table header to PDF table
     */
    private fun addTableHeader(table: Table) {
        listOf("ID", "Email", "Name", "Created").forEach { headerTitle ->
            table.addCell(
                Cell().add(
                    Paragraph(headerTitle)
                        .setBold()
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                )
            )
        }
    }
    
    /**
     * Add user row to PDF table
     */
    private fun addUserRow(table: Table, user: User) {
        // ID
        table.addCell(Cell().add(Paragraph(user.id.take(8) + "...")))
        
        // Email
        table.addCell(Cell().add(Paragraph(user.email)))
        
        // Name
        table.addCell(Cell().add(Paragraph(user.name.ifEmpty { "-" })))
        
        // Created
        table.addCell(Cell().add(Paragraph(formatTimestamp(user.creationTime))))
    }
    
    /**
     * Format timestamp as readable date
     */
    private fun formatTimestamp(timestamp: Long): String {
        return DateUtils.formatTimestamp(timestamp)
    }
    
    /**
     * Get current formatted timestamp
     */
    private fun formatCurrentTimestamp(): String {
        val now = LocalDateTime.now()
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
} 