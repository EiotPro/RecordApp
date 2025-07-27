package com.example.recordapp.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
//import androidx.compose.material.icons.filled.Done
//import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.recordapp.R
import com.example.recordapp.ui.components.AddExpenseBottomSheet
//import com.example.recordApp.ui.components.AddWidgetPlaceholder
import com.example.recordapp.ui.components.ImageCaptureDialog
import com.example.recordapp.ui.components.RecentTransactionsWidget
import com.example.recordapp.ui.components.SettingsBottomSheet
import com.example.recordapp.ui.navigation.Screen
import com.example.recordapp.util.PermissionUtils
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

/**
 * Redesigned HomeScreen as a personalized dashboard with customizable widgets
 */
@SuppressLint("UseOfNonLambdaOffsetOverload")
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
//   var isEditingDashboard by remember { mutableStateOf(false) }
    
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
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentImageUri != null) {
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
            viewModel.processImageWithOcr(it, 80)
            showImageCaptureDialog = true
        }
    }

    fun scanReceipt() {
        if (PermissionUtils.hasStoragePermissions(context)) {
            // Create a temporary file for the image
            currentImageUri = viewModel.createImageFileUri()
            // Launch camera instead of gallery
            currentImageUri?.let {
                cameraLauncher.launch(it)
            }
        } else {
            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
        }
    }
    
    // Calculate total amount and expense count
    val totalAmount = expenses.sumOf { it.amount }
    val expenseCount = expenses.size
    
    Scaffold(
        topBar = {
            // Custom top app bar (empty) to reserve space
            Box(modifier = Modifier.height(0.dp)) // Reduced from 4dp to 0dp
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Content with reduced top padding to move closer to top
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Animated welcome text at the top with reduced top padding
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(25.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp)
                ) {
                    // Settings button positioned inside the blue banner on the left
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterStart)
                            .offset(x = (-8).dp) // Move slightly left to align better
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Text(
                        text = "IotLogic RecordApp - Track Your Expenses Smartly",
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
                        .padding(top = 8.dp) 
                ) {
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
                        // Using logo2 png image
                        Image(
                            painter = painterResource(id = R.drawable.logo2),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .scale(logoScaleState.value)
                                .background(Color.Black),
                            contentScale = ContentScale.Fit
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
                                    text = "RecordApp->Amir",
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
                    
                    // Financial summary cards - MOVED UP before folder section
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Enhanced folder card with capture/upload actions - MOVED DOWN after financial summary
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
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(3.dp)
                                            .size(14.dp)
                                    )
                                }
                                
                                Text(
                                    text = " to select or ",
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
                    
                    // Recent transactions section
                    RecentTransactionsWidget(
                        recentExpenses = expenses.sortedByDescending { it.timestamp }.take(5),
                        onExpenseClick = { expenseId -> navController.navigate("${Screen.ExpenseDetail.route}/$expenseId") },
                        onViewAllClick = { navController.navigate(Screen.Expenses.route) },
//                    isEditing = isEditingDashboard
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Add widget placeholder (only visible in edit mode)
//                    AnimatedVisibility(visible = isEditingDashboard)
//                    {
//                        AddWidgetPlaceholder(
//                            onClick = { /* Show widget selection dialog */ }
//                        )
//                    }
                    
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
            onSave = { serialNum, desc, amt, expenseDateTime ->
                viewModel.addExpenseWithImage(
                    description = desc,
                    amount = amt,
                    folderName = currentFolderValue,
                    imageUri = currentImageUri,
                    serialNumber = serialNum,
                    expenseDateTime = expenseDateTime,
                    generateRandomSerialIfBlank = true
                )
                showImageCaptureDialog = false
            },
            onOcrRequest = {
                currentImageUri?.let {
                    val settingsManager = SettingsManager.getInstance(context)
                    viewModel.processImageWithOcr(it, settingsManager.imageCompression)
                }
            },
            onCancel = { showImageCaptureDialog = false }
        )
    }
}