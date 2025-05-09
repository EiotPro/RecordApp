package com.example.recordapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recordapp.R
import com.example.recordapp.ui.components.FolderStatsCard
import com.example.recordapp.ui.components.FolderTabs
import com.example.recordapp.ui.components.MoveExpenseDialog
import com.example.recordapp.ui.components.PagedExpenseList
import com.example.recordapp.ui.components.SwipeableExpenseList
import com.example.recordapp.ui.navigation.Screen
import com.example.recordapp.util.GridSize
import com.example.recordapp.util.PermissionUtils
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val settings = SettingsManager.getInstance(context)
    val scope = rememberCoroutineScope()
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val availableFolders by viewModel.availableFolders.collectAsState()
    val selectedFolder by viewModel.selectedViewFolder.collectAsState()
    
    // UI state
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var selectedExpenseId by remember { mutableStateOf<String?>(null) }
    var selectedExpenseFolder by remember { mutableStateOf("") }
    var pendingExportOperation by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    // Define pending export operation types
    val PDF_EXPORT = 1
    val CSV_EXPORT = 2
    val GRID_EXPORT = 3
    var pendingExportType by remember { mutableStateOf(0) }
    var pendingExportFolder by remember { mutableStateOf("") }
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted && pendingExportOperation != null) {
            // Execute the pending export operation
            pendingExportOperation?.invoke()
            pendingExportOperation = null
            
            // Show a notification that permission is granted
            Toast.makeText(
                context,
                "Starting ${if (pendingExportType == PDF_EXPORT) "PDF" else if (pendingExportType == CSV_EXPORT) "CSV" else "Grid PDF"} export...",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Show error message
            Toast.makeText(
                context,
                context.getString(R.string.storage_permission_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Function to export PDF with permission handling
    fun exportPdf(folderName: String) {
        if (PermissionUtils.hasStoragePermissions(context)) {
            scope.launch {
                // Show a toast that export is starting
                Toast.makeText(
                    context,
                    context.getString(R.string.export_starting, "PDF"),
                    Toast.LENGTH_SHORT
                ).show()
                
                viewModel.generatePdfByFolder(folderName) { pdfFile ->
                    pdfFile?.let { file ->
                        try {
                            // Export successful
                            Toast.makeText(
                                context,
                                context.getString(R.string.export_success, "PDF") + 
                                " - " + file.absolutePath,
                                Toast.LENGTH_LONG
                            ).show()
                            
                            val intent = viewModel.getViewPdfIntent(file)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Failed to open the file
                            Toast.makeText(
                                context,
                                "PDF created but couldn't open: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("ExpensesScreen", "Failed to open PDF", e)
                        }
                    } ?: run {
                        // Export failed
                        val errorMsg = viewModel.uiState.value.errorMessage ?: "Unknown error"
                        Toast.makeText(
                            context,
                            context.getString(R.string.export_failed, "PDF") + 
                            " - $errorMsg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            // Save the operation for after permissions are granted
            pendingExportType = PDF_EXPORT
            pendingExportFolder = folderName
            pendingExportOperation = { exportPdf(folderName) }
            
            // Show permission dialog
            showPermissionDialog = true
        }
    }
    
    // Function to export CSV with permission handling
    fun exportCsv(folderName: String) {
        if (PermissionUtils.hasStoragePermissions(context)) {
            scope.launch {
                // Show a toast that export is starting
                Toast.makeText(
                    context,
                    context.getString(R.string.export_starting, "CSV"),
                    Toast.LENGTH_SHORT
                ).show()
                
                viewModel.generateCsvByFolder(folderName) { csvFile ->
                    csvFile?.let { file ->
                        try {
                            // Export successful
                            Toast.makeText(
                                context,
                                context.getString(R.string.export_success, "CSV") + 
                                " - " + file.absolutePath,
                                Toast.LENGTH_LONG
                            ).show()
                            
                            val intent = viewModel.getShareFileIntent(file)
                            context.startActivity(Intent.createChooser(intent, "Share CSV File"))
                        } catch (e: Exception) {
                            // Failed to share the file
                            Toast.makeText(
                                context,
                                "CSV created but couldn't share: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("ExpensesScreen", "Failed to share CSV", e)
                        }
                    } ?: run {
                        // Export failed
                        val errorMsg = viewModel.uiState.value.errorMessage ?: "Unknown error"
                        Toast.makeText(
                            context,
                            context.getString(R.string.export_failed, "CSV") + 
                            " - $errorMsg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            // Save the operation for after permissions are granted
            pendingExportType = CSV_EXPORT
            pendingExportFolder = folderName
            pendingExportOperation = { exportCsv(folderName) }
            
            // Show permission dialog
            showPermissionDialog = true
        }
    }
    
    // Function to export grid-based PDF with permission handling
    fun exportGridPdf() {
        if (PermissionUtils.hasStoragePermissions(context)) {
            scope.launch {
                // Show a toast that export is starting
                Toast.makeText(
                    context,
                    context.getString(R.string.export_starting, "Grid PDF"),
                    Toast.LENGTH_SHORT
                ).show()
                
                // Get default grid size from settings
                val defaultGridSize = try {
                    GridSize.valueOf(settings.defaultGridSize)
                } catch (e: Exception) {
                    GridSize.ONE_BY_ONE
                }
                
                // Generate the grid PDF using the default grid size from settings
                viewModel.generateImageGridPdf(defaultGridSize) { pdfFile ->
                    pdfFile?.let { file ->
                        try {
                            // Export successful
                            Toast.makeText(
                                context,
                                context.getString(R.string.export_success, "Grid PDF") + 
                                " - " + file.absolutePath,
                                Toast.LENGTH_LONG
                            ).show()
                            
                            val intent = viewModel.getViewPdfIntent(file)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Failed to open the file
                            Toast.makeText(
                                context,
                                "Grid PDF created but couldn't open: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("ExpensesScreen", "Failed to open Grid PDF", e)
                        }
                    } ?: run {
                        // Export failed
                        val errorMsg = viewModel.uiState.value.errorMessage ?: "Unknown error"
                        Toast.makeText(
                            context,
                            context.getString(R.string.export_failed, "Grid PDF") + 
                            " - $errorMsg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            // Save the operation for after permissions are granted
            pendingExportType = GRID_EXPORT
            pendingExportOperation = { exportGridPdf() }
            
            // Show permission dialog
            showPermissionDialog = true
        }
    }
    
    // Folder stats for the currently selected folder
    var folderStats by remember { mutableStateOf(Pair(0, 0.0)) }
    
    // Update folder stats when selected folder changes
    LaunchedEffect(selectedFolder) {
        selectedFolder?.let { folderName ->
            viewModel.getFolderStats(folderName).collect { stats ->
                folderStats = stats
            }
        }
    }
    
    // Refresh folders list when expenses change
    LaunchedEffect(expenses) {
        viewModel.refreshFolders()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    // Create a local non-delegated property for the title
                    val titleText = if (selectedFolder == null) {
                        stringResource(R.string.expense_list)
                    } else {
                        "Folder: $selectedFolder"
                    }
                    Text(text = titleText)
                },
                actions = {
                    // Only show dropdown menu when there are expenses
                    if (expenses.isNotEmpty()) {
                        IconButton(onClick = { showDropdownMenu = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            // Export options only shown when a folder is selected
                            selectedFolder?.let { folderName ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.export_folder_pdf)) },
                                    onClick = {
                                        showDropdownMenu = false
                                        exportPdf(folderName)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.PictureAsPdf,
                                            contentDescription = null
                                        )
                                    }
                                )
                                
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.export_folder_csv)) },
                                    onClick = {
                                        showDropdownMenu = false
                                        exportCsv(folderName)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.TableChart,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                            
                            // Grid export option is always shown regardless of folder selection
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_folder_grid)) },
                                onClick = {
                                    showDropdownMenu = false
                                    exportGridPdf()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Folder tabs
            FolderTabs(
                folders = availableFolders,
                selectedFolder = selectedFolder,
                onFolderSelected = { folder ->
                    viewModel.setSelectedViewFolder(folder)
                }
            )
            
            // Show folder stats if a folder is selected
            if (selectedFolder != null) {
                selectedFolder?.let { folderName ->
                    FolderStatsCard(
                        folderName = folderName,
                        expenseCount = folderStats.first,
                        totalAmount = folderStats.second,
                        formattedAmount = settings.formatAmount(folderStats.second),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            
            // Expenses list
            Box(
                modifier = Modifier.weight(1f)
            ) {
                PagedExpenseList(
                    expenses = viewModel.pagedExpenses,
                    onDeleteExpense = { viewModel.deleteExpense(it) },
                    onUndoDelete = { viewModel.undoDelete(it) },
                    onMoveExpense = { expenseId, currentFolder ->
                        selectedExpenseId = expenseId
                        selectedExpenseFolder = currentFolder
                        showMoveDialog = true
                    },
                    onExpenseClick = { expenseId ->
                        navController.navigate(Screen.ExpenseDetail.createRoute(expenseId))
                    }
                )
            }
        }
    }
    
    // Show move dialog when requested
    if (showMoveDialog && selectedExpenseId != null) {
        MoveExpenseDialog(
            currentFolder = selectedExpenseFolder,
            availableFolders = availableFolders.map { it.first },
            onMove = { newFolder -> 
                viewModel.moveExpenseToFolder(selectedExpenseId!!, newFolder)
                selectedExpenseId = null
            },
            onDismiss = { 
                showMoveDialog = false
                selectedExpenseId = null
            },
            onCreateFolder = { newFolder ->
                // Create folder and refresh
                viewModel.setCurrentFolder(newFolder)
                viewModel.refreshFolders()
            }
        )
    }
    
    // Add permission dialog at the end of the composable
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.storage_permission_required)) },
            text = { 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Text(stringResource(R.string.permission_settings_prompt) + 
                         "\n\nFor Android 11+, you must grant \"Allow management of all files\" permission in Settings.")
                } else {
                    Text(stringResource(R.string.permission_settings_prompt))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // For Android 11+, we need to direct to Special app access settings
                            try {
                                val intent = PermissionUtils.getManageExternalStorageIntent(context)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Please enable 'All files access' permission manually in Settings",
                                    Toast.LENGTH_LONG
                                ).show()
                                // Fall back to application settings
                                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(settingsIntent)
                            }
                        } else {
                            // For Android 10 and below, use the standard permission request
                            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
                        }
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}