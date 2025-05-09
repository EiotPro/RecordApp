package com.example.recordapp.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Utility class for file operations
 */
object FileUtils {
    private const val TAG = "FileUtils"
    
    // Default compression quality (0-100)
    const val DEFAULT_COMPRESSION_QUALITY = 80
    
    // Default max image dimension for scaling
    private const val DEFAULT_MAX_DIMENSION = 1200
    
    /**
     * Create folder directory if it doesn't exist
     */
    fun createFolderIfNotExists(context: Context, folderName: String): File {
        // Make sure folder name is valid - replace invalid characters with underscore
        val sanitizedFolderName = folderName.replace(Regex("[^a-zA-Z0-9_\\-.]"), "_")
        
        // Create base images directory
        val baseDir = File(context.getExternalFilesDir(null), "images")
        if (!baseDir.exists()) {
            val created = baseDir.mkdirs()
            Log.d(TAG, "Creating base image directory: $baseDir, success: $created")
        }
        
        // Create specific folder
        val folder = File(baseDir, sanitizedFolderName)
        if (!folder.exists()) {
            val created = folder.mkdirs()
            Log.d(TAG, "Creating folder: $sanitizedFolderName, success: $created, path: ${folder.absolutePath}")
        } else {
            Log.d(TAG, "Folder already exists: $sanitizedFolderName, path: ${folder.absolutePath}")
        }
        
        return folder
    }

    /**
     * Create image file with specific folder
     */
    fun createImageFile(context: Context, folderName: String = "default"): Pair<File, Uri> {
        // Create folder if it doesn't exist
        val storageDir = createFolderIfNotExists(context, folderName)
        
        // Create a unique filename
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        
        val file = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
        
        Log.d(TAG, "Created image file at: ${file.absolutePath} in folder: $folderName")
        
        // Get a content URI using FileProvider
        val uri = getUriForFile(context, file)
        
        return Pair(file, uri)
    }

