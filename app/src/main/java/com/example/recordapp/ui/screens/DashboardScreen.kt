package com.example.recordapp.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recordapp.R
import com.example.recordapp.model.Expense
import com.example.recordapp.ui.components.*
import com.example.recordapp.ui.navigation.Screen
import com.example.recordapp.util.PermissionUtils
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.viewmodel.ExpenseViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.example.recordapp.util.FileUtils

/**
 * Redesigned HomeScreen as a personalized dashboard with customizable widgets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val settings = SettingsManager.getInstance(context)
    val scrollState = rememberScrollState()
    
    // Dashboard editing state
    var isEditingDashboard by remember { mutableStateOf(false) }
    
    // Bottom sheet states
    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showImageCaptureDialog by remember { mutableStateOf(false) }
    
    // Camera and gallery states
    var currentImageUri by remember { mutableStateOf<Uri?>(null) }
    val ocrResult by viewModel.ocrResult.collectAsState(initial = null)
    
    // Folder state 
    val currentFolder by viewModel.currentFolder.collectAsState()
    val availableFolders by viewModel.availableFolders.collectAsState(initial = emptyList())
    
    // Create a list of all folders with "default" always being first
    val allFolders = remember(availableFolders) {
        val folderNames = availableFolders.map { it.first }
        if (!folderNames.contains("default")) {
            listOf("default") + folderNames.sorted()
        } else {
            listOf("default") + folderNames.filter { it != "default" }.sorted()
        }
    }
    
    // Get accent color
    val accentColorResource = when (settings.accentColor) {
        "green" -> R.color.accent_green
        "purple" -> R.color.accent_purple
        "red" -> R.color.accent_red
        "orange" -> R.color.accent_orange
        "blue" -> R.color.accent_blue
        else -> R.color.accent_blue
    }
    
    // Animation states
    val welcomeTextState = remember { MutableTransitionState(false) }
    val logoScaleState = remember { Animatable(0.8f) }
    
    // Start animations
    LaunchedEffect(true) {
        welcomeTextState.targetState = true
        logoScaleState.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = EaseOutBack
            )
        )
    }

    // Text animation for left to right and right to left
    val leftToRightAnim = rememberInfiniteTransition(label = "leftToRight")
    val textOffset = leftToRightAnim.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textMove"
    )
    
    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Handle permission granted
        } else {
            // Handle permission denied
        }
    }
    
    // Camera capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentImageUri != null) {
            // Process image with OCR
            viewModel.processImageWithOcr(currentImageUri!!, 80)
            showImageCaptureDialog = true
        }
    }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            currentImageUri = it
            viewModel.processImageWithOcr(it, 80)
            showImageCaptureDialog = true
        }
    }
    
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
    
    fun scanReceipt() {
        if (PermissionUtils.hasCameraPermissions(context)) {
            // Create a temporary file for the camera image
            currentImageUri = viewModel.createImageFileUri()
            currentImageUri?.let {
                cameraLauncher.launch(it)
            }
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }
    
    // Calculate total amount and expense count
    val totalAmount = expenses.sumOf { it.amount }
    val expenseCount = expenses.size
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "iotogic RecordApp",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                actions = {
                    // Edit dashboard button
                    IconButton(onClick = { isEditingDashboard = !isEditingDashboard }) {
                        Icon(
                            imageVector = if (isEditingDashboard) Icons.Default.Done else Icons.Default.Edit,
                            contentDescription = if (isEditingDashboard) "Done" else "Edit Dashboard"
                        )
                    }
                    
                    // Settings button
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Animated welcome text at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(25.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Welcome to iotogic RecordApp - Track Your Expenses Smartly",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = textOffset.value.dp)
                )
            }
            
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 25.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Header with logo and welcome animation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colorResource(accentColorResource),
                                    colorResource(accentColorResource).copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo animation
                    Image(
                        painter = painterResource(id = R.drawable.logo2), 
                        contentDescription = "Logo",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .scale(logoScaleState.value)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Welcome text with animation
                    Column {
                        AnimatedVisibility(
                            visibleState = welcomeTextState,
                            enter = fadeIn(animationSpec = tween(1000)) +
                                    slideInHorizontally(
                                        animationSpec = tween(1000),
                                        initialOffsetX = { it }
                                    ),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = "Welcome Back",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        AnimatedVisibility(
                            visibleState = welcomeTextState,
                            enter = fadeIn(animationSpec = tween(1000, delayMillis = 300)) +
                                    slideInHorizontally(
                                        animationSpec = tween(1000, delayMillis = 300),
                                        initialOffsetX = { -it }
                                    ),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = "Your financial companion",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Enhanced folder card with capture/upload actions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Header with folder info and actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Folder icon and name - SMALLER TEXT
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Current Folder",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "Active Folder: $currentFolder",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Camera button - BIGGER SIZE
                            IconButton(
                                onClick = { scanReceipt() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Capture Photo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Gallery upload button - BIGGER SIZE
                            IconButton(
                                onClick = { 
                                    // Launch gallery picker
                                    galleryLauncher.launch("image/*") 
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.tertiary,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "Upload from Gallery",
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Folder selection - GREEN INDICATOR FOR SELECTED FOLDER
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allFolders) { folder ->
                                val isSelected = folder == currentFolder
                                val selectedColor = Color(0xFF4CAF50) // Green color for selected folder
                                
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        scope.launch {
                                            viewModel.setCurrentFolder(folder)
                                            settings.defaultFolder = folder
                                        }
                                    },
                                    label = { Text(folder) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = selectedColor.copy(alpha = 0.15f),
                                        selectedLabelColor = selectedColor,
                                        selectedLeadingIconColor = selectedColor
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) selectedColor else MaterialTheme.colorScheme.outline,
                                        borderWidth = if (isSelected) 1.5.dp else 1.dp
                                    ),
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = selectedColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                        
                        // Folder action hints
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Use ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .size(14.dp)
                                )
                            }
                            
                            Text(
                                text = " to capture or ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .size(14.dp)
                                )
                            }
                            
                            Text(
                                text = " to upload to ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            
                            Text(
                                text = currentFolder,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50) // Green to match selected folder
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Financial summary cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total amount card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total Expenses",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatCurrency(totalAmount),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // Record count card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Records",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$expenseCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Recent transactions section
                RecentTransactionsWidget(
                    recentExpenses = expenses.sortedByDescending { it.timestamp }.take(3),
                    onExpenseClick = { expenseId -> navController.navigate("${Screen.ExpenseDetail.route}/$expenseId") },
                    onViewAllClick = { navController.navigate(Screen.Expenses.route) },
                    isEditing = isEditingDashboard
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add widget placeholder (only visible in edit mode)
                AnimatedVisibility(visible = isEditingDashboard) {
                    AddWidgetPlaceholder(
                        onClick = { /* Show widget selection dialog */ }
                    )
                }
                
                Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB
            }
            
            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // Bottom sheets
    if (showAddExpenseSheet) {
        AddExpenseBottomSheet(
            onDismiss = { showAddExpenseSheet = false },
            onSave = { description, folder, amount ->
                scope.launch {
                    viewModel.addExpense(
                        description = description,
                        amount = amount,
                        folderName = folder
                    )
                }
                showAddExpenseSheet = false
            },
            viewModel = viewModel
        )
    }
    
    if (showSettingsSheet) {
        SettingsBottomSheet(
            onDismiss = { showSettingsSheet = false },
            viewModel = viewModel
        )
    }
    
    // Image capture dialog
    if (showImageCaptureDialog && ocrResult != null) {
        val currentFolderValue = viewModel.currentFolder.collectAsState().value
        ImageCaptureDialog(
            imageUri = currentImageUri,
            ocrResult = ocrResult,
            folderName = currentFolderValue,
            onSave = { serialNum, desc, amt ->
                viewModel.addExpenseWithImage(
                    description = desc,
                    amount = amt,
                    folderName = currentFolderValue,
                    imageUri = currentImageUri
                )
                showImageCaptureDialog = false
            },
            onOcrRequest = {
                currentImageUri?.let {
                    viewModel.processImageWithOcr(it, FileUtils.DEFAULT_COMPRESSION_QUALITY)
                }
            },
            onCancel = { showImageCaptureDialog = false }
        )
    }
}