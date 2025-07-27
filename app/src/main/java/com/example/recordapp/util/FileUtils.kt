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
import com.example.recordapp.util.SettingsManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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
        val timeStamp = DateUtils.getTimestampForFileName()
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
            Log.d(TAG, "Saving bitmap to file: ${file.absolutePath}")
            Log.d(TAG, "Bitmap size: ${bitmap.width}x${bitmap.height}")
            Log.d(TAG, "Quality: $quality")

            // Ensure parent directory exists
            file.parentFile?.let { parent ->
                if (!parent.exists()) {
                    val created = parent.mkdirs()
                    Log.d(TAG, "Created parent directory: $created")
                }
            }

            // Save the bitmap
            FileOutputStream(file).use { out ->
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
                Log.d(TAG, "Bitmap compression success: $success")

                if (!success) {
                    Log.e(TAG, "Bitmap compression returned false")
                    return false
                }
            }

            // Verify file was written
            if (!file.exists()) {
                Log.e(TAG, "File does not exist after save operation")
                return false
            }

            val fileSize = file.length()
            Log.d(TAG, "File saved successfully, size: $fileSize bytes")

            if (fileSize == 0L) {
                Log.e(TAG, "File is empty after save operation")
                return false
            }

            true
        } catch (e: IOException) {
            Log.e(TAG, "Error saving bitmap to file: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error saving bitmap to file: ${e.message}", e)
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
     * Save an image to a specific folder
     */
    fun saveImageToFolder(context: Context, sourceUri: Uri, folderName: String, quality: Int? = null): Uri? {
        return try {
            Log.d(TAG, "Saving image to folder: $folderName with URI: $sourceUri")
            
            // Get settings manager for compression quality
            val settingsManager = SettingsManager.getInstance(context)
            val compressionQuality = quality ?: settingsManager.imageCompression
            
            Log.d(TAG, "Using compression quality: $compressionQuality")
            
            // Ensure folder exists
            val folder = createFolderIfNotExists(context, folderName)
            if (!folder.exists()) {
                Log.e(TAG, "Failed to create folder: $folderName")
                return null
            }
            
            // Compress the image
            val compressedBitmap = compressImage(context, sourceUri, compressionQuality)
            if (compressedBitmap == null) {
                Log.e(TAG, "Failed to compress image for URI: $sourceUri")
                return null
            }
            
            // Create a file in the specified folder
            val destinationFile = createImageFile(context, folderName)
            Log.d(TAG, "Created destination file: ${destinationFile.first.absolutePath}")
            
            // Save the compressed bitmap to the file
            if (saveBitmapToFile(compressedBitmap, destinationFile.first, compressionQuality)) {
                Log.d(TAG, "Image saved to folder: $folderName with compression quality: $compressionQuality")
                Log.d(TAG, "Saved to path: ${destinationFile.first.absolutePath}")
                Log.d(TAG, "Generated URI: ${destinationFile.second}")
                
                // Return a content URI for the saved file
                return destinationFile.second
            } else {
                Log.e(TAG, "Failed to save bitmap to file")
                return null
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
        val timeStamp = DateUtils.getTimestampForFileName()
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
    
    /**
     * Get the output directory for exported files
     */
    fun getOutputDirectory(context: Context): File {
        val settingsManager = SettingsManager.getInstance(context)
        val storageLocation = settingsManager.storageLocation
        
        val outputDir = when (storageLocation) {
            "downloads" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10+, we'll use the app-specific directory but with a "Downloads" subfolder
                    val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    downloadsDir
                } else {
                    // For older Android versions, we can use the public Downloads directory
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                }
            }
            else -> {
                // Default to app's documents directory
                getDocumentsDirectory(context)
            }
        }
        
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        return outputDir
    }
    
    // Get the properly formatted date as a string
    fun getFormattedDate(): String {
        return DateUtils.getFormattedDateForFileName()
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

    /**
     * Rotate a bitmap by specified degrees
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
    
    /**
     * Rotate an image from URI and save it back to the same location
     */
    fun rotateImage(context: Context, imageUri: Uri, degrees: Float, compressionQuality: Int = DEFAULT_COMPRESSION_QUALITY): Uri? {
        try {
            Log.d(TAG, "=== STARTING IMAGE ROTATION ===")
            Log.d(TAG, "Rotating image by $degrees degrees: $imageUri")
            Log.d(TAG, "URI scheme: ${imageUri.scheme}")
            Log.d(TAG, "URI path: ${imageUri.path}")

            // Get bitmap from URI
            Log.d(TAG, "Step 1: Loading bitmap from URI...")
            val bitmap = getBitmapFromUri(context, imageUri)
            if (bitmap == null) {
                Log.e(TAG, "Failed to load bitmap from URI: $imageUri")
                return null
            }
            Log.d(TAG, "Bitmap loaded successfully: ${bitmap.width}x${bitmap.height}")

            // Rotate the bitmap
            Log.d(TAG, "Step 2: Rotating bitmap by $degrees degrees...")
            val rotatedBitmap = rotateBitmap(bitmap, degrees)
            Log.d(TAG, "Bitmap rotated successfully: ${rotatedBitmap.width}x${rotatedBitmap.height}")

            // Get the file path from URI
            Log.d(TAG, "Step 3: Getting file from URI...")
            val file = getFileFromUri(context, imageUri)
            if (file == null) {
                Log.e(TAG, "Failed to get file from URI: $imageUri")
                return null
            }
            Log.d(TAG, "File resolved: ${file.absolutePath}")
            Log.d(TAG, "File exists: ${file.exists()}")
            Log.d(TAG, "File can write: ${file.canWrite()}")

            // Save the rotated bitmap back to the file
            Log.d(TAG, "Step 4: Saving rotated bitmap to file...")

            // ENHANCED: Create a backup and ensure file is writable
            val backupFile = File(file.parentFile, "${file.nameWithoutExtension}_backup_${System.currentTimeMillis()}.jpg")
            try {
                // Create backup of original
                if (file.exists()) {
                    file.copyTo(backupFile, overwrite = true)
                    Log.d(TAG, "Created backup: ${backupFile.absolutePath}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not create backup: ${e.message}")
            }

            // Ensure file is writable
            if (file.exists() && !file.canWrite()) {
                try {
                    file.setWritable(true)
                    Log.d(TAG, "Made file writable")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not make file writable: ${e.message}")
                }
            }

            if (saveBitmapToFile(rotatedBitmap, file, compressionQuality)) {
                Log.d(TAG, "Image rotated successfully and saved to: ${file.absolutePath}")

                // Clean up backup file
                try {
                    if (backupFile.exists()) {
                        backupFile.delete()
                        Log.d(TAG, "Cleaned up backup file")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not clean up backup: ${e.message}")
                }

                Log.d(TAG, "=== ROTATION COMPLETED SUCCESSFULLY ===")
                return imageUri
            } else {
                Log.e(TAG, "Failed to save rotated bitmap to file")

                // Restore from backup if save failed
                try {
                    if (backupFile.exists()) {
                        backupFile.copyTo(file, overwrite = true)
                        backupFile.delete()
                        Log.d(TAG, "Restored from backup after save failure")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Could not restore from backup: ${e.message}")
                }

                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating image: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Get File object from Uri
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        try {
            Log.d(TAG, "=== RESOLVING FILE FROM URI ===")
            Log.d(TAG, "Input URI: $uri")
            Log.d(TAG, "URI scheme: ${uri.scheme}")
            Log.d(TAG, "URI path: ${uri.path}")

            // Handle content:// scheme
            if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
                Log.d(TAG, "Handling content:// URI")

                // Check if it's a FileProvider URI from our app
                val path = uri.path
                Log.d(TAG, "URI path: $path")

                if (path != null && path.contains("/external/")) {
                    Log.d(TAG, "Found FileProvider external path")
                    val segments = path.split("/external/").toTypedArray()
                    if (segments.size > 1) {
                        val relativePath = segments[1]
                        val file = File(context.getExternalFilesDir(null), relativePath)
                        Log.d(TAG, "Resolved to external files: ${file.absolutePath}")
                        return file
                    }
                }

                // For other content URIs, try to get the actual file path
                Log.d(TAG, "Trying to query content resolver for file path...")
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                        Log.d(TAG, "DATA column index: $columnIndex")
                        if (columnIndex != -1) {
                            val filePath = cursor.getString(columnIndex)
                            Log.d(TAG, "Found file path from cursor: $filePath")
                            if (!filePath.isNullOrEmpty()) {
                                val file = File(filePath)
                                Log.d(TAG, "Resolved to file: ${file.absolutePath}")
                                return file
                            }
                        }
                    }
                }

                // If we couldn't get the file path, create a copy in the cache directory
                Log.d(TAG, "Creating temporary copy in cache directory...")
                val fileName = getFileNameFromUri(context, uri) ?: "temp_file"
                val extension = getFileExtensionFromUri(context, uri) ?: ""
                val tempFile = File(context.cacheDir, "${fileName}_copy${if (extension.isNotEmpty()) ".$extension" else ""}")
                Log.d(TAG, "Temp file path: ${tempFile.absolutePath}")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                Log.d(TAG, "Copied content to temp file: ${tempFile.absolutePath}")
                return tempFile
            }
            // Handle file:// scheme
            else if (ContentResolver.SCHEME_FILE == uri.scheme) {
                Log.d(TAG, "Handling file:// URI")
                val file = File(uri.path ?: "")
                Log.d(TAG, "Resolved to file: ${file.absolutePath}")
                return file
            }

            Log.e(TAG, "Could not determine file path from URI: $uri")
            Log.d(TAG, "=== FILE RESOLUTION FAILED ===")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file from URI: ${e.message}", e)
            Log.d(TAG, "=== FILE RESOLUTION ERROR ===")
            return null
        }
    }
    
    /**
     * Get file name from URI
     */
    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var fileName: String? = null
        
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        fileName = cursor.getString(columnIndex)
                    }
                }
            }
        }
        
        if (fileName == null) {
            fileName = uri.lastPathSegment
        }
        
        return fileName
    }
    
    /**
     * Get file extension from URI
     */
    private fun getFileExtensionFromUri(context: Context, uri: Uri): String? {
        val fileName = getFileNameFromUri(context, uri) ?: return null
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) {
            fileName.substring(dotIndex + 1)
        } else {
            null
        }
    }
} 