    /**
     * Get a URI for the file using FileProvider
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    /**
     * Save an image bitmap to a file
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int = DEFAULT_COMPRESSION_QUALITY): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error saving bitmap to file", e)
            false
        }
    }
    
    /**
     * Get a bitmap from a URI
     */
    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bitmap from URI", e)
            null
        }
    }

    /**
     * Compress and scale an image from URI
     */
    fun compressImage(context: Context, uri: Uri, quality: Int = DEFAULT_COMPRESSION_QUALITY, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap? {
        try {
            // Get input stream from URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $uri")
                return null
            }

            // Decode image size first to determine scaling
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Calculate sample size for scaling
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxDimension)
            
            // Decode bitmap with sample size
            val newInputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (newInputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $uri")
                return null
            }
            
            val decodingOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }
            val scaledBitmap = BitmapFactory.decodeStream(newInputStream, null, decodingOptions)
            newInputStream.close()

            if (scaledBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                return null
            }

            // Further compress the bitmap
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val compressedData = outputStream.toByteArray()
            return BitmapFactory.decodeByteArray(compressedData, 0, compressedData.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image: ${e.message}", e)
            return null
        }
    }

    /**
     * Calculate sample size for downscaling images
     */
    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        if (width > maxDimension || height > maxDimension) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than or equal to the requested maxDimension
            while ((halfWidth / sampleSize) >= maxDimension || (halfHeight / sampleSize) >= maxDimension) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    /**
     * Save image from URI to a specific folder with compression
     */
    fun saveImageToFolder(context: Context, sourceUri: Uri, folderName: String, quality: Int = DEFAULT_COMPRESSION_QUALITY): Uri? {
        return try {
            Log.d(TAG, "Saving image to folder: $folderName with URI: $sourceUri")
            
            // Ensure folder exists
            val folder = createFolderIfNotExists(context, folderName)
            if (!folder.exists()) {
                Log.e(TAG, "Failed to create folder: $folderName")
                return null
            }
            
            // Compress the image
            val compressedBitmap = compressImage(context, sourceUri, quality)
            if (compressedBitmap == null) {
                Log.e(TAG, "Failed to compress image for URI: $sourceUri")
                return null
            }
            
            // Create a file in the specified folder
            val destinationFile = createImageFile(context, folderName)
            Log.d(TAG, "Created destination file: ${destinationFile.first.absolutePath}")
            
            // Save the compressed bitmap to the file
            if (saveBitmapToFile(compressedBitmap, destinationFile.first, quality)) {
                Log.d(TAG, "Image saved to folder: $folderName with compression quality: $quality")
                Log.d(TAG, "Saved to path: ${destinationFile.first.absolutePath}")
                Log.d(TAG, "Generated URI: ${destinationFile.second}")
                
                // Return a content URI for the saved file
                destinationFile.second
            } else {
                Log.e(TAG, "Failed to save bitmap to file")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to folder: ${e.message}", e)
            null
        }
    }
    
    /**
     * List all folders containing images
     */
    fun listImageFolders(context: Context): List<String> {
        val baseDir = File(context.getExternalFilesDir(null), "images")
        if (!baseDir.exists()) {
            return emptyList()
        }
        
        return baseDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?: emptyList()
    }
    
    /**
     * Get all images in a folder
     */
    fun getImagesInFolder(context: Context, folderName: String): List<Uri> {
        val folder = File(context.getExternalFilesDir(null), "images/$folderName")
        if (!folder.exists()) {
            return emptyList()
        }
        
        return folder.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".jpg", ignoreCase = true) }
            ?.map { getUriForFile(context, it) }
            ?: emptyList()
    }
    
    /**
     * Get image count in a folder
     */
    fun getImageCountInFolder(context: Context, folderName: String): Int {
        val folder = File(context.getExternalFilesDir(null), "images/$folderName")
        if (!folder.exists()) {
            return 0
        }
        
        return folder.listFiles()
            ?.count { it.isFile && it.name.endsWith(".jpg", ignoreCase = true) }
            ?: 0
    }
    
    /**
     * Get sharing intent for a file
     */
    fun getShareFileIntent(context: Context, file: File): Intent {
        Log.d(TAG, "Creating share intent for file: ${file.absolutePath}")
        val uri = try {
            getUriForFile(context, file)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting URI for file: ${file.absolutePath}", e)
            // Try again with error handling
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        }
        
        Log.d(TAG, "Generated URI for sharing: $uri")
        val mimeType = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            file.name.endsWith(".csv", ignoreCase = true) -> "text/csv"
            file.name.endsWith(".jpg", ignoreCase = true) || 
            file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            else -> "*/*"
        }
        
        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Creates intent to view a PDF file
     */
    fun getViewPdfIntent(context: Context, file: File): Intent {
        Log.d(TAG, "Creating view intent for PDF: ${file.absolutePath}")
        val uri = try {
            getUriForFile(context, file)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting URI for PDF file: ${file.absolutePath}", e)
            // Try again with error handling
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        }
        
        Log.d(TAG, "Generated URI for viewing: $uri")
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // Creates a temporary file for storing camera output
    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
    
    // Get the Documents directory for storage
    fun getDocumentsDirectory(context: Context): File {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        return storageDir
    }
    
    // Get the properly formatted date as a string
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    // Create a file in the appropriate storage location based on Android version
    fun createOutputFile(context: Context, filename: String, mimeType: String): Pair<Uri?, OutputStream?> {
        // First try the MediaStore on Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                }
                
                val resolver: ContentResolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                
                if (uri != null) {
                    val outputStream = resolver.openOutputStream(uri)
                    if (outputStream != null) {
                        return Pair(uri, outputStream)
                    }
                    
                    Log.e(TAG, "Failed to open output stream for MediaStore URI")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error using MediaStore for file creation", e)
            }
        }
        
        // For all Android versions - fallback to app-specific directory
        try {
            val documentsDir = getDocumentsDirectory(context)
            val file = File(documentsDir, filename)
            
            try {
                val outputStream = FileOutputStream(file)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                return Pair(uri, outputStream)
            } catch (e: IOException) {
                Log.e(TAG, "Error creating output stream for file", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating file in app directory", e)
        }
        
        // Last resort - use cache directory
        try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, filename)
            val outputStream = FileOutputStream(file)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            Log.d(TAG, "Using cache directory as fallback: ${file.absolutePath}")
            return Pair(uri, outputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating file in cache directory", e)
        }
        
        return Pair(null, null)
    }
} 