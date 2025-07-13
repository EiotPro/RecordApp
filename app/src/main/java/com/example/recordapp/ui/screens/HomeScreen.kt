package com.example.recordapp.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.recordapp.R
import com.example.recordapp.model.Expense
import com.example.recordapp.model.GridSize
import com.example.recordapp.ui.components.ExpenseList
import com.example.recordapp.ui.components.FolderSelector
import com.example.recordapp.ui.components.ImageCaptureDialog
import com.example.recordapp.ui.components.RecentExpenseCard
import com.example.recordapp.ui.navigation.Screen
import com.example.recordapp.util.PermissionUtils
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.viewmodel.ExpenseViewModel
import com.example.recordapp.viewmodel.ExpenseUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState(initial = ExpenseUiState())
    val ocrResult by viewModel.ocrResult.collectAsState(initial = null)
    val settings = SettingsManager.getInstance(context)
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showFolderSelectionDialog by remember { mutableStateOf(false) }
    var showImageCaptureDialog by remember { mutableStateOf(false) }
    var showImageSelectionDialog by remember { mutableStateOf(false) }
    var currentImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Get accent color
    val accentColorResource = when (settings.accentColor) {
        "green" -> R.color.accent_green
        "purple" -> R.color.accent_purple
        "red" -> R.color.accent_red
        "orange" -> R.color.accent_orange
        else -> R.color.accent_blue
    }
    
    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // First show folder selection dialog instead of image selection dialog
            showFolderSelectionDialog = true
        } else {
            showPermissionDialog = true
        }
    }
    
    // Camera launcher for taking pictures
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentImageUri != null) {
            // Process image with OCR using the selected compression quality
            viewModel.processImageWithOcr(currentImageUri!!, settings.imageCompression)
            showImageCaptureDialog = true
        }
    }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            currentImageUri = it
            // Process image with OCR using the selected compression quality
            viewModel.processImageWithOcr(it, settings.imageCompression)
            showImageCaptureDialog = true
        }
    }
    
    // Add currentFolder state collection
    val currentFolder by viewModel.currentFolder.collectAsState()
    val availableFolders by viewModel.availableFolders.collectAsState()
    
    // Export functions
    fun exportPdf() {
        if (PermissionUtils.hasStoragePermissions(context)) {
            scope.launch {
                viewModel.generatePdf { pdfFile ->
                    pdfFile?.let { file ->
                        try {
                            val intent = viewModel.getViewPdfIntent(file)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                }
            }
        } else {
            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
        }
    }
    
    fun exportCsv() {
        if (PermissionUtils.hasStoragePermissions(context)) {
            scope.launch {
                viewModel.generateCsv { csvFile ->
                    csvFile?.let { file ->
                        try {
                            val intent = viewModel.getShareFileIntent(file)
                            context.startActivity(Intent.createChooser(intent, "Share CSV File"))
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                }
            }
        } else {
            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
        }
    }
    
    fun exportGridPdf() {
        if (PermissionUtils.hasStoragePermissions(context)) {
            scope.launch {
                // Get default grid size from settings
                val defaultGridSize = try {
                    GridSize.valueOf(settings.defaultGridSize)
                } catch (e: Exception) {
                    GridSize.ONE_BY_ONE
                }
                
                viewModel.generateImageGridPdf(defaultGridSize) { pdfFile ->
                    pdfFile?.let { file ->
                        try {
                            val intent = viewModel.getViewPdfIntent(file)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                }
            }
        } else {
            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "RecordApp",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            
                            // Show current folder as subtitle
                            if (currentFolder != "default") {
                                Text(
                                    "Folder: $currentFolder",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        
                        // Settings icon moved inside title row for less padding
                        IconButton(
                            onClick = { navController.navigate(Screen.Settings.route) },
                            modifier = Modifier.padding(start = 0.dp)  // Remove left padding
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                },
                actions = {
                    // Settings icon moved to title area
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(accentColorResource)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (PermissionUtils.hasStoragePermissions(context)) {
                        // First show folder selection dialog
                        showFolderSelectionDialog = true
                    } else {
                        permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
                    }
                },
                containerColor = colorResource(accentColorResource),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Record") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Get most recent expense
                val mostRecentExpense by viewModel.getMostRecentExpense().collectAsState(initial = null)
                
                // Display quick action buttons at the top for easy access
                if (expenses.isNotEmpty()) {
                    QuickActionButtons(
                        onExportPdf = { exportPdf() },
                        onExportCsv = { exportCsv() },
                        onExportGrid = { exportGridPdf() },
                        onViewAll = { navController.navigate(Screen.Expenses.route) },
                        accentColor = colorResource(accentColorResource),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                // Display the folder selector in a more compact form
                FolderSelectorCompact(
                    folders = availableFolders.map { it.first },
                    selectedFolder = currentFolder,
                    onFolderSelected = { folderName ->
                        viewModel.setCurrentFolder(folderName)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                if (expenses.isEmpty()) {
                    // Empty state with animated illustration
                    EmptyHomeState()
                } else {
                    // Financial summary card
                    FinancialSummaryCard(
                        expenses = expenses,
                        accentColor = colorResource(accentColorResource),
                        settings = settings,
                        onViewAllClick = { navController.navigate(Screen.Expenses.route) }
                    )
                    
                    // Most recent expense
                    if (mostRecentExpense != null) {
                        Text(
                            text = "Latest Transaction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        RecentTransactionItem(
                            expense = mostRecentExpense!!,
                            onClick = {
                                navController.navigate(Screen.ExpenseDetail.createRoute(mostRecentExpense!!.id))
                            },
                            accentColor = colorResource(accentColorResource),
                            settings = settings
                        )
                    }
                    
                    // Recent transactions (up to 5)
                    if (expenses.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            TextButton(onClick = { navController.navigate(Screen.Expenses.route) }) {
                                Text("View All")
                            }
                        }
                        
                        // Display up to 5 recent expenses (excluding the most recent one)
                        expenses.drop(1).take(5).forEach { expense ->
                            TransactionItem(
                                expense = expense,
                                onClick = {
                                    navController.navigate(Screen.ExpenseDetail.createRoute(expense.id))
                                },
                                settings = settings
                            )
                        }
                    }
                    
                    // Folder summary section
                    if (availableFolders.isNotEmpty()) {
                        FolderSummarySection(
                            folders = availableFolders.take(4),
                            onFolderClick = { folderName ->
                                viewModel.setSelectedViewFolder(folderName)
                                navController.navigate(Screen.Expenses.route)
                            },
                            accentColor = colorResource(accentColorResource),
                            settings = settings
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                }
            }
            
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorResource(accentColorResource)
                )
            }
        }
        
        // Permission dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Permissions Required") },
                text = {
                    Text(
                        "Storage permissions are required to use this app." +
                                " Please grant these permissions in app settings."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Folder selection dialog
        if (showFolderSelectionDialog) {
            Dialog(onDismissRequest = { showFolderSelectionDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Select Folder",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(accentColorResource)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Select a folder to store your captured images",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Folder selector
                        FolderSelector(
                            selectedFolder = currentFolder,
                            onFolderSelected = { folderName -> 
                                viewModel.setCurrentFolder(folderName)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showFolderSelectionDialog = false }
                            ) {
                                Text("Cancel")
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    showFolderSelectionDialog = false
                                    showImageSelectionDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(accentColorResource)
                                )
                            ) {
                                Text("Continue")
                            }
                        }
                    }
                }
            }
        }
        
        // Image selection dialog
        if (showImageSelectionDialog) {
            EnhancedImageSelectionDialog(
                onCameraSelected = {
                    showImageSelectionDialog = false
                    // Use camera launcher
                    currentImageUri = viewModel.createImageFileUri()
                    currentImageUri?.let {
                        cameraLauncher.launch(it)
                    }
                },
                onGallerySelected = {
                    showImageSelectionDialog = false
                    galleryLauncher.launch("image/*")
                },
                onDismiss = {
                    showImageSelectionDialog = false
                },
                accentColor = colorResource(accentColorResource)
            )
        }
        
        // Image capture dialog
        if (showImageCaptureDialog && currentImageUri != null) {
            ImageCaptureDialog(
                imageUri = currentImageUri,
                ocrResult = ocrResult,
                folderName = currentFolder,
                onSave = { serialNumber, description, amount ->
                    viewModel.addExpense(
                        imagePath = currentImageUri,
                        serialNumber = serialNumber,
                        amount = amount,
                        description = description,
                        compressionQuality = settings.imageCompression
                    )
                    currentImageUri = null
                    showImageCaptureDialog = false
                },
                onOcrRequest = {
                    currentImageUri?.let {
                        viewModel.processImageWithOcr(it, settings.imageCompression)
                    }
                },
                onCancel = {
                    currentImageUri = null
                    viewModel.clearOcrResult()
                    showImageCaptureDialog = false
                }
            )
        }
    }
}

@Composable
fun QuickActionButtons(
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onExportGrid: () -> Unit,
    onViewAll: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.1f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        IconButton(
            onClick = onExportPdf,
            modifier = Modifier
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = "Export PDF",
                tint = accentColor
            )
        }
        
        IconButton(
            onClick = onExportCsv,
            modifier = Modifier
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.TableView,
                contentDescription = "Export CSV",
                tint = accentColor
            )
        }
        
        IconButton(
            onClick = onExportGrid,
            modifier = Modifier
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Export Grid PDF",
                tint = accentColor
            )
        }
        
        IconButton(
            onClick = onViewAll,
            modifier = Modifier
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ViewList,
                contentDescription = "View All Expenses",
                tint = accentColor
            )
        }
    }
}

@Composable
fun FolderSelectorCompact(
    folders: List<String>,
    selectedFolder: String,
    onFolderSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDropdown = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Current Folder",
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = selectedFolder,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Folder"
            )
        }
        
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder) },
                    onClick = {
                        onFolderSelected(folder)
                        showDropdown = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (folder == selectedFolder) Icons.Default.FolderOpen else Icons.Default.Folder,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun FolderSummarySection(
    folders: List<Pair<String, Int>>,
    onFolderClick: (String) -> Unit,
    accentColor: Color,
    settings: SettingsManager
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Folders",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            folders.forEach { (folder, count) ->
                FolderItem(
                    name = folder,
                    count = count,
                    onClick = { onFolderClick(folder) },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FolderItem(
    name: String,
    count: Int,
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.2f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = accentColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = "$count items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyHomeState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Icon
        val infiniteTransition = rememberInfiniteTransition(label = "emptyAnimation")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .padding(16.dp)
                .scale(scale),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to RecordApp!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Start tracking your expenses by tapping the + button to add your first record.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Tips to get started:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take a photo of your receipt")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add amount and description")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export to PDF when needed")
                }
            }
        }
    }
}

@Composable
fun FinancialSummaryCard(
    expenses: List<Expense>,
    accentColor: Color,
    settings: SettingsManager,
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.8f),
                            accentColor.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Financial Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Total amount
                SummaryItem(
                    value = settings.formatAmount(expenses.sumOf { it.amount }),
                    label = "Total Spent",
                    backgroundColor = Color.White.copy(alpha = 0.2f),
                    textColor = Color.White
                )
                
                // Total records
                SummaryItem(
                    value = expenses.size.toString(),
                    label = "Records",
                    backgroundColor = Color.White.copy(alpha = 0.2f),
                    textColor = Color.White
                )
                
                // Average amount
                SummaryItem(
                    value = settings.formatAmount(expenses.sumOf { it.amount } / expenses.size),
                    label = "Average",
                    backgroundColor = Color.White.copy(alpha = 0.2f),
                    textColor = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // View all expenses button
            Button(
                onClick = onViewAllClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = accentColor
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = "View all",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View All Expenses")
            }
        }
    }
}

@Composable
fun SummaryItem(
    value: String,
    label: String,
    backgroundColor: Color,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(12.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun RecentTransactionItem(
    expense: Expense,
    onClick: () -> Unit,
    accentColor: Color,
    settings: SettingsManager
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Image if available
            if (expense.imagePath != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    AsyncImage(
                        model = expense.imagePath,
                        contentDescription = "Receipt image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                    
                    // Amount overlay
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = settings.formatAmount(expense.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    
                    // Date and optional serial number
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        // Format date to more readable form
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
                        val date = expense.getFormattedTimestamp()
                        
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        
                        if (expense.serialNumber.isNotEmpty()) {
                            Text(
                                text = "Ref: ${expense.serialNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        if (expense.description.isNotEmpty()) {
                            Text(
                                text = expense.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // If no image, show a simple card with details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon placeholder
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.2f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = expense.getFormattedTimestamp(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (expense.description.isNotEmpty()) {
                            Text(
                                text = expense.description,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        if (expense.serialNumber.isNotEmpty()) {
                            Text(
                                text = "Ref: ${expense.serialNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Text(
                        text = settings.formatAmount(expense.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    expense: Expense,
    onClick: () -> Unit,
    settings: SettingsManager
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail if available
        if (expense.imagePath != null) {
            AsyncImage(
                model = expense.imagePath,
                contentDescription = "Receipt thumbnail",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Description and date
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = expense.description.ifEmpty { "Expense Record" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = expense.getFormattedTimestamp(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (expense.folderName != "default") {
                    Text(
                        text = " • ${expense.folderName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Amount
        Text(
            text = settings.formatAmount(expense.amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun EnhancedImageSelectionDialog(
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Add New Record",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Choose how you want to add your receipt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // First option - Camera
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onCameraSelected)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Take Photo",
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Take Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Text(
                            text = "Capture a new image with your camera",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Gallery option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onGallerySelected)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = "Browse Gallery",
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Browse Gallery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Text(
                            text = "Browse and select from all your images",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Cancel",
                        color = accentColor
                    )
                }
            }
        }
    }
} 