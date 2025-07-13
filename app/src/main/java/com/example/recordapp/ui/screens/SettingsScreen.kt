package com.example.recordapp.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.recordapp.R
import com.example.recordapp.model.GridSize
import com.example.recordapp.repository.ExpenseRepository
import com.example.recordapp.ui.components.EnhancedSettingsItem
import com.example.recordapp.ui.components.ExpandableSettingsSection
import com.example.recordapp.ui.navigation.Screen
import com.example.recordapp.util.AppImageLoader
import com.example.recordapp.util.BackupModule
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.viewmodel.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel? = null
) {
    val context = LocalContext.current
    val settings = SettingsManager.getInstance(context)
    
    // Coroutine scope for async operations
    val scope = rememberCoroutineScope()
    
    // Get current user if available
    val currentUser = authViewModel?.currentUser?.collectAsState()
    
    // Get app version information safely
    val appVersion = remember { mutableStateOf("RecordApp 1.0.0") }
    
    LaunchedEffect(Unit) {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            
            // Updated version code handling to avoid deprecation warning
            @Suppress("DEPRECATION")
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }
            
            appVersion.value = "RecordApp ${packageInfo.versionName} ($versionCode)"
        } catch (e: Exception) {
            // Keep default version
        }
    }
    
    // State for dialog controls - removed sort and date format dialogs
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    // State for settings
    var currencySymbol by remember { mutableStateOf(settings.currencySymbol) }
    var defaultGrid by remember { mutableStateOf(getGridSizeFromString(settings.defaultGridSize)) }
    var pdfQuality by remember { mutableFloatStateOf(settings.pdfQuality.toFloat()) }
    var storageLocation by remember { mutableStateOf(settings.storageLocation) }
    var notificationsEnabled by remember { mutableStateOf(settings.notificationsEnabled) }
    
    // ZIP storage settings - with default values
    var useZipStorage by remember { mutableStateOf(false) }
    var autoArchive by remember { mutableStateOf(false) }
    var archiveNaming by remember { mutableStateOf("date") }
    
    // Try to read ZIP storage settings if they exist
    LaunchedEffect(Unit) {
        try {
            useZipStorage = settings.javaClass.getDeclaredField("useZipStorage")
                .apply { isAccessible = true }.get(settings) as Boolean
        } catch (e: Exception) {
            Log.d("SettingsScreen", "useZipStorage property not found", e)
        }
        
        try {
            autoArchive = settings.javaClass.getDeclaredField("autoArchive")
                .apply { isAccessible = true }.get(settings) as Boolean
        } catch (e: Exception) {
            Log.d("SettingsScreen", "autoArchive property not found", e)
        }
        
        try {
            archiveNaming = settings.javaClass.getDeclaredField("archiveNaming")
                .apply { isAccessible = true }.get(settings) as String
        } catch (e: Exception) {
            Log.d("SettingsScreen", "archiveNaming property not found", e)
        }
    }
    
    // Backup state
    var backupInProgress by remember { mutableStateOf(false) }
    var restoreInProgress by remember { mutableStateOf(false) }
    var showBackupResultDialog by remember { mutableStateOf(false) }
    var backupResultMessage by remember { mutableStateOf("") }
    var backupProgress by remember { mutableFloatStateOf(0f) }
    var restoreProgress by remember { mutableFloatStateOf(0f) }
    var showBackupLocationDialog by remember { mutableStateOf(false) }
    var customBackupLocation by remember { mutableStateOf<Uri?>(null) }
    
    // For backup location selection
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                // Take persistent permissions to access this folder
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                
                // Take persistable URI permission to ensure we can access this folder in the future
                context.contentResolver.takePersistableUriPermission(uri, flags)
                
                // Save the URI in settings
                val settings = SettingsManager.getInstance(context)
                settings.customBackupFolderUri = uri.toString()
                
                // Log the URI for debugging
                Log.d("SettingsScreen", "Selected backup folder URI: $uri")
                
                // Verify we have the permissions
                var hasPermission = false
                for (uriPermission in context.contentResolver.persistedUriPermissions) {
                    if (uriPermission.uri == uri && 
                        uriPermission.isReadPermission && uriPermission.isWritePermission) {
                        hasPermission = true
                        break
                    }
                }
                
                if (hasPermission) {
                    // Show confirmation
                    Toast.makeText(
                        context,
                        "Backup folder selected successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Failed to persist permissions for the selected folder",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: SecurityException) {
                Log.e("SettingsScreen", "Error persisting folder URI permission", e)
                Toast.makeText(
                    context,
                    "Error: Unable to get permission for the selected folder",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e("SettingsScreen", "Error saving backup folder", e)
                Toast.makeText(
                    context,
                    "Error saving backup folder: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // Document picker for restoring from backup
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Mark restore as in progress
            restoreInProgress = true
            restoreProgress = 0f
            
            // Process on background thread
            scope.launch {
                val result = BackupModule.restoreFromBackup(
                    context, 
                    uri,
                    onProgressUpdate = { progress ->
                        // Update the progress on the UI thread
                        launch(Dispatchers.Main) {
                            restoreProgress = progress
                        }
                    }
                )
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    // Hide loading indicator
                    restoreInProgress = false
                    
                    // If it's a duplicate backup, show a different message
                    if (result.isDuplicate) {
                        Toast.makeText(
                            context,
                            "This backup has already been restored. No changes detected.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@withContext
                    }
                    
                    // Show result message for non-duplicate backups
                    Toast.makeText(
                        context,
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // If restore was successful, restart app
                    if (result.success) {
                        // Add delay to ensure the toast is visible
                        delay(2000)
                        
                        // Restart the app
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        context.startActivity(intent)
                        
                        // Force system-level restart to ensure clean state
                        Process.killProcess(Process.myPid())
                    }
                }
            }
        }
    }
    
    // Set default date format to DD/MM/YYYY
    LaunchedEffect(Unit) {
        settings.dateFormat = "dd/MM/yyyy"
    }
    
    // Storage analysis state
    var showStorageAnalysisDialog by remember { mutableStateOf(false) }
    var isAnalyzingStorage by remember { mutableStateOf(false) }
    var storageAnalysisResult by remember { mutableStateOf<Map<String, Long>?>(null) }
    var totalStorageUsed by remember { mutableStateOf(0L) }
    var isCleaningStorage by remember { mutableStateOf(false) }
    var cleanupResult by remember { mutableStateOf("") }
    var showCleanupResultDialog by remember { mutableStateOf(false) }
    
    // Map accent color to Color resource
    val accentColorResource = R.color.accent_blue
    
    Scaffold(
        topBar = {
            // Use custom top bar with minimal height
            Box(modifier = Modifier.height(0.dp))
        }
    ) { paddingValues ->
        // Show loading indicator during backup or restore operations
        if (backupInProgress || restoreInProgress) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (backupInProgress) "Creating backup..." else "Restoring backup...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // Show progress percentage when backup or restore is in progress
                    if (backupInProgress || restoreInProgress) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (backupInProgress) 
                                "${(backupProgress * 100).toInt()}%" 
                            else 
                                "${(restoreProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { if (backupInProgress) backupProgress else restoreProgress },
                            modifier = Modifier.width(240.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Custom title bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(accentColorResource))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        
                        // Title
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                        
                        // User profile action in top bar
                        if (authViewModel != null && currentUser != null) {
                            IconButton(
                                onClick = { navController.navigate(Screen.Profile.route) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // Application settings section
                    ExpandableSettingsSection(
                        title = "Application Settings",
                        icon = Icons.Default.Settings,
                        iconTint = colorResource(accentColorResource),
                        initiallyExpanded = true
                    ) {
                        // Currency setting
                        EnhancedSettingsItem(
                            title = "Currency Symbol",
                            description = "Change the currency symbol used throughout the app",
                            icon = Icons.Default.AttachMoney,
                            value = currencySymbol,
                            onClick = { showCurrencyDialog = true },
                            iconTint = colorResource(accentColorResource)
                        )
                        
                        // Grid size setting
                        EnhancedSettingsItem(
                            title = "Default Grid Size",
                            description = "Change the default grid size for image galleries",
                            icon = Icons.Default.GridView,
                            value = getGridSizeDisplayName(defaultGrid),
                            onClick = { showGridDialog = true },
                            iconTint = colorResource(accentColorResource)
                        )
                        
                        // Enhanced Notifications toggle with extra padding
                        EnhancedSettingsItem(
                            title = "Notifications",
                            description = "Enable or disable app notifications",
                            icon = Icons.Default.Notifications,
                            iconTint = colorResource(R.color.accent_red),
                            isSwitch = true,
                            isSwitchEnabled = notificationsEnabled,
                            onClick = { /* Handled by switch */ },
                            onSwitchChange = { newValue ->
                                notificationsEnabled = newValue
                                settings.notificationsEnabled = newValue
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    // Export settings - simplified
                    ExpandableSettingsSection(
                        title = "Export Settings",
                        icon = Icons.Default.PictureAsPdf,
                        iconTint = colorResource(R.color.accent_red)
                    ) {
                        // PDF Quality slider
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "PDF Quality",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = "Adjust the quality of exported PDF files",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Low",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                
                                Slider(
                                    value = pdfQuality,
                                    onValueChange = { 
                                        pdfQuality = it
                                        settings.pdfQuality = it.toInt()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = colorResource(R.color.accent_red),
                                        activeTrackColor = colorResource(R.color.accent_red).copy(alpha = 0.5f)
                                    )
                                )
                                
                                Text(
                                    text = "High",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        
                        // Quick Export Options
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // PDF Export Button
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                                    .clickable { 
                                        Toast.makeText(context, "Exporting PDF...", Toast.LENGTH_SHORT).show()
                                        // Simplified approach without actual PDF export
                                        Toast.makeText(context, "PDF export functionality coming soon", Toast.LENGTH_LONG).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Export PDF",
                                        tint = colorResource(R.color.accent_red)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Export PDF",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            
                            // CSV Export Button
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                                    .clickable { 
                                        Toast.makeText(context, "Exporting CSV...", Toast.LENGTH_SHORT).show()
                                        // Simplified approach without actual CSV export
                                        Toast.makeText(context, "CSV export functionality coming soon", Toast.LENGTH_LONG).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TableView,
                                        contentDescription = "Export CSV",
                                        tint = colorResource(R.color.accent_green)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Export CSV",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    
                    // Storage settings
                    ExpandableSettingsSection(
                        title = "Storage",
                        icon = Icons.Default.Storage,
                        iconTint = colorResource(R.color.accent_orange)
                    ) {
                        // Clear data button
                        EnhancedSettingsItem(
                            title = "Clear Data",
                            description = "Delete all app data and reset settings",
                            icon = Icons.Default.DeleteForever,
                            iconTint = colorResource(R.color.accent_red),
                            onClick = { showClearDataDialog = true },
                            isDestructive = true
                        )
                    }
                    
                    // Backup & Restore Section
                    ExpandableSettingsSection(
                        title = "Backup & Restore",
                        icon = Icons.Default.Backup,
                        iconTint = colorResource(R.color.accent_green)
                    ) {
                        // Backup location setting
                        val customBackupFolderUri = remember { mutableStateOf(settings.customBackupFolderUri) }
                        
                        EnhancedSettingsItem(
                            title = "Backup Location",
                            description = if (customBackupFolderUri.value.isEmpty()) "Select a folder to store your backups" else "Tap to change backup folder",
                            value = if (customBackupFolderUri.value.isEmpty()) "Not selected" else "Selected",
                            icon = Icons.Default.Folder,
                            iconTint = colorResource(R.color.accent_blue),
                            onClick = { 
                                try {
                                    folderPickerLauncher.launch(null)
                                } catch (e: Exception) {
                                    Log.e("SettingsScreen", "Error launching folder picker", e)
                                    Toast.makeText(
                                        context,
                                        "Error launching folder picker: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                        
                        // Auto backup toggle
                        var autoBackupEnabled by remember { mutableStateOf(settings.autoBackupEnabled) }
                        
                        EnhancedSettingsItem(
                            title = "Auto Backup",
                            description = "Automatically backup your data",
                            icon = Icons.Default.Schedule,
                            iconTint = colorResource(R.color.accent_green),
                            isSwitch = true,
                            isSwitchEnabled = autoBackupEnabled,
                            onClick = { /* Handled by switch */ },
                            onSwitchChange = { newValue ->
                                autoBackupEnabled = newValue
                                settings.autoBackupEnabled = newValue
                                
                                // Update the backup schedule
                                scope.launch {
                                    BackupModule.scheduleBackup(
                                        context = context,
                                        isEnabled = newValue,
                                        frequency = settings.backupFrequency
                                    )
                                }
                            }
                        )
                        
                        // Backup frequency selector - show only when auto backup is enabled
                        if (autoBackupEnabled) {
                            var backupFrequency by remember { mutableStateOf(settings.backupFrequency) }
                            
                            EnhancedSettingsItem(
                                title = "Backup Frequency",
                                description = "How often to perform automatic backups",
                                value = when (backupFrequency) {
                                    "daily" -> "Daily"
                                    "weekly" -> "Weekly"
                                    "monthly" -> "Monthly"
                                    else -> "Weekly"
                                },
                                icon = Icons.Default.Refresh,
                                iconTint = colorResource(R.color.accent_green),
                                onClick = {
                                    // Cycle through options
                                    backupFrequency = when (backupFrequency) {
                                        "daily" -> "weekly"
                                        "weekly" -> "monthly"
                                        "monthly" -> "daily"
                                        else -> "weekly"
                                    }
                                    
                                    // Save setting
                                    settings.backupFrequency = backupFrequency
                                    
                                    // Reschedule backup with updated frequency
                                    scope.launch {
                                        BackupModule.scheduleBackup(
                                            context = context,
                                            isEnabled = true,
                                            frequency = backupFrequency
                                        )
                                    }
                                    
                                    Toast.makeText(
                                        context,
                                        "Backup frequency set to ${backupFrequency.replaceFirstChar { it.uppercase() }}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                        
                        // Create Backup button
                        EnhancedSettingsItem(
                            title = "Backup Now",
                            description = if (customBackupFolderUri.value.isEmpty()) 
                                "Select a backup location first" 
                            else 
                                "Create a backup of all your data",
                            icon = Icons.Default.Save,
                            iconTint = colorResource(R.color.accent_blue),
                            enabled = !backupInProgress && !restoreInProgress && customBackupFolderUri.value.isNotEmpty(),
                            onClick = {
                                if (customBackupFolderUri.value.isEmpty()) {
                                    // Prompt to select a backup location first
                                    Toast.makeText(
                                        context,
                                        "Please select a backup location first",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    // Start backup process with progress tracking
                                    backupInProgress = true
                                    backupProgress = 0f
                                    
                                    scope.launch {
                                        val result = BackupModule.createBackupWithProgress(
                                            context = context,
                                            onProgressUpdate = { progress ->
                                                // Update progress on the UI thread
                                                launch(Dispatchers.Main) {
                                                    backupProgress = progress
                                                }
                                            }
                                        )
                                        
                                        // Update UI on main thread
                                        withContext(Dispatchers.Main) {
                                            backupInProgress = false
                                            
                                            // Show the result
                                            backupResultMessage = result.message
                                            showBackupResultDialog = true
                                        }
                                    }
                                }
                            }
                        )
                        
                        // Restore backup button
                        EnhancedSettingsItem(
                            title = "Restore from Backup",
                            description = "Restore your data from a backup file",
                            icon = Icons.Default.Restore,
                            iconTint = colorResource(R.color.accent_purple),
                            enabled = !backupInProgress && !restoreInProgress,
                            onClick = { 
                                // Launch file picker to select backup file
                                restoreFileLauncher.launch("application/zip")
                            }
                        )
                    }
                    
                    // Storage Management section
                    ExpandableSettingsSection(
                        title = "Storage Management",
                        icon = Icons.Default.Storage,
                        iconTint = MaterialTheme.colorScheme.tertiary
                    ) {
                        // Storage Analysis Button
                        EnhancedSettingsItem(
                            title = "Analyze Storage Usage",
                            description = "Identify what's using the most space in the app",
                            icon = Icons.Default.DataUsage,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                isAnalyzingStorage = true
                                scope.launch {
                                    try {
                                        // Run the storage analysis
                                        val analysis = BackupModule.analyzeStorageUsage(context)
                                        
                                        // Calculate total size
                                        var totalSize = 0L
                                        analysis.values.forEach { size ->
                                            totalSize += size
                                        }
                                        
                                        withContext(Dispatchers.Main) {
                                            storageAnalysisResult = analysis
                                            totalStorageUsed = totalSize
                                            isAnalyzingStorage = false
                                            showStorageAnalysisDialog = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SettingsScreen", "Error analyzing storage", e)
                                        withContext(Dispatchers.Main) {
                                            isAnalyzingStorage = false
                                            Toast.makeText(
                                                context, 
                                                "Error analyzing storage: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                        
                        // Cleanup Storage Button
                        EnhancedSettingsItem(
                            title = "Clean Up Temporary Files",
                            description = "Remove residual and temporary files to free up space",
                            icon = Icons.Default.CleaningServices,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            onClick = {
                                isCleaningStorage = true
                                scope.launch {
                                    try {
                                        // Run the cleanup process
                                        val result = BackupModule.cleanupResidualFiles(context)
                                        
                                        withContext(Dispatchers.Main) {
                                            isCleaningStorage = false
                                            cleanupResult = result
                                            showCleanupResultDialog = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SettingsScreen", "Error cleaning storage", e)
                                        withContext(Dispatchers.Main) {
                                            isCleaningStorage = false
                                            Toast.makeText(
                                                context, 
                                                "Error cleaning storage: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                    
                    // About section with app version
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = appVersion.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
    
    // Currency dialog
    if (showCurrencyDialog) {
        SimpleSelectionDialog(
            title = "Choose Currency Symbol",
            onDismiss = { showCurrencyDialog = false }
        ) {
            val currencies = listOf("$", "₹", "€", "£", "¥")
            
            currencies.forEach { symbol ->
                SimpleRadioOption(
                    text = symbol,
                    selected = currencySymbol == symbol,
                    onClick = {
                        currencySymbol = symbol
                        settings.currencySymbol = symbol
                        showCurrencyDialog = false
                    }
                )
            }
        }
    }
    
    // Grid size dialog
    if (showGridDialog) {
        SimpleSelectionDialog(
            title = "Default Grid Size",
            onDismiss = { showGridDialog = false }
        ) {
            // Use the actual GridSize enum values
            val gridSizes = listOf(
                GridSize.ONE_BY_ONE,
                GridSize.TWO_BY_TWO,
                GridSize.TWO_BY_THREE,
                GridSize.TWO_BY_FOUR
            )
            
            gridSizes.forEach { size ->
                SimpleRadioOption(
                    text = size.displayName,
                    selected = defaultGrid == size,
                    onClick = {
                        defaultGrid = size
                        settings.defaultGridSize = size.name
                        showGridDialog = false
                    }
                )
            }
        }
    }
    
    // Storage location dialog
    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("Storage Location") },
            text = { 
                Text(
                    "The app now uses a single backup location that you select using the system file picker. " +
                    "This ensures your backups are stored in a location of your choice and prevents duplicate backups."
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showStorageDialog = false
                        
                        // Launch the folder picker
                        try {
                            folderPickerLauncher.launch(null)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Error launching folder picker", e)
                            Toast.makeText(
                                context,
                                "Error launching folder picker: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("Select Backup Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Backup result dialog
    if (showBackupResultDialog) {
        AlertDialog(
            onDismissRequest = { showBackupResultDialog = false },
            title = { Text("Backup Complete") },
            text = { 
                Column {
                    Text(backupResultMessage)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your backup was saved to your selected backup folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBackupResultDialog = false }
                ) {
                    Text("OK")
                }
            }
        )
    }
    
    // Clear data confirmation dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data?") },
            text = { 
                Text(
                    "This will delete all your expenses, receipts, and reset all settings. This " +
                    "action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d("SettingsScreen", "Clear Data button clicked")
                        // Clear the database and settings, but preserve login credentials
                        scope.launch {
                            Log.d("SettingsScreen", "Beginning data clear process")
                            val expenseRepository = ExpenseRepository.getInstance(context)
                            
                            try {
                                // 1. Clear all expenses from the database
                                Log.d("SettingsScreen", "Step 1: Clearing expenses from database")
                                try {
                                    withContext(Dispatchers.IO) {
                                        expenseRepository.clearAllExpenses()
                                    }
                                    Log.d("SettingsScreen", "Database cleared successfully")
                                } catch (dbException: Exception) {
                                    Log.e("SettingsScreen", "Error clearing database", dbException)
                                    // Continue with other clearing operations even if database clearing failed
                                }
                                
                                // 2. Reset app settings but NOT auth prefs (they are in a different file)
                                Log.d("SettingsScreen", "Step 2: Resetting app settings")
                                settings.clearAll()
                                Log.d("SettingsScreen", "Settings cleared successfully")
                                
                                // 3. Clear image caches with fallback method
                                Log.d("SettingsScreen", "Step 3: Clearing image caches")
                                try {
                                    // Get the AppImageLoader instance
                                    val appImageLoader = AppImageLoader.getInstance(context)
                                    
                                    // Use direct coil ImageLoader methods
                                    @OptIn(coil.annotation.ExperimentalCoilApi::class)
                                    appImageLoader.memoryCache?.clear()
                                    
                                    @OptIn(coil.annotation.ExperimentalCoilApi::class)
                                    appImageLoader.diskCache?.clear()
                                    
                                    Log.d("SettingsScreen", "Image caches cleared successfully")
                                } catch (e: Exception) {
                                    // Fallback to manually clearing cache directories
                                    Log.e("SettingsScreen", "Error clearing image caches, using fallback method", e)
                                    val cacheDir = context.cacheDir
                                    if (cacheDir.exists()) {
                                        val imageCacheDir = File(cacheDir, "image_cache")
                                        if (imageCacheDir.exists() && imageCacheDir.isDirectory) {
                                            imageCacheDir.listFiles()?.forEach { file ->
                                                if (file.isFile) {
                                                    file.delete()
                                                }
                                            }
                                        }
                                    }
                                    Log.d("SettingsScreen", "Fallback cache clearing completed")
                                }
                                
                                // 4. Clear physical image files from storage
                                Log.d("SettingsScreen", "Step 4: Clearing physical image files")
                                try {
                                    val imagesBaseDir = File(context.getExternalFilesDir(null), "images")
                                    Log.d("SettingsScreen", "Images directory: ${imagesBaseDir.absolutePath}")
                                    
                                    if (imagesBaseDir.exists() && imagesBaseDir.isDirectory) {
                                        Log.d("SettingsScreen", "Images directory exists, deleting files")
                                        val folders = imagesBaseDir.listFiles()
                                        Log.d("SettingsScreen", "Found ${folders?.size ?: 0} folders")
                                        
                                        // Try both the folders approach and a direct file delete approach
                                        var filesDeleted = 0
                                        
                                        // Method 1: Delete files in folder structure
                                        folders?.forEach { folder ->
                                            if (folder.isDirectory) {
                                                val files = folder.listFiles()
                                                Log.d("SettingsScreen", "Folder ${folder.name} has ${files?.size ?: 0} files")
                                                
                                                files?.forEach { file ->
                                                    if (file.isFile) {
                                                        try {
                                                            val deleted = file.delete()
                                                            if (deleted) filesDeleted++
                                                            Log.d("SettingsScreen", "Deleted file ${file.name}: $deleted")
                                                        } catch (fileEx: Exception) {
                                                            Log.e("SettingsScreen", "Error deleting file ${file.name}", fileEx)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Method 2: Try direct traversal of all files (if Method 1 found no files)
                                        if (filesDeleted == 0) {
                                            Log.d("SettingsScreen", "Trying alternative file deletion method")
                                            // Compatible approach that doesn't require Java NIO Path API
                                            filesDeleted = deleteFilesRecursively(imagesBaseDir)
                                            Log.d("SettingsScreen", "Alternative deletion completed, files deleted: $filesDeleted")
                                        }
                                        
                                        Log.d("SettingsScreen", "Total files deleted: $filesDeleted")
                                    } else {
                                        Log.d("SettingsScreen", "Images directory does not exist or is not a directory")
                                    }
                                } catch (e: Exception) {
                                    Log.e("SettingsScreen", "Error while deleting image files", e)
                                    // Continue with rest of clearing process
                                }
                                
                                // 5. Add a delay to ensure operations complete
                                Log.d("SettingsScreen", "Adding delay for operations to complete")
                                delay(500)
                                
                                // 6. Show confirmation
                                withContext(Dispatchers.Main) {
                                    Log.d("SettingsScreen", "Showing success toast")
                                    
                                    // First dismiss the dialog
                                    showClearDataDialog = false
                                    
                                    // Show success message
                                    Toast.makeText(
                                        context,
                                        "All data has been cleared",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    
                                    try {
                                        // Force refresh UI components
                                        currencySymbol = settings.currencySymbol
                                        defaultGrid = getGridSizeFromString(settings.defaultGridSize)
                                        pdfQuality = settings.pdfQuality.toFloat()
                                        storageLocation = settings.storageLocation
                                        notificationsEnabled = settings.notificationsEnabled
                                        
                                        // Check if these properties exist before accessing
                                        try {
                                            useZipStorage = settings.javaClass.getDeclaredField("useZipStorage")
                                                .apply { isAccessible = true }.get(settings) as Boolean
                                        } catch (e: NoSuchFieldException) { 
                                            Log.d("SettingsScreen", "useZipStorage property not found")
                                        }
                                        
                                        try {
                                            autoArchive = settings.javaClass.getDeclaredField("autoArchive")
                                                .apply { isAccessible = true }.get(settings) as Boolean
                                        } catch (e: NoSuchFieldException) { 
                                            Log.d("SettingsScreen", "autoArchive property not found")
                                        }
                                        
                                        try {
                                            archiveNaming = settings.javaClass.getDeclaredField("archiveNaming")
                                                .apply { isAccessible = true }.get(settings) as String
                                        } catch (e: NoSuchFieldException) { 
                                            Log.d("SettingsScreen", "archiveNaming property not found")
                                        }
                                    } catch (refreshEx: Exception) {
                                        Log.e("SettingsScreen", "Error refreshing UI state", refreshEx)
                                    }
                                    
                                    Log.d("SettingsScreen", "Data clearing complete, UI refreshed")
                                }
                            } catch (e: Exception) {
                                Log.e("SettingsScreen", "Error clearing data", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Error clearing data: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    showClearDataDialog = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE13434)  // Bright red color as shown in screenshot
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel", color = Color(0xFF0C7489))  // Teal color as in screenshot
                }
            }
        )
    }
    
    // Storage Analysis Dialog
    if (showStorageAnalysisDialog && storageAnalysisResult != null) {
        AlertDialog(
            onDismissRequest = { showStorageAnalysisDialog = false },
            title = { Text("Storage Analysis") },
            text = { 
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Total Storage Used: ${formatFileSize(totalStorageUsed)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // List each storage category with size
                    storageAnalysisResult?.forEach { (category, size) ->
                        if (size > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatStorageCategory(category),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatFileSize(size),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            // Show percentage bar
                            val percentage = (size.toFloat() / totalStorageUsed.toFloat())
                            LinearProgressIndicator(
                                progress = percentage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .padding(bottom = 8.dp),
                                color = when {
                                    percentage > 0.5f -> MaterialTheme.colorScheme.error
                                    percentage > 0.3f -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Note: Run the 'Clean Up Temporary Files' function to remove unnecessary files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorageAnalysisDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Storage Cleanup Result Dialog
    if (showCleanupResultDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupResultDialog = false },
            title = { Text("Cleanup Results") },
            text = { 
                Text(cleanupResult)
            },
            confirmButton = {
                TextButton(onClick = { showCleanupResultDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Loading indicator for storage operations
    if (isAnalyzingStorage || isCleaningStorage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isAnalyzingStorage) 
                            "Analyzing storage usage..." 
                        else 
                            "Cleaning up files...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Get a GridSize enum from a string name
 */
private fun getGridSizeFromString(gridSizeName: String): GridSize {
    return try {
        GridSize.valueOf(gridSizeName)
    } catch (e: Exception) {
        GridSize.ONE_BY_ONE
    }
}

/**
 * Get a display name for a grid size
 */
private fun getGridSizeDisplayName(gridSize: GridSize): String {
    return gridSize.displayName
}

/**
 * Customized dialog for settings selection
 */
@Composable
fun SimpleSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Radio button option for selection dialogs
 */
@Composable
fun SimpleRadioOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * Extension function to start a backup process
 */
fun CoroutineScope.startBackup(
    context: Context,
    destinationUri: Uri? = null,
    setBackupInProgress: (Boolean) -> Unit,
    setBackupProgress: (Float) -> Unit,
    setBackupResultMessage: (String) -> Unit,
    setShowBackupResultDialog: (Boolean) -> Unit
) {
    val settingsManager = SettingsManager.getInstance(context)
    
    // Show loading indicator
    setBackupInProgress(true)
    setBackupProgress(0f)
    
    // Launch in the CoroutineScope
    this.launch {
        try {
            // Verify settings first and log diagnostics
            val diagnostics = BackupModule.verifyBackupSettings(context)
            Log.d("SettingsScreen", diagnostics)
            
            // Ensure backup directories exist
            val directoriesExist = BackupModule.ensureDirectoriesExist(context)
            if (!directoriesExist) {
                Log.e("SettingsScreen", "Failed to create backup directories")
                
                withContext(Dispatchers.Main) {
                    setBackupInProgress(false)
                    Toast.makeText(
                        context,
                        "Failed to create backup directories. Check app permissions.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }
            
            // Create backup with progress updates
            @Suppress("SpellCheckingInspection")
            val timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            )
            
            // Log the timestamp that will be used in the backup filename
            Log.d("SettingsScreen", "Creating backup with timestamp: $timestamp")
            
            // Create backup with progress updates - directly call without creating backupFileName
            val result = BackupModule.createBackupWithProgress(
                context,
                destinationUri,
                onProgressUpdate = { progress ->
                    // Update the progress on the UI thread
                    launch(Dispatchers.Main) {
                        setBackupProgress(progress)
                    }
                }
            )
            
            // Update UI on main thread
            withContext(Dispatchers.Main) {
                setBackupInProgress(false)
                
                if (result.isDuplicate) {
                    // Show a toast for duplicate backups
                    Toast.makeText(
                        context,
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                } else if (result.success) {
                    setBackupResultMessage("Backup created successfully: ${result.fileName}\nLocation: ${if (destinationUri != null) "Custom location" else settingsManager.storageLocation}")
                    setShowBackupResultDialog(true)
                } else {
                    Toast.makeText(
                        context,
                        "Backup failed: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                setBackupInProgress(false)
                Toast.makeText(
                    context,
                    "Backup failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

/**
 * Recursively delete files in a directory and return count of deleted files
 * This is a compatible alternative to Path.walk() which requires API level 26+
 */
private fun deleteFilesRecursively(directory: File): Int {
    var deletedCount = 0
    if (directory.exists() && directory.isDirectory) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deletedCount += deleteFilesRecursively(file)
            } else if (file.isFile) {
                if (file.delete()) {
                    deletedCount++
                    Log.d("SettingsScreen", "Deleted file: ${file.name}")
                } else {
                    Log.e("SettingsScreen", "Failed to delete file: ${file.name}")
                }
            }
        }
    }
    return deletedCount
}

/**
 * Format a file size in bytes to a human-readable string
 */
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${(size / 1024f).toInt()} KB"
        size < 1024 * 1024 * 1024 -> "${(size / (1024f * 1024f)).toInt()} MB"
        else -> "${(size / (1024f * 1024f * 1024f)).toInt()} GB"
    }
}

/**
 * Format a storage category name to be more readable
 */
private fun formatStorageCategory(category: String): String {
    return when (category) {
        "database" -> "Database Files"
        "preferences" -> "Preferences Files"
        "cache" -> "Cache Files"
        "temp" -> "Temporary Files"
        "logs" -> "Log Files"
        "receipts" -> "Receipt Images"
        "exports" -> "Exported Files"
        "backups" -> "Backup Files"
        "residual" -> "Residual Files"
        else -> category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
} 