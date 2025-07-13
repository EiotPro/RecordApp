package com.example.recordapp.util

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import com.example.recordapp.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Unified backup module that combines functionality from:
 * - BackupRestoreManager
 * - BackupScheduler
 * - BackupWorker
 * - BackupFileUtils
 *
 * Handles all backup, restore, scheduling, and file operations
 */
object BackupModule {
    private const val TAG = "BackupModule"
    
    // Backup file prefix
    private const val BACKUP_PREFIX = "recordapp_backup_"
    
    // WorkManager tags
    private const val BACKUP_WORKER_TAG = "backup_worker"
    private const val BACKUP_WORK_NAME = "backup_periodic_work"
    
    // Backup directory names
    const val BACKUP_DIR_INTERNAL = "backups"
    const val BACKUP_DIR_EXTERNAL = "RecordApp/backups"
    
    // Shared preferences for tracking restored backups
    private const val PREF_RESTORED_BACKUPS = "restored_backups_history"
    
    // Key for storing the last backup fingerprint
    private const val PREF_LAST_BACKUP_FINGERPRINT = "last_backup_fingerprint"
    
    /**
     * Data class to represent the result of a backup operation
     */
    data class BackupResult(
        val success: Boolean,
        val message: String,
        val filePath: String? = null,
        val fileName: String? = null,
        val isDuplicate: Boolean = false,
        val storageLocation: String? = null
    )
    
    /**
     * Schedule backups based on user preferences
     *
     * @param context The application context
     * @param isEnabled Whether backups are enabled
     * @param frequency The backup frequency (daily, weekly, monthly)
     */
    fun scheduleBackup(context: Context, isEnabled: Boolean, frequency: String) {
        // Cancel any existing backup work
        cancelScheduledBackups(context)
        
        // If backups are disabled, we're done
        if (!isEnabled) {
            Log.d(TAG, "Automatic backups are disabled")
            return
        }
        
        // Determine the repeat interval based on frequency
        val (repeatInterval, timeUnit) = when (frequency.lowercase()) {
            "daily" -> Pair(24L, TimeUnit.HOURS)
            "weekly" -> Pair(7L, TimeUnit.DAYS)
            "monthly" -> Pair(30L, TimeUnit.DAYS)
            else -> Pair(7L, TimeUnit.DAYS)  // Default to weekly if unknown
        }
        
        Log.d(TAG, "Scheduling automatic backups with frequency: $frequency")
        
        // Create work constraints
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)  // Don't run when battery is low
            .setRequiresStorageNotLow(true)  // Don't run when storage is low
            .build()
        
        // Create a custom worker request
        val backupWorkRequest = PeriodicWorkRequestBuilder<BackupWorkerImpl>(
            repeatInterval = repeatInterval,
            repeatIntervalTimeUnit = timeUnit
        )
            .setConstraints(constraints)
            .addTag(BACKUP_WORKER_TAG)
            .build()
        
        // Enqueue the work request
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                backupWorkRequest
            )
        
        Log.d(TAG, "Backup scheduled with interval: $repeatInterval $timeUnit")
    }
    
    /**
     * Cancel all scheduled backup tasks
     */
    fun cancelScheduledBackups(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(BACKUP_WORK_NAME)
        
        Log.d(TAG, "Scheduled backups canceled")
    }
    
    /**
     * Create a complete app backup as a ZIP file with user-selected location
     * 
     * @param context Application context
     * @param destinationUri Optional URI where the backup should be saved (if user selected a location)
     * @param onProgressUpdate Optional callback to report progress (0.0f to 1.0f)
     * @return Result of the backup operation with file path and details
     */
    suspend fun createBackupWithProgress(
        context: Context,
        destinationUri: Uri? = null,
        onProgressUpdate: ((Float) -> Unit)? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting backup creation process with progress updates")
            
            // Log storage breakdown to help diagnose storage issues
            logStorageBreakdown(context)
            
            // Initial progress for fingerprint calculation
            onProgressUpdate?.invoke(0.05f)
            
            // Check if there are any changes since the last backup
            val currentFingerprint = generateCurrentDataFingerprint(context)
            val lastFingerprint = getLastBackupFingerprint(context)
            
            // Compare fingerprints if both exist
            if (lastFingerprint != null && currentFingerprint != null && 
                lastFingerprint == currentFingerprint && destinationUri == null) {
                // No changes detected since last backup
                Log.d(TAG, "No changes detected since last backup, skipping")
                return@withContext BackupResult(
                    success = false,
                    message = "No changes detected since the last backup. Backup skipped.",
                    filePath = null,
                    fileName = null,
                    isDuplicate = true
                )
            }
            
            // Create a timestamp for the backup file name
            val timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            )
            val backupFileName = "${BACKUP_PREFIX}${timestamp}.zip"
            
            // Get database file
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            if (!dbFile.exists()) {
                Log.e(TAG, "Database file does not exist: ${dbFile.absolutePath}")
                return@withContext BackupResult(
                    success = false,
                    message = "Database file not found",
                    filePath = null,
                    fileName = null
                )
            }
            
            // Initial progress report
            onProgressUpdate?.invoke(0.1f)
            
            // Create output stream based on the destination URI or the custom backup folder URI
            val outputStream = if (destinationUri != null) {
                // User selected a specific location for this backup
                context.contentResolver.openOutputStream(destinationUri)
            } else {
                // Use the saved custom backup folder URI
                val settings = SettingsManager.getInstance(context)
                val customFolderUri = settings.customBackupFolderUri
                
                if (customFolderUri.isEmpty()) {
                    Log.e(TAG, "No custom backup folder selected. Please select a backup location first.")
                    return@withContext BackupResult(
                        success = false,
                        message = "No backup location selected. Please select a backup location first.",
                        filePath = null,
                        fileName = null
                    )
                }
                
                try {
                    // Parse the saved URI
                    val savedUri = Uri.parse(customFolderUri)
                    
                    // Make sure we have permissions to the URI
                    try {
                        // Check if we have persistable URI permissions
                        var hasPermission = false
                        for (uriPermission in context.contentResolver.persistedUriPermissions) {
                            if (uriPermission.uri == savedUri && 
                                (uriPermission.isWritePermission || uriPermission.isReadPermission)) {
                                hasPermission = true
                                break
                            }
                        }
                        
                        // If we don't have permission, try to take it
                        if (!hasPermission) {
                            Log.w(TAG, "No persistable URI permission found for $savedUri, trying to take it")
                            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            context.contentResolver.takePersistableUriPermission(savedUri, flags)
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Failed to get or take URI permissions for backup folder", e)
                        return@withContext BackupResult(
                            success = false,
                            message = "Failed to access backup location. Please select a new location.",
                            filePath = null,
                            fileName = null
                        )
                    }
                    
                    // Get a document file representing the folder
                    val documentFile = DocumentFile.fromTreeUri(context, savedUri)
                    if (documentFile == null || !documentFile.exists() || !documentFile.isDirectory) {
                        Log.e(TAG, "Custom backup folder is invalid or does not exist: $customFolderUri")
                        return@withContext BackupResult(
                            success = false,
                            message = "Backup location is no longer accessible. Please select a new backup location.",
                            filePath = null,
                            fileName = null
                        )
                    }
                    
                    // Create a new file in the selected directory
                    val newFile = documentFile.createFile("application/zip", backupFileName)
                    if (newFile == null) {
                        Log.e(TAG, "Failed to create backup file in selected directory: $customFolderUri")
                        return@withContext BackupResult(
                            success = false,
                            message = "Failed to create backup file in the selected location. Please check permissions.",
                            filePath = null,
                            fileName = null
                        )
                    }
                    
                    // Open an output stream to the newly created file
                    context.contentResolver.openOutputStream(newFile.uri)
                } catch (e: Exception) {
                    Log.e(TAG, "Error accessing custom backup folder: ${e.message}", e)
                    return@withContext BackupResult(
                        success = false,
                        message = "Error accessing backup location: ${e.message}. Please select a new location.",
                        filePath = null,
                        fileName = null
                    )
                }
            }
            
            if (outputStream == null) {
                Log.e(TAG, "Failed to create output stream for backup")
                return@withContext BackupResult(
                    success = false,
                    message = "Failed to create backup file",
                    filePath = null,
                    fileName = null
                )
            }
            
            // Progress update after creating output stream
            onProgressUpdate?.invoke(0.2f)
            
            // Variables to track backup metrics
            var totalFilesCount = 0
            var totalOriginalSize = 0L
            
            // Create a ZIP file with organized folder structure
            try {
                // Create a ZIP file
                val zipOutputStream = ZipOutputStream(outputStream)
                val buffer = ByteArray(4096)
                
                // 1. Add database files to "database" folder
                Log.d(TAG, "Adding database files to backup")
                zipOutputStream.putNextEntry(ZipEntry("database/"))
                zipOutputStream.closeEntry()
                
                // Add main database file
                val dbFileName = AppDatabase.DATABASE_NAME
                val dbFileInput = FileInputStream(dbFile)
                zipOutputStream.putNextEntry(ZipEntry("database/$dbFileName"))
                var length: Int
                var bytesRead = 0L
                while (dbFileInput.read(buffer).also { length = it } > 0) {
                    zipOutputStream.write(buffer, 0, length)
                    bytesRead += length
                }
                zipOutputStream.closeEntry()
                dbFileInput.close()
                
                // Update metrics
                totalFilesCount++
                totalOriginalSize += dbFile.length()
                Log.d(TAG, "Added database file: $dbFileName (${dbFile.length()} bytes)")
                
                // Add -shm file if it exists (shared memory file)
                val shmFile = File(dbFile.parent, "${dbFileName}-shm")
                if (shmFile.exists()) {
                    val shmInput = FileInputStream(shmFile)
                    zipOutputStream.putNextEntry(ZipEntry("database/${dbFileName}-shm"))
                    bytesRead = 0L
                    while (shmInput.read(buffer).also { length = it } > 0) {
                        zipOutputStream.write(buffer, 0, length)
                        bytesRead += length
                    }
                    zipOutputStream.closeEntry()
                    shmInput.close()
                    
                    // Update metrics
                    totalFilesCount++
                    totalOriginalSize += shmFile.length()
                    Log.d(TAG, "Added database-shm file: ${shmFile.name} (${shmFile.length()} bytes)")
                }
                
                // Add -wal file if it exists (write-ahead log)
                val walFile = File(dbFile.parent, "${dbFileName}-wal")
                if (walFile.exists()) {
                    val walInput = FileInputStream(walFile)
                    zipOutputStream.putNextEntry(ZipEntry("database/${dbFileName}-wal"))
                    bytesRead = 0L
                    while (walInput.read(buffer).also { length = it } > 0) {
                        zipOutputStream.write(buffer, 0, length)
                        bytesRead += length
                    }
                    zipOutputStream.closeEntry()
                    walInput.close()
                    
                    // Update metrics
                    totalFilesCount++
                    totalOriginalSize += walFile.length()
                    Log.d(TAG, "Added database-wal file: ${walFile.name} (${walFile.length()} bytes)")
                }
                
                onProgressUpdate?.invoke(0.4f)
                
                // 2. Add shared_prefs to "preferences" folder
                Log.d(TAG, "Adding preferences to backup")
                zipOutputStream.putNextEntry(ZipEntry("preferences/"))
                zipOutputStream.closeEntry()
                
                val sharedPrefsDir = File(context.dataDir, "shared_prefs")
                if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                    var prefFilesCount = 0
                    var prefsTotalSize = 0L
                    
                    sharedPrefsDir.listFiles()?.forEach { prefsFile ->
                        if (prefsFile.isFile && !shouldExcludeFromBackup(prefsFile)) {
                            val prefsInput = FileInputStream(prefsFile)
                            zipOutputStream.putNextEntry(ZipEntry("preferences/${prefsFile.name}"))
                            bytesRead = 0L
                            while (prefsInput.read(buffer).also { length = it } > 0) {
                                zipOutputStream.write(buffer, 0, length)
                                bytesRead += length
                            }
                            zipOutputStream.closeEntry()
                            prefsInput.close()
                            
                            // Update metrics
                            prefFilesCount++
                            prefsTotalSize += prefsFile.length()
                            totalFilesCount++
                            totalOriginalSize += prefsFile.length()
                            
                            Log.d(TAG, "Added preferences file: ${prefsFile.name} (${prefsFile.length()} bytes)")
                        }
                    }
                    
                    Log.d(TAG, "Added $prefFilesCount preferences files ($prefsTotalSize bytes)")
                }
                
                onProgressUpdate?.invoke(0.5f)
                
                // 3. Add images directory to "images" folder
                Log.d(TAG, "Adding image files to backup")
                val imagesDir = File(context.getExternalFilesDir(null), "images")
                if (imagesDir.exists() && imagesDir.isDirectory) {
                    // Create root images folder
                    zipOutputStream.putNextEntry(ZipEntry("images/"))
                    zipOutputStream.closeEntry()
                    
                    var imageCount = 0
                    var imagesTotalSize = 0L
                    
                    // Add each subfolder (e.g., receipts, expenses, etc.)
                    imagesDir.listFiles()?.forEach { subFolder ->
                        if (subFolder.isDirectory && !shouldExcludeFromBackup(subFolder)) {
                            // Create subfolder entry
                            zipOutputStream.putNextEntry(ZipEntry("images/${subFolder.name}/"))
                            zipOutputStream.closeEntry()
                            
                            // Add images from this subfolder
                            subFolder.listFiles()?.forEach { imageFile ->
                                if (imageFile.isFile && isImageFile(imageFile) && !shouldExcludeFromBackup(imageFile)) {
                                    val imageInput = FileInputStream(imageFile)
                                    zipOutputStream.putNextEntry(ZipEntry("images/${subFolder.name}/${imageFile.name}"))
                                    bytesRead = 0L
                                    while (imageInput.read(buffer).also { length = it } > 0) {
                                        zipOutputStream.write(buffer, 0, length)
                                        bytesRead += length
                                    }
                                    zipOutputStream.closeEntry()
                                    imageInput.close()
                                    
                                    // Update metrics
                                    imageCount++
                                    imagesTotalSize += imageFile.length()
                                    totalFilesCount++
                                    totalOriginalSize += imageFile.length()
                                }
                            }
                            
                            Log.d(TAG, "Processed image subfolder: ${subFolder.name}")
                        }
                    }
                    
                    Log.d(TAG, "Added $imageCount image files ($imagesTotalSize bytes)")
                }
                
                onProgressUpdate?.invoke(0.7f)
                
                // 4. Add PDF directory to "pdf" folder - only include files that have been explicitly generated
                Log.d(TAG, "Adding PDF files to backup")
                val pdfDir = File(context.getExternalFilesDir(null), "pdf")
                if (pdfDir.exists() && pdfDir.isDirectory) {
                    // Create PDF folder
                    zipOutputStream.putNextEntry(ZipEntry("pdf/"))
                    zipOutputStream.closeEntry()
                    
                    var pdfCount = 0
                    var pdfTotalSize = 0L
                    
                    // Add PDFs from this folder
                    pdfDir.listFiles()?.forEach { pdfFile ->
                        if (pdfFile.isFile && isPdfFile(pdfFile) && !shouldExcludeFromBackup(pdfFile)) {
                            val pdfInput = FileInputStream(pdfFile)
                            zipOutputStream.putNextEntry(ZipEntry("pdf/${pdfFile.name}"))
                            bytesRead = 0L
                            while (pdfInput.read(buffer).also { length = it } > 0) {
                                zipOutputStream.write(buffer, 0, length)
                                bytesRead += length
                            }
                            zipOutputStream.closeEntry()
                            pdfInput.close()
                            
                            // Update metrics
                            pdfCount++
                            pdfTotalSize += pdfFile.length()
                            totalFilesCount++
                            totalOriginalSize += pdfFile.length()
                        }
                    }
                    
                    Log.d(TAG, "Added $pdfCount PDF files ($pdfTotalSize bytes)")
                }
                
                onProgressUpdate?.invoke(0.8f)
                
                // 5. Add backup metadata
                Log.d(TAG, "Adding backup metadata")
                zipOutputStream.putNextEntry(ZipEntry("metadata/"))
                zipOutputStream.closeEntry()
                
                // Create backup_info.txt with metadata about this backup
                val backupInfo = """
                    |RecordApp Backup
                    |Timestamp: ${LocalDateTime.now()}
                    |Files: $totalFilesCount
                    |Total Size: $totalOriginalSize bytes
                    |App Version: ${getAppVersion(context)}
                    |Android SDK: ${Build.VERSION.SDK_INT}
                    |Device: ${Build.MANUFACTURER} ${Build.MODEL}
                """.trimMargin()
                
                zipOutputStream.putNextEntry(ZipEntry("metadata/backup_info.txt"))
                zipOutputStream.write(backupInfo.toByteArray())
                zipOutputStream.closeEntry()
                
                // Close the ZIP file
                zipOutputStream.close()
                outputStream.close()
                
                // Store the current fingerprint for future comparison
            if (currentFingerprint != null) {
                saveLastBackupFingerprint(context, currentFingerprint)
            }
            
            // Final progress update
            onProgressUpdate?.invoke(1.0f)
            
                // Return success
                Log.d(TAG, "Backup completed successfully")
            return@withContext BackupResult(
                success = true,
                    message = "Backup completed successfully with $totalFilesCount files ($totalOriginalSize bytes)",
                    filePath = null, // We no longer track the exact file path for custom locations
                fileName = backupFileName,
                    storageLocation = "custom" // We now only use the custom location
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            return@withContext BackupResult(
                success = false,
                message = "Error creating backup: ${e.message}",
                    filePath = null,
                    fileName = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in backup process", e)
            return@withContext BackupResult(
                success = false,
                message = "Unexpected error in backup process: ${e.message}",
                filePath = null,
                fileName = null
            )
        }
    }
    
    /**
     * Create a backup (simplified version without progress updates)
     */
    suspend fun createBackup(context: Context): BackupResult {
        return createBackupWithProgress(context, null, null)
    }
    
    /**
     * Get a user-friendly path to display based on the storage type
     */
    fun getDisplayPath(storageLocation: String, fileName: String): String {
        return when (storageLocation) {
            "internal" -> "Internal Storage/RecordApp/backups/$fileName"
            "external" -> "Storage/RecordApp/backups/$fileName"
            "downloads" -> "Downloads/backups/$fileName"
            else -> "App Storage/$fileName"
        }
    }
    
    /**
     * Open the file if possible
     */
    fun openBackupFile(context: Context, filePath: String): Boolean {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "Backup file not found", Toast.LENGTH_SHORT).show()
                return false
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )
            
            return openFileWithIntent(context, uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening backup file", e)
            Toast.makeText(context, "Could not open backup file", Toast.LENGTH_SHORT).show()
            return false
        }
    }
    
    /**
     * Share the backup file
     */
    fun shareBackupFile(context: Context, filePath: String): Boolean {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "Backup file not found", Toast.LENGTH_SHORT).show()
                return false
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "application/zip"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            val chooser = Intent.createChooser(shareIntent, "Share Backup File")
            context.startActivity(chooser)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing backup file", e)
            Toast.makeText(context, "Could not share backup file", Toast.LENGTH_SHORT).show()
            return false
        }
    }
    
    /**
     * Open the containing folder of the backup file
     */
    fun openBackupFolder(context: Context, filePath: String): Boolean {
        try {
            val file = File(filePath).parentFile ?: return false
            if (!file.exists()) {
                Toast.makeText(context, "Backup folder not found", Toast.LENGTH_SHORT).show()
                return false
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )
            
            return openFolderWithIntent(context, uri, file)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening backup folder", e)
            Toast.makeText(context, "Could not open backup folder", Toast.LENGTH_SHORT).show()
            return false
        }
    }
    
    /**
     * Try to open a file using an intent
     */
    private fun openFileWithIntent(context: Context, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/zip")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No app found to open this file type",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }
    
    /**
     * Try to open a folder using an intent
     */
    private fun openFolderWithIntent(context: Context, uri: Uri, folder: File): Boolean {
        // Try several intent approaches to open the folder
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "resource/folder")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        
        try {
            context.startActivity(intent)
            return true
        } catch (e: ActivityNotFoundException) {
            // Try with explicit file manager intents
            val fileManagerIntent = Intent(Intent.ACTION_VIEW)
            fileManagerIntent.setDataAndType(Uri.parse("content://com.android.externalstorage.documents/document/primary:"), "resource/folder")
            
            try {
                context.startActivity(fileManagerIntent)
                return true
            } catch (e2: Exception) {
                // Last resort: Just show a toast with the path
                Toast.makeText(
                    context,
                    "Backup saved to: ${folder.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
                return false
            }
        }
    }
    
    /**
     * Verify backup settings and access
     * Returns a diagnostic message
     */
    fun verifyBackupSettings(context: Context): String {
        val settings = SettingsManager.getInstance(context)
        val location = settings.storageLocation
        val sb = StringBuilder()
        
        sb.appendLine("Storage location: $location")
        
        when (location) {
            "internal" -> {
                val dir = File(context.filesDir, BACKUP_DIR_INTERNAL)
                sb.appendLine("Internal dir exists: ${dir.exists()}")
                sb.appendLine("Internal dir can write: ${dir.canWrite()}")
                if (!dir.exists()) {
                    val created = dir.mkdirs()
                    sb.appendLine("Created internal dir: $created")
                }
            }
            "external" -> {
                val externalDir = context.getExternalFilesDir(null)
                sb.appendLine("External dir null: ${externalDir == null}")
                if (externalDir != null) {
                    val dir = File(externalDir, BACKUP_DIR_EXTERNAL)
                    sb.appendLine("External backup dir exists: ${dir.exists()}")
                    sb.appendLine("External backup dir can write: ${dir.canWrite()}")
                    if (!dir.exists()) {
                        val created = dir.mkdirs()
                        sb.appendLine("Created external dir: $created")
                    }
                }
            }
            "downloads" -> {
                val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                sb.appendLine("Downloads dir null: ${downloadsDir == null}")
                if (downloadsDir != null) {
                    val dir = File(downloadsDir, BACKUP_DIR_INTERNAL)
                    sb.appendLine("Downloads backup dir exists: ${dir.exists()}")
                    sb.appendLine("Downloads backup dir can write: ${dir.canWrite()}")
                    if (!dir.exists()) {
                        val created = dir.mkdirs()
                        sb.appendLine("Created downloads dir: $created")
                    }
                }
            }
        }
        
        return sb.toString()
    }
    
    /**
     * Generate a fingerprint of the current data for change detection
     * This could be implemented as a hash or other identifier
     */
    private fun generateCurrentDataFingerprint(context: Context): String? {
        // Implementation would be specific to the app's data structure
        return null // Placeholder
    }
    
    /**
     * Get the last backup fingerprint from preferences
     */
    private fun getLastBackupFingerprint(context: Context): String? {
        val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        return prefs.getString(PREF_LAST_BACKUP_FINGERPRINT, null)
    }
    
    /**
     * Save the last backup fingerprint to preferences
     */
    private fun saveLastBackupFingerprint(context: Context, fingerprint: String) {
        val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_LAST_BACKUP_FINGERPRINT, fingerprint).apply()
    }
    
    /**
     * Ensure backup directories exist
     * Creates directories if they don't exist
     * @return true if directories exist or were successfully created
     */
    fun ensureDirectoriesExist(context: Context): Boolean {
        try {
            val settings = SettingsManager.getInstance(context)
            val location = settings.storageLocation
            
            Log.d(TAG, "Ensuring backup directories exist for location: $location")
            
            val dirCreated = when (location) {
                "internal" -> {
                    val dir = File(context.filesDir, BACKUP_DIR_INTERNAL)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    } else {
                        true
                    }
                }
                "external" -> {
                    val externalDir = context.getExternalFilesDir(null)
                    if (externalDir != null) {
                        val dir = File(externalDir, BACKUP_DIR_EXTERNAL)
                        if (!dir.exists()) {
                            dir.mkdirs()
                        } else {
                            true
                        }
                    } else {
                        false
                    }
                }
                "downloads" -> {
                    val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (downloadsDir != null) {
                        val dir = File(downloadsDir, BACKUP_DIR_INTERNAL)
                        if (!dir.exists()) {
                            dir.mkdirs()
                        } else {
                            true
                        }
                    } else {
                        false
                    }
                }
                else -> {
                    // Default to internal storage
                    val dir = File(context.filesDir, BACKUP_DIR_INTERNAL)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    } else {
                        true
                    }
                }
            }
            
            Log.d(TAG, "Backup directories created or exist: $dirCreated")
            return dirCreated
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring backup directories", e)
            return false
        }
    }

    /**
     * Restore an app backup from a ZIP file selected by the user
     * 
     * @param context Application context
     * @param uri URI of the ZIP file to restore
     * @param onProgressUpdate Optional callback to report progress (0.0f to 1.0f)
     * @return Result of the restore operation
     */
    @SuppressLint("SuspiciousIndentation")
    suspend fun restoreFromBackup(
        context: Context,
        uri: Uri,
        onProgressUpdate: ((Float) -> Unit)? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting restore from backup: $uri")
            
        try {
            // Initial progress update
            onProgressUpdate?.invoke(0.05f)
            
            // Create a temporary file to store the backup
            val tempFile = File(context.cacheDir, "temp_backup_${System.currentTimeMillis()}.zip")
            
            // Copy the content from the URI to the temp file
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $uri")
                return@withContext BackupResult(
                    success = false,
                    message = "Failed to read the backup file",
                    filePath = null,
                    fileName = uri.lastPathSegment
                )
            }
            
            val outputStream = FileOutputStream(tempFile)
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.close()
            inputStream.close()
            
            Log.d(TAG, "Copied backup to temporary file: ${tempFile.absolutePath}")
            
            // Update progress after copying the file
            onProgressUpdate?.invoke(0.1f)
            
            // List to track extracted files
            val extractedFiles = mutableListOf<File>()
            
            // Counters for different file types
            var dbFilesRestored = 0
            var prefFilesRestored = 0
            var imageFilesRestored = 0
            var pdfFilesRestored = 0
            var otherFilesRestored = 0
            
            // Extract the backup from the temporary file
            val zipInputStream = ZipInputStream(FileInputStream(tempFile))
            
            // Extract files from the ZIP
            var zipEntry: ZipEntry? = zipInputStream.nextEntry
            var extractCount = 0
            while (zipEntry != null) {
                val entryName = zipEntry.name
                Log.d(TAG, "Processing ZIP entry: $entryName")
                
                // Skip directory entries
                if (zipEntry.isDirectory) {
                    Log.d(TAG, "Skipping directory entry: $entryName")
                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                    continue
                }
                
                // Handle file entries based on the new folder structure
                if (entryName.startsWith("database/")) {
                        // Database files - extract to database directory
                    val fileName = entryName.substring("database/".length)
                        val outputFile = context.getDatabasePath(fileName)
                        
                        // Ensure parent directory exists
                        outputFile.parentFile?.mkdirs()
                        
                        // Extract file
                        extractFileFromZip(zipInputStream, outputFile)
                        extractedFiles.add(outputFile)
                        dbFilesRestored++
                        
                        Log.d(TAG, "Restored database file: $fileName to ${outputFile.absolutePath}")
                } else if (entryName.startsWith("preferences/")) {
                        // Preferences files - extract to shared_prefs directory
                    val fileName = entryName.substring("preferences/".length)
                        val sharedPrefsDir = File(context.dataDir, "shared_prefs")
                        sharedPrefsDir.mkdirs()
                        
                        val outputFile = File(sharedPrefsDir, fileName)
                        extractFileFromZip(zipInputStream, outputFile)
                        extractedFiles.add(outputFile)
                        prefFilesRestored++
                        
                        Log.d(TAG, "Restored preferences file: $fileName to ${outputFile.absolutePath}")
                } else if (entryName.startsWith("images/")) {
                        // Image files - extract to images directory in external storage
                        // Format: images/subfolder/image.jpg
                    val relativePath = entryName.substring("images/".length)
                    val pathParts = relativePath.split("/", limit = 2)
                        
                        if (pathParts.size == 2) {
                            val subfolder = pathParts[0]
                            val fileName = pathParts[1]
                            
                            // Create subfolder in external files directory
                            val imagesDir = File(context.getExternalFilesDir(null), "images")
                            val subfolderDir = File(imagesDir, subfolder)
                            subfolderDir.mkdirs()
                            
                            val outputFile = File(subfolderDir, fileName)
                            extractFileFromZip(zipInputStream, outputFile)
                            extractedFiles.add(outputFile)
                            imageFilesRestored++
                            
                            Log.d(TAG, "Restored image file: $fileName to ${outputFile.absolutePath}")
                        } else {
                            Log.d(TAG, "Skipping image file with invalid path: $relativePath")
                        }
                } else if (entryName.startsWith("pdf/")) {
                    // PDF files - extract to pdf directory
                    val fileName = entryName.substring("pdf/".length)
                    val pdfDir = File(context.getExternalFilesDir(null), "pdf")
                    pdfDir.mkdirs()
                    
                    val outputFile = File(pdfDir, fileName)
                    extractFileFromZip(zipInputStream, outputFile)
                    extractedFiles.add(outputFile)
                    pdfFilesRestored++
                    
                    Log.d(TAG, "Restored PDF file: $fileName to ${outputFile.absolutePath}")
                } else if (entryName.startsWith("metadata/")) {
                    // Metadata files - we don't need to restore these
                    Log.d(TAG, "Skipping metadata file: $entryName")
                } else if (entryName.startsWith("RecordAppBackup/")) {
                    // Handle legacy backup format for backward compatibility
                    handleLegacyBackupEntry(context, zipInputStream, entryName, extractedFiles, 
                        { dbFilesRestored++ }, 
                        { prefFilesRestored++ }, 
                        { imageFilesRestored++ }, 
                        { otherFilesRestored++ })
                } else {
                    // Other files - extract to files directory
                    val outputFile = File(context.filesDir, entryName)
                    
                    // Ensure parent directory exists
                    outputFile.parentFile?.mkdirs()
                    
                    extractFileFromZip(zipInputStream, outputFile)
                    extractedFiles.add(outputFile)
                    otherFilesRestored++
                    
                    Log.d(TAG, "Restored other file: $entryName to ${outputFile.absolutePath}")
                }
                
                // Move to next entry
                zipInputStream.closeEntry()
                zipEntry = zipInputStream.nextEntry
                
                // Update progress (10% - 80% range)
                extractCount++
                onProgressUpdate?.invoke(0.1f + (0.7f * (extractCount.toFloat() / (extractCount + 1))))
            }
            
            zipInputStream.close()
            
            // Delete the temp file
            tempFile.delete()
            
            // Reload preferences and database
            try {
                // Reload settings
                SettingsManager.getInstance(context).reload()
                Log.d(TAG, "Reloaded settings from restored preferences")
                
                // Force database reconnection
                AppDatabase.getInstance(context).close()
                AppDatabase.getInstance(context)
                Log.d(TAG, "Invalidated database instance to force reconnection")
            } catch (e: Exception) {
                Log.e(TAG, "Error reloading app state after restore", e)
            }
            
            // Create restore summary
            val summary = StringBuilder()
            summary.appendLine("===== RESTORE SUMMARY =====")
            summary.appendLine("Database files: $dbFilesRestored")
            summary.appendLine("Preference files: $prefFilesRestored")
            summary.appendLine("Image files: $imageFilesRestored")
            summary.appendLine("PDF files: $pdfFilesRestored")
            summary.appendLine("Other files: $otherFilesRestored")
            summary.appendLine("Total files: ${extractedFiles.size}")
            
            Log.d(TAG, summary.toString())
            
            onProgressUpdate?.invoke(0.9f)
            
            // Verify the restore was successful
            val success = extractedFiles.isNotEmpty() && dbFilesRestored > 0
            
            // Final progress update
            onProgressUpdate?.invoke(1.0f)
            
            // Return success or failure
            if (success) {
                Log.d(TAG, "Backup restored successfully with ${extractedFiles.size} files")
                return@withContext BackupResult(
                    success = true,
                    message = "Backup restored successfully with ${extractedFiles.size} files: " +
                            "$dbFilesRestored database, $prefFilesRestored preferences, " +
                            "$imageFilesRestored images, $pdfFilesRestored PDF files",
                    filePath = null,
                    fileName = uri.lastPathSegment
                )
            } else {
                Log.e(TAG, "Backup restore failed - no valid data found")
                return@withContext BackupResult(
                    success = false,
                    message = "Backup restore failed - no valid data found. " +
                            "Make sure the backup file is valid and contains database files.",
                    filePath = null,
                    fileName = uri.lastPathSegment
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            return@withContext BackupResult(
                success = false,
                message = "Error restoring backup: ${e.message}",
                filePath = null,
                fileName = null
            )
        }
    }
    
    /**
     * Helper function to handle legacy backup format entries for backward compatibility
     */
    private fun handleLegacyBackupEntry(
        context: Context,
        zipInputStream: ZipInputStream,
        entryName: String,
        extractedFiles: MutableList<File>,
        onDbRestored: () -> Unit,
        onPrefRestored: () -> Unit,
        onImageRestored: () -> Unit,
        onOtherRestored: () -> Unit
    ) {
        // Remove the root folder prefix
        val relativePath = entryName.substring("RecordAppBackup/".length)
        
        if (relativePath.startsWith("database/")) {
            // Database files
            val fileName = relativePath.substring("database/".length)
            val outputFile = context.getDatabasePath(fileName)
            outputFile.parentFile?.mkdirs()
            
            extractFileFromZip(zipInputStream, outputFile)
            extractedFiles.add(outputFile)
            onDbRestored()
            
            Log.d(TAG, "Restored legacy database file: $fileName to ${outputFile.absolutePath}")
        } else if (relativePath.startsWith("preferences/")) {
            // Preferences files
            val fileName = relativePath.substring("preferences/".length)
            val sharedPrefsDir = File(context.dataDir, "shared_prefs")
            sharedPrefsDir.mkdirs()
            
            val outputFile = File(sharedPrefsDir, fileName)
            extractFileFromZip(zipInputStream, outputFile)
            extractedFiles.add(outputFile)
            onPrefRestored()
            
            Log.d(TAG, "Restored legacy preferences file: $fileName to ${outputFile.absolutePath}")
        } else if (relativePath.startsWith("images/")) {
            // Image files
            val remainingPath = relativePath.substring("images/".length)
            val pathParts = remainingPath.split("/", limit = 2)
            
            if (pathParts.size == 2) {
                val subfolder = pathParts[0]
                val fileName = pathParts[1]
                
                val imagesDir = File(context.getExternalFilesDir(null), "images")
                val subfolderDir = File(imagesDir, subfolder)
                subfolderDir.mkdirs()
                
                val outputFile = File(subfolderDir, fileName)
                extractFileFromZip(zipInputStream, outputFile)
                extractedFiles.add(outputFile)
                onImageRestored()
                
                Log.d(TAG, "Restored legacy image file: $fileName to ${outputFile.absolutePath}")
            } else {
                Log.d(TAG, "Skipping legacy image with invalid path: $relativePath")
            }
        } else if (relativePath.startsWith("cache/")) {
            // Skip cache files from legacy backups
            Log.d(TAG, "Skipping legacy cache file: $relativePath")
        } else if (!relativePath.equals("backup_info.txt", ignoreCase = true)) {
            // Other files
            val outputFile = File(context.filesDir, relativePath)
            outputFile.parentFile?.mkdirs()
            
            extractFileFromZip(zipInputStream, outputFile)
            extractedFiles.add(outputFile)
            onOtherRestored()
            
            Log.d(TAG, "Restored legacy file: $relativePath to ${outputFile.absolutePath}")
        } else {
            // Skip info file
            Log.d(TAG, "Skipping legacy info file: $entryName")
        }
    }
    
    /**
     * Helper function to extract a file from a ZIP input stream to a destination file
     */
    private fun extractFileFromZip(zipInputStream: ZipInputStream, outputFile: File): Boolean {
        try {
            // Create parent directories if they don't exist
            outputFile.parentFile?.mkdirs()
            
            // Extract the file
            val fileOutputStream = FileOutputStream(outputFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (zipInputStream.read(buffer).also { bytesRead = it } != -1) {
                fileOutputStream.write(buffer, 0, bytesRead)
            }
            
            fileOutputStream.close()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting file from ZIP: ${outputFile.absolutePath}", e)
            return false
        }
    }
    
    /**
     * Worker implementation for performing backups
     */
    class BackupWorkerImpl(
        context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(context, params) {
        
        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
            try {
                val settings = SettingsManager.getInstance(applicationContext)
                val storageLocation = settings.storageLocation
                
                Log.d(TAG, "Starting scheduled backup with storage location: $storageLocation")
                
                // Log diagnostic information to help troubleshoot storage issues
                val diagnostics = verifyBackupSettings(applicationContext)
                Log.d(TAG, "Backup diagnostics: $diagnostics")
                
                // Use BackupModule to create backup
                val result = createBackup(applicationContext)
                
                return@withContext if (result.success) {
                    Log.d(TAG, "Backup completed successfully: ${result.filePath}")
                    Log.d(TAG, "Using storage location: ${settings.storageLocation}")
                    
                    // Prepare output data for work manager
                    val outputData = workDataOf(
                        "backup_file_path" to result.filePath,
                        "backup_file_name" to result.fileName,
                        "storage_location" to settings.storageLocation
                    )
                    Result.success(outputData)
                } else {
                    Log.e(TAG, "Backup failed: ${result.message}")
                    Result.retry()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during backup", e)
                Result.failure()
            }
        }
    }

    /**
     * Clean up temporary files that might be left from previous backup operations
     * This helps prevent storage bloat over time
     * 
     * @param context Application context
     * @return Number of files deleted
     */
    private fun cleanupTempFiles(context: Context): Int {
        try {
            Log.d(TAG, "Cleaning up temporary backup files")
            
            // 1. Clean temp files in the cache directory
            val cacheDir = context.cacheDir
            var deletedCount = 0
            var freedSpace = 0L
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.contains("temp_backup_") || 
                    file.name.endsWith(".tmp") || 
                    file.name.endsWith(".temp") ||
                    file.name.endsWith(".bak") ||
                    file.name.contains("backup_temp")) {
                    
                    val fileSize = file.length()
                    if (file.delete()) {
                        deletedCount++
                        freedSpace += fileSize
                        Log.d(TAG, "Deleted temp file: ${file.absolutePath}, size: $fileSize bytes")
                    }
                }
            }
            
            // 2. Clean temp files in the files directory
            val filesDir = context.filesDir
            filesDir.listFiles()?.forEach { file ->
                if (file.name.contains("temp_backup_") || 
                    file.name.endsWith(".tmp") || 
                    file.name.endsWith(".temp") ||
                    file.name.endsWith(".bak") ||
                    file.name.contains("backup_temp")) {
                    
                    val fileSize = file.length()
                    if (file.delete()) {
                        deletedCount++
                        freedSpace += fileSize
                        Log.d(TAG, "Deleted temp file: ${file.absolutePath}, size: $fileSize bytes")
                    }
                }
            }
            
            // 3. Clean database journal files
            val dbDir = File(context.filesDir, "databases")
            if (dbDir.exists() && dbDir.isDirectory) {
                dbDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith("-journal") || 
                        file.name.endsWith("-wal") || 
                        file.name.endsWith("-shm")) {
                        
                        val fileSize = file.length()
                        if (file.delete()) {
                            deletedCount++
                            freedSpace += fileSize
                            Log.d(TAG, "Deleted database journal file: ${file.absolutePath}, size: $fileSize bytes")
                        }
                    }
                }
            }
            
            Log.d(TAG, "Cleanup completed: deleted $deletedCount files, freed ${freedSpace/1024} KB")
            return deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up temp files", e)
            return 0
        }
    }
    
    /**
     * Log a comprehensive breakdown of storage usage
     * This helps diagnose what's actually using space in the app
     */
    private fun logStorageBreakdown(context: Context) {
        try {
            val MB = 1024 * 1024L
            
            // App files directory
            val filesDir = context.filesDir
            val totalFilesSize = calculateDirectorySize(filesDir)
            
            // Database files
            val dbDir = context.getDatabasePath(AppDatabase.DATABASE_NAME).parentFile
            val dbFiles = dbDir?.listFiles()?.filter { it.name.endsWith(".db") }?.sumOf { it.length() } ?: 0L
            val dbJournalFiles = dbDir?.listFiles()?.filter { 
                it.name.endsWith("-journal") || it.name.endsWith("-wal") || it.name.endsWith("-shm") 
            }?.sumOf { it.length() } ?: 0L
            
            // External files directory
            val externalFilesDir = context.getExternalFilesDir(null)
            val externalFilesSize = if (externalFilesDir != null) calculateDirectorySize(externalFilesDir) else 0L
            
            // Images directory
            val imagesDir = File(context.getExternalFilesDir(null), "images")
            val imagesSize = if (imagesDir.exists()) calculateDirectorySize(imagesDir) else 0L
            
            // Cache directory
            val cacheSize = calculateDirectorySize(context.cacheDir)
            
            // Find ZIP files recursively
            val zipFiles = findAllZipFiles(context)
            val zipFilesSize = zipFiles.sumOf { it.length() }
            
            // Log the breakdown
            Log.d(TAG, "===== STORAGE BREAKDOWN =====")
            Log.d(TAG, "Total app files size: ${totalFilesSize / MB} MB")
            Log.d(TAG, "Database files (.db): ${dbFiles / MB} MB")
            Log.d(TAG, "Database journal files: ${dbJournalFiles / MB} MB")
            Log.d(TAG, "External files size: ${externalFilesSize / MB} MB")
            Log.d(TAG, "Images size: ${imagesSize / MB} MB")
            Log.d(TAG, "Cache size: ${cacheSize / MB} MB")
            Log.d(TAG, "ZIP files (${zipFiles.size} files): ${zipFilesSize / MB} MB")
            
            // List the top 10 largest files
            val allFiles = findAllFiles(context).sortedByDescending { it.length() }.take(10)
            Log.d(TAG, "===== TOP 10 LARGEST FILES =====")
            allFiles.forEach { file ->
                Log.d(TAG, "${file.absolutePath}: ${file.length() / 1024} KB")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error logging storage breakdown", e)
        }
    }
    
    /**
     * Find all ZIP files in the app's storage
     */
    private fun findAllZipFiles(context: Context): List<File> {
        val zipFiles = mutableListOf<File>()
        
        try {
            // Check internal storage
            context.filesDir.walkTopDown().filter { it.isFile && it.name.endsWith(".zip") }.forEach {
                zipFiles.add(it)
            }
            
            // Check external storage
            context.getExternalFilesDir(null)?.walkTopDown()?.filter { it.isFile && it.name.endsWith(".zip") }?.forEach {
                zipFiles.add(it)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding ZIP files", e)
        }
        
        return zipFiles
    }
    
    /**
     * Find all files in the app's storage for analysis
     */
    private fun findAllFiles(context: Context): List<File> {
        val allFiles = mutableListOf<File>()
        
        try {
            // Check internal storage
            context.filesDir.walkTopDown().filter { it.isFile }.forEach {
                allFiles.add(it)
            }
            
            // Check database directory
            context.getDatabasePath(AppDatabase.DATABASE_NAME).parentFile?.walkTopDown()?.filter { it.isFile }?.forEach {
                allFiles.add(it)
            }
            
            // Check external storage
            context.getExternalFilesDir(null)?.walkTopDown()?.filter { it.isFile }?.forEach {
                allFiles.add(it)
            }
            
            // Check cache directory
            context.cacheDir.walkTopDown().filter { it.isFile }.forEach {
                allFiles.add(it)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding all files", e)
        }
        
        return allFiles
    }

    /**
     * Analyze storage usage across the app to identify what's taking up space
     * This helps diagnose storage discrepancies
     * 
     * @param context Application context
     * @return Map of category names to sizes in bytes
     */
    suspend fun analyzeStorageUsage(context: Context): Map<String, Long> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Long>()
        
        try {
            Log.d(TAG, "Starting storage analysis...")
            
            // Log detailed storage breakdown
            logStorageBreakdown(context)
            
            // 1. Analyze main directories
            val cacheSize = calculateDirectorySize(context.cacheDir)
            val filesSize = calculateDirectorySize(context.filesDir)
            
            result["cache"] = cacheSize
            result["files"] = filesSize
            
            // 2. Analyze database size
            val databaseSize = calculateDatabaseSize(context)
            result["database"] = databaseSize
            
            // 3. Analyze shared preferences
            val prefsSize = calculatePreferencesSize(context)
            result["preferences"] = prefsSize
            
            // 4. Analyze receipt images
            val receiptSize = calculateReceiptImagesSize(context)
            result["receipts"] = receiptSize
            
            // 5. Analyze temporary files
            val tempSize = calculateTempFilesSize(context)
            result["temp"] = tempSize
            
            // 6. Analyze potential residual files
            val residualSize = calculateResidualFilesSize(context)
            result["residual"] = residualSize
            
            // 7. Analyze backup files
            val backupSize = calculateBackupFilesSize(context)
            result["backups"] = backupSize
            
            // 8. Analyze ZIP files (new)
            val zipFiles = findAllZipFiles(context)
            val zipFilesSize = zipFiles.sumOf { it.length() }
            result["zip_files"] = zipFilesSize
            
            // Log the results
            var totalSize = 0L
            result.forEach { (category, size) ->
                totalSize += size
                Log.d(TAG, "Storage category: $category, size: ${size/1024} KB")
            }
            
            Log.d(TAG, "Total storage used: ${totalSize/1024} KB")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing storage", e)
        }
        
        return@withContext result
    }
    
    /**
     * Clean up residual files to free up storage space
     * 
     * @param context Application context
     * @return Message with results of cleanup
     */
    suspend fun cleanupResidualFiles(context: Context): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting residual file cleanup...")
            
            // Log storage breakdown before cleanup
            logStorageBreakdown(context)
            
            var totalFilesDeleted = 0
            var totalSpaceFreed = 0L
            
            // 1. Clean up temp files
            val tempFilesDeleted = cleanupTempFiles(context)
            totalFilesDeleted += tempFilesDeleted
            
            // 2. Clean up cache files that are older than 7 days
            val cacheFilesDeleted = cleanupOldCacheFiles(context)
            totalFilesDeleted += cacheFilesDeleted.first
            totalSpaceFreed += cacheFilesDeleted.second
            
            // 3. Clean up residual backup fragments
            val backupFragmentsDeleted = cleanupBackupFragments(context)
            totalFilesDeleted += backupFragmentsDeleted.first
            totalSpaceFreed += backupFragmentsDeleted.second
            
            // 4. Clean up orphaned database journal files
            val journalFilesDeleted = cleanupDatabaseJournalFiles(context)
            totalFilesDeleted += journalFilesDeleted.first
            totalSpaceFreed += journalFilesDeleted.second
            
            // 5. Clean up duplicate files
            val duplicatesDeleted = cleanupDuplicateFiles(context)
            totalFilesDeleted += duplicatesDeleted.first
            totalSpaceFreed += duplicatesDeleted.second
            
            // 6. Clean up old backup ZIP files (keep only the most recent 3)
            val oldBackupsDeleted = cleanupOldBackups(context)
            totalFilesDeleted += oldBackupsDeleted.first
            totalSpaceFreed += oldBackupsDeleted.second
            
            // 7. Check for orphaned or hidden large files
            val hiddenFilesDeleted = cleanupHiddenLargeFiles(context)
            totalFilesDeleted += hiddenFilesDeleted.first
            totalSpaceFreed += hiddenFilesDeleted.second
            
            // 8. Check parent directories for unexpected large files
            val unexpectedFilesDeleted = cleanupUnexpectedLargeFiles(context)
            totalFilesDeleted += unexpectedFilesDeleted.first
            totalSpaceFreed += unexpectedFilesDeleted.second
            
            // 9. Vacuum the database
            vacuumDatabase(context)
            
            // Log storage breakdown after cleanup
            Log.d(TAG, "Storage breakdown after cleanup:")
            logStorageBreakdown(context)
            
            return@withContext "Cleanup completed successfully!\n\n" +
                   "• ${totalFilesDeleted} files deleted\n" +
                   "• ${totalSpaceFreed / 1024 / 1024} MB space freed\n\n" +
                   "Run the 'Analyze Storage Usage' function to see the current storage usage."
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up residual files", e)
            return@withContext "Error cleaning up residual files: ${e.message}"
        }
    }
    
    /**
     * Clean up old backup ZIP files, keeping only the most recent ones
     * 
     * @param context Application context
     * @return Pair of (number of files deleted, bytes freed)
     */
    private fun cleanupOldBackups(context: Context): Pair<Int, Long> {
        var filesDeleted = 0
        var spaceFreed = 0L
        
        try {
            // Find all ZIP files
            val backupFiles = findAllZipFiles(context).filter { 
                it.name.startsWith(BACKUP_PREFIX) 
            }.sortedByDescending { 
                it.lastModified() 
            }
            
            // Keep only the most recent 3 backups
            if (backupFiles.size > 3) {
                backupFiles.drop(3).forEach { file ->
                    val fileSize = file.length()
                    if (file.delete()) {
                        filesDeleted++
                        spaceFreed += fileSize
                        Log.d(TAG, "Deleted old backup: ${file.absolutePath}, size: $fileSize bytes")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old backups", e)
        }
        
        return Pair(filesDeleted, spaceFreed)
    }

    /**
     * Find and cleanup hidden large files that may be consuming storage
     * This targets files over 10MB that may be orphaned or unused
     * 
     * @param context Application context
     * @return Pair of (number of files deleted, bytes freed)
     */
    private fun cleanupHiddenLargeFiles(context: Context): Pair<Int, Long> {
        var filesDeleted = 0
        var spaceFreed = 0L
        val MIN_FILE_SIZE = 10 * 1024 * 1024L // 10MB
        
        try {
            // Get all files in app directories
            val allFiles = findAllFiles(context)
            
            // Filter for large files
            val largeFiles = allFiles.filter { 
                it.length() > MIN_FILE_SIZE && 
                !it.name.endsWith(".db") && // Keep database files
                !it.name.contains("backup") && // Don't delete current backups here
                !it.path.contains("images") // Don't delete user images
            }
            
            // Log large files found
            if (largeFiles.isNotEmpty()) {
                Log.d(TAG, "Found ${largeFiles.size} large files (>10MB):")
                largeFiles.forEach { 
                    Log.d(TAG, "Large file: ${it.absolutePath}, size: ${it.length() / (1024 * 1024)} MB") 
                }
            }
            
            // Delete large hidden files (files starting with . or in hidden directories)
            largeFiles.filter { 
                it.name.startsWith(".") || it.path.contains("/.")
            }.forEach { file ->
                val fileSize = file.length()
                if (file.delete()) {
                    filesDeleted++
                    spaceFreed += fileSize
                    Log.d(TAG, "Deleted hidden large file: ${file.absolutePath}, size: ${fileSize / (1024 * 1024)} MB")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up hidden large files", e)
        }
        
        return Pair(filesDeleted, spaceFreed)
    }
    
    /**
     * Check for unexpected large files in parent directories
     * This targets files that might be in directories above the app's directories
     * 
     * @param context Application context
     * @return Pair of (number of files deleted, bytes freed)
     */
    private fun cleanupUnexpectedLargeFiles(context: Context): Pair<Int, Long> {
        var filesDeleted = 0
        var spaceFreed = 0L
        val MIN_FILE_SIZE = 100 * 1024 * 1024L // 100MB
        
        try {
            // Check parent directories for unexpected large files
            // App's files directory
            val filesDir = context.filesDir
            val parentDir = filesDir.parentFile
            
            if (parentDir != null && parentDir.exists()) {
                parentDir.listFiles()?.forEach { file ->
                    // Check for large files that aren't our database or normal app directories
                    if (file.isFile && 
                        file.length() > MIN_FILE_SIZE &&
                        file.name != "databases" && 
                        file.name != "shared_prefs" &&
                        file.name != "files" &&
                        file.name != "code_cache" &&
                        !file.name.endsWith(".db")) {
                        
                        Log.d(TAG, "Found unexpected large file in parent directory: ${file.absolutePath}, size: ${file.length() / (1024 * 1024)} MB")
                        
                        // Don't auto-delete these files, just report them
                        // We'll leave it to manual deletion to avoid removing critical files
                    }
                }
            }
            
            // Look for specific problem files identified in system storage
            // This checks if a file named "app_old" exists in common locations
            val appOldDirs = listOf(
                File(context.filesDir.parentFile?.parentFile, "app_old"),
                File(context.filesDir.parentFile, "app_old"),
                File(context.cacheDir.parentFile?.parentFile, "app_old")
            )
            
            appOldDirs.forEach { dir ->
                if (dir.exists()) {
                    val dirSize = calculateDirectorySize(dir)
                    if (dirSize > MIN_FILE_SIZE) {
                        Log.d(TAG, "Found large app_old directory: ${dir.absolutePath}, size: ${dirSize / (1024 * 1024)} MB")
                        
                        // Ask if we have permission to delete this
                        val canDelete = dir.absolutePath.contains(context.packageName)
                        
                        if (canDelete) {
                            var deletedSize = 0L
                            var deletedCount = 0
                            
                            try {
                                // Delete files inside first
                                dir.walkTopDown().filter { it.isFile }.forEach { file ->
                                    val fileSize = file.length()
                                    if (file.delete()) {
                                        deletedSize += fileSize
                                        deletedCount++
                                    }
                                }
                                
                                // Then try to delete directories bottom-up
                                dir.walkBottomUp().filter { it.isDirectory }.forEach { it.delete() }
                                
                                // Finally delete the root directory
                                if (dir.delete()) {
                                    Log.d(TAG, "Successfully deleted app_old directory")
                                } else {
                                    Log.d(TAG, "Deleted $deletedCount files (${deletedSize / (1024 * 1024)} MB) but couldn't delete root directory")
                                }
                                
                                filesDeleted += deletedCount
                                spaceFreed += deletedSize
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deleting app_old directory", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for unexpected large files", e)
        }
        
        return Pair(filesDeleted, spaceFreed)
    }

    /**
     * Calculate the size of a directory and its subdirectories
     * 
     * @param directory Directory to calculate size for
     * @return Size in bytes
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        
        if (directory.exists()) {
            try {
                directory.walkTopDown().filter { it.isFile }.forEach { file ->
                    size += file.length()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating directory size for ${directory.absolutePath}", e)
            }
        }
        
        return size
    }
    
    /**
     * Calculate the size of the database files
     * 
     * @param context Application context
     * @return Size in bytes
     */
    private fun calculateDatabaseSize(context: Context): Long {
        var size = 0L
        val dbDir = context.getDatabasePath(AppDatabase.DATABASE_NAME).parentFile
        
        if (dbDir != null && dbDir.exists() && dbDir.isDirectory) {
            dbDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".db") || file.name.endsWith(".sqlite")) {
                    size += file.length()
                }
            }
        }
        
        // Also check the direct database path
        val databasePath = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        if (databasePath.exists()) {
            size += databasePath.length()
        }
        
        return size
    }
    
    /**
     * Calculate the size of shared preferences
     * 
     * @param context Application context
     * @return Size in bytes
     */
    private fun calculatePreferencesSize(context: Context): Long {
        var size = 0L
        val prefsDir = File(context.dataDir, "shared_prefs")
        
        if (prefsDir.exists() && prefsDir.isDirectory) {
            prefsDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".xml")) {
                    size += file.length()
                }
            }
        }
        
        return size
    }
    
    /**
     * Calculate the size of receipt images
     * 
     * @param context Application context
     * @return Size in bytes
     */
    private fun calculateReceiptImagesSize(context: Context): Long {
        var size = 0L
        val imagesDir = File(context.getExternalFilesDir(null), "images")
        
        if (imagesDir.exists() && imagesDir.isDirectory) {
            val receiptDir = File(imagesDir, "receipts")
            if (receiptDir.exists()) {
                size = calculateDirectorySize(receiptDir)
            }
        }
        
        return size
    }
    
    /**
     * Calculate the size of temporary files
     * 
     * @param context Application context
     * @return Size in bytes
     */
    private fun calculateTempFilesSize(context: Context): Long {
        var size = 0L
        
        try {
            // Check cache directory for temp files
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.contains("temp") || 
                    file.name.endsWith(".tmp") || 
                    file.name.endsWith(".temp")) {
                    size += file.length()
                }
            }
            
            // Check files directory for temp files
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.contains("temp") || 
                    file.name.endsWith(".tmp") || 
                    file.name.endsWith(".temp")) {
                    size += file.length()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating temp files size", e)
        }
        
        return size
    }
    
    /**
     * Calculate the size of residual files that might be causing storage bloat
     * 
     * @param context Application context
     * @return Size in bytes
     */
    private fun calculateResidualFilesSize(context: Context): Long {
        var size = 0L
        
        try {
            // Check for database journal files
            val dbDir = context.getDatabasePath(AppDatabase.DATABASE_NAME).parentFile
            if (dbDir != null && dbDir.exists() && dbDir.isDirectory) {
                dbDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith("-journal") || 
                        file.name.endsWith("-wal") || 
                        file.name.endsWith("-shm")) {
                        size += file.length()
                    }
                }
            }
            
            // Check for old backup fragments
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.contains("backup_fragment") || 
                    file.name.contains("partial_backup")) {
                    size += file.length()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating residual files size", e)
        }
        
        return size
    }
    
    /**
     * Calculate the size of backup files
     * 
     * @param context Application context
     * @return Size in bytes
     */
    private fun calculateBackupFilesSize(context: Context): Long {
        var size = 0L
        
        try {
            // Get all ZIP files
            val zipFiles = findAllZipFiles(context)
            size = zipFiles.sumOf { it.length() }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating backup files size", e)
        }
        
        return size
    }
    
    /**
     * Clean up old cache files (older than 7 days)
     * 
     * @param context Application context
     * @return Pair of (number of files deleted, bytes freed)
     */
    private fun cleanupOldCacheFiles(context: Context): Pair<Int, Long> {
        var filesDeleted = 0
        var spaceFreed = 0L
        val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days ago
        
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    val fileSize = file.length()
                    if (file.delete()) {
                        filesDeleted++
                        spaceFreed += fileSize
                        Log.d(TAG, "Deleted old cache file: ${file.absolutePath}, size: $fileSize bytes")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old cache files", e)
        }
        
        return Pair(filesDeleted, spaceFreed)
    }
    
    /**
     * Clean up backup fragments that might be left from failed backups
     * 
     * @param context Application context
     * @return Pair of (number of files deleted, bytes freed)
     */
    private fun cleanupBackupFragments(context: Context): Pair<Int, Long> {
        var filesDeleted = 0
        var spaceFreed = 0L
        
        try {
            // Check files directory for backup fragments
            context.filesDir.walkTopDown().filter { file ->
                file.isFile && (
                    file.name.contains("backup_fragment") || 
                    file.name.contains("partial_backup") ||
                    file.name.contains("temp_backup"))
            }.forEach { file ->
                val fileSize = file.length()
                if (file.delete()) {
                    filesDeleted++
                    spaceFreed += fileSize
                    Log.d(TAG, "Deleted backup fragment: ${file.absolutePath}, size: $fileSize bytes")
                }
            }
            
            // Check cache directory as well
            context.cacheDir.walkTopDown().filter { file ->
                file.isFile && (
                    file.name.contains("backup_fragment") || 
                    file.name.contains("partial_backup") ||
                    file.name.contains("temp_backup"))
            }.forEach { file ->
                val fileSize = file.length()
                if (file.delete()) {
                    filesDeleted++
                    spaceFreed += fileSize
                    Log.d(TAG, "Deleted backup fragment from cache: ${file.absolutePath}, size: $fileSize bytes")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up backup fragments", e)
        }
        
        return Pair(filesDeleted, spaceFreed)
    }
    
    /**
     * Clean up orphaned database journal files
     * 
     * @param context Application context
     * @return Pair of (number of files deleted, bytes freed)
     */
    private fun cleanupDatabaseJournalFiles(context: Context): Pair<Int, Long> {
        var filesDeleted = 0
        var spaceFreed = 0L
        
        try {
            val dbDir = context.getDatabasePath(AppDatabase.DATABASE_NAME).parentFile
            if (dbDir != null && dbDir.exists() && dbDir.isDirectory) {
                dbDir.listFiles()?.forEach { file ->
                    if (file.isFile && (
                        file.name.endsWith("-journal") || 
                        file.name.endsWith("-wal") || 
                        file.name.endsWith("-shm"))) {
                        
                        val fileSize = file.length()
                        if (file.delete()) {
                            filesDeleted++
                            spaceFreed += fileSize
                            Log.d(TAG, "Deleted database journal file: ${file.absolutePath}, size: $fileSize bytes")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up database journal files", e)
        }
        
        return Pair(filesDeleted, spaceFreed)
    }
    
    /**
     * Clean up duplicate files by comparing content hashes
     * 
     * @param context Application context
     * @return Pair of (number of files deleted, bytes freed)
     */
    private fun cleanupDuplicateFiles(context: Context): Pair<Int, Long> {
        var filesDeleted = 0
        var spaceFreed = 0L
        
        try {
            // Keep track of file hashes to identify duplicates
            val fileHashes = mutableMapOf<String, File>()
            
            // Check files directory for duplicates
            val filesDirResult = findDuplicatesInDirectory(context.filesDir, fileHashes)
            filesDeleted += filesDirResult.first
            spaceFreed += filesDirResult.second
            
            // Check cache directory for duplicates
            val cacheDirResult = findDuplicatesInDirectory(context.cacheDir, fileHashes)
            filesDeleted += cacheDirResult.first
            spaceFreed += cacheDirResult.second
            
            // Check external files directory for duplicates
            context.getExternalFilesDir(null)?.let { externalDir ->
                val externalDirResult = findDuplicatesInDirectory(externalDir, fileHashes)
                filesDeleted += externalDirResult.first
                spaceFreed += externalDirResult.second
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up duplicate files", e)
        }
        
        return Pair(filesDeleted, spaceFreed)
    }
    
    /**
     * Find duplicates in a directory based on content hash
     * 
     * @param directory Directory to check
     * @param fileHashes Map of file hashes to original files
     * @return Pair of (number of files deleted, bytes freed)
     */
    private fun findDuplicatesInDirectory(directory: File, fileHashes: MutableMap<String, File>): Pair<Int, Long> {
        var filesDeleted = 0
        var spaceFreed = 0L
        
        try {
            directory.walkTopDown().filter { it.isFile && it.length() > 1024 }.forEach { file ->
                // Skip backup ZIP files
                if (!file.name.endsWith(".zip") && !shouldExcludeFromBackup(file)) {
                    val hash = getFileHash(file)
                    if (hash != null) {
                        if (fileHashes.containsKey(hash)) {
                            // This is a duplicate
                            val fileSize = file.length()
                            if (file.delete()) {
                                filesDeleted++
                                spaceFreed += fileSize
                                Log.d(TAG, "Deleted duplicate file: ${file.absolutePath}, size: $fileSize bytes")
                            }
                        } else {
                            fileHashes[hash] = file
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding duplicates in directory: ${directory.absolutePath}", e)
        }
        
        return Pair(filesDeleted, spaceFreed)
    }
    
    /**
     * Get a hash of a file's content
     * 
     * @param file File to hash
     * @return MD5 hash of the file content, or null if there was an error
     */
    private fun getFileHash(file: File): String? {
        try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            file.inputStream().use { inputStream ->
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            
            val digest = md.digest()
            return digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file hash for ${file.absolutePath}", e)
            return null
        }
    }
    
    /**
     * Vacuum the database to reclaim space
     * 
     * @param context Application context
     */
    private fun vacuumDatabase(context: Context) {
        try {
            // Use the database name from AppDatabase
            val db = context.openOrCreateDatabase(AppDatabase.DATABASE_NAME, Context.MODE_PRIVATE, null)
            db.execSQL("VACUUM")
            db.close()
            Log.d(TAG, "Database vacuum completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error vacuuming database", e)
        }
    }

    /**
     * Helper function to check if a file is an image file
     */
    private fun isImageFile(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
    }

    /**
     * Helper function to check if a file is a PDF file
     */
    private fun isPdfFile(file: File): Boolean {
        return file.name.lowercase().endsWith(".pdf")
    }

    /**
     * Helper function to get the app version
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "Unknown" // Handle null case explicitly
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Determines if a file should be excluded from backup
     */
    private fun shouldExcludeFromBackup(file: File): Boolean {
        // Skip system files, temp files, and cache files
        val fileName = file.name.lowercase()
        
        // Skip hidden files and directories
        if (fileName.startsWith(".")) {
            return true
        }
        
        // Skip temporary files
        if (fileName.endsWith(".tmp") || 
            fileName.endsWith(".temp") ||
            fileName.contains("temp_backup_") ||
            fileName.endsWith(".bak")) {
            return true
        }
        
        // Skip journal files that are recreated
        if (fileName.endsWith("-journal") || 
            fileName.endsWith("-wal") ||
            fileName.endsWith("-shm") ||
            fileName.endsWith(".log") ||
            fileName.endsWith(".old")) {
            return true
        }

        // Skip backup files and folders to prevent recursive backup
        if (fileName.endsWith(".zip") ||
            fileName.contains("backup") ||
            fileName.contains("restore")) {
            return true
        }
        
        // Skip system and cache directories
        if (file.isDirectory) {
            if (fileName == "cache" || 
                fileName == "tmp" || 
                fileName == "temp" ||
                fileName == "thumbnails" ||
                fileName == "logs" ||
                fileName == "crash" ||
                fileName.contains("cached") ||
                fileName.contains("backup")) {
                return true
            }
        }
        
        return false
    }

    /**
     * Helper function to safely get a DocumentFile from a URI with proper error handling
     *
     * @param context Application context
     * @param uri URI to resolve to a DocumentFile
     * @return DocumentFile or null if there was an error
     */
    private fun safeGetDocumentFile(context: Context, uri: Uri): DocumentFile? {
        return try {
            // Check if we have persistable URI permissions
            var hasPermission = false
            for (uriPermission in context.contentResolver.persistedUriPermissions) {
                if (uriPermission.uri == uri && 
                    (uriPermission.isWritePermission || uriPermission.isReadPermission)) {
                    hasPermission = true
                    break
                }
            }
            
            if (!hasPermission) {
                Log.w(TAG, "No persistable URI permission found for $uri")
                return null
            }
            
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            if (documentFile == null || !documentFile.exists()) {
                Log.e(TAG, "Document file is null or does not exist for URI: $uri")
                return null
            }
            
            documentFile
        } catch (e: Exception) {
            Log.e(TAG, "Error getting DocumentFile from URI: $uri", e)
            null
        }
    }
} 