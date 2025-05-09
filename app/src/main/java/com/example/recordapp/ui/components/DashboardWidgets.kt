package com.example.recordapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.recordapp.model.Expense
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.launch

/**
 * Widget that displays a summary of expenses
 */
@Composable
fun ExpenseSummaryWidget(
    totalAmount: Double,
    expenseCount: Int,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onRemove: () -> Unit = {},
    onConfigure: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = SettingsManager.getInstance(context)
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(Locale.getDefault())
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    
    DashboardWidget(
        title = "Expense Summary",
        icon = Icons.Default.BarChart,
        modifier = modifier,
        isEditing = isEditing,
        onRemove = onRemove,
        onConfigure = onConfigure
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Total amount
            Text(
                text = "Total Expenses",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = currencyFormatter.format(totalAmount),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Expense count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$expenseCount ${if (expenseCount == 1) "Record" else "Records"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                TextButton(onClick = onViewAllClick) {
                    Text("View All")
                }
            }
        }
    }
}

/**
 * Widget that displays recent transactions
 */
@Composable
fun RecentTransactionsWidget(
    recentExpenses: List<Expense>,
    onExpenseClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onRemove: () -> Unit = {},
    onConfigure: () -> Unit = {}
) {
    val context = LocalContext.current
    
    DashboardWidget(
        title = "Recent Transactions",
        icon = Icons.Default.Receipt,
        modifier = modifier,
        isEditing = isEditing,
        onRemove = onRemove,
        onConfigure = onConfigure
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (recentExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent transactions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // List of recent expenses
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentExpenses.take(3).forEach { expense ->
                        TransactionItem(
                            expense = expense,
                            onClick = { onExpenseClick(expense.id) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // View all button
                TextButton(
                    onClick = onViewAllClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("View All")
                }
            }
        }
    }
}

/**
 * Widget that displays quick action buttons
 */
@Composable
fun QuickActionsWidget(
    onAddExpense: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onScanReceipt: () -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onRemove: () -> Unit = {},
    onConfigure: () -> Unit = {}
) {
    DashboardWidget(
        title = "Quick Actions",
        icon = Icons.Default.FlashOn,
        modifier = modifier,
        isEditing = isEditing,
        onRemove = onRemove,
        onConfigure = onConfigure
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(quickActions) { action ->
                QuickActionButton(
                    icon = action.icon,
                    label = action.label,
                    onClick = when (action.id) {
                        "add" -> onAddExpense
                        "pdf" -> onExportPdf
                        "csv" -> onExportCsv
                        "scan" -> onScanReceipt
                        else -> {{}}
                    }
                )
            }
        }
    }
}

/**
 * A single transaction item in the recent transactions widget
 */
@Composable
private fun TransactionItem(
    expense: Expense,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = SettingsManager.getInstance(context)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon or thumbnail
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (expense.imagePath != null) Icons.Default.Image else Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Expense details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Description or placeholder
                Text(
                    text = if (expense.description.isNotBlank()) expense.description else "Expense",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Date
                Text(
                    text = expense.getFormattedDate(context, includeTime = false),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Amount
            Text(
                text = expense.getFormattedAmount(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * A quick action button
 */
@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        // Button
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Data class for quick action buttons
 */
private data class QuickAction(
    val id: String,
    val icon: ImageVector,
    val label: String
)

/**
 * List of quick actions
 */
private val quickActions = listOf(
    QuickAction("add", Icons.Default.Add, "Add"),
    QuickAction("scan", Icons.Default.PhotoCamera, "Scan"),
    QuickAction("pdf", Icons.Default.PictureAsPdf, "PDF"),
    QuickAction("csv", Icons.Default.TableView, "CSV")
)

/**
 * Bottom sheet for adding a new expense
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    onDismiss: () -> Unit,
    onSave: (String, String, Double) -> Unit,
    viewModel: ExpenseViewModel
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val context = LocalContext.current
    val settingsManager = SettingsManager.getInstance(context)
    
    // Folder selection
    val availableFolders by viewModel.availableFolders.collectAsState(initial = emptyList())
    val currentFolder by viewModel.currentFolder.collectAsState()
    var selectedFolder by remember { mutableStateOf(currentFolder) }
    
    // Create a list of all folders with "default" always being first
    val allFolders = remember(availableFolders) {
        val folderNames = availableFolders.map { it.first }
        if (!folderNames.contains("default")) {
            listOf("default") + folderNames.sorted()
        } else {
            listOf("default") + folderNames.filter { it != "default" }.sorted()
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Add New Expense",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Amount field
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Folder selection
            Text(
                text = "Save to folder",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Folder chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allFolders) { folder ->
                    FilterChip(
                        selected = folder == selectedFolder,
                        onClick = { selectedFolder = folder },
                        label = { Text(folder) },
                        leadingIcon = if (folder == selectedFolder) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        onSave(description, selectedFolder, amountValue)
                    },
                    enabled = amount.toDoubleOrNull() != null
                ) {
                    Text("Save")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Bottom sheet for configuring settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    onDismiss: () -> Unit,
    viewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val settingsManager = SettingsManager.getInstance(context)
    val availableFolders by viewModel.availableFolders.collectAsState(initial = emptyList())
    var selectedFolder by remember { mutableStateOf(settingsManager.defaultFolder) }
    val currentFolder by viewModel.currentFolder.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Dialog states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var showDeleteFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf("") }
    
    // Form states
    var newFolderName by remember { mutableStateOf("") }
    var targetMoveFolder by remember { mutableStateOf("default") }
    var shouldDeleteContents by remember { mutableStateOf(false) }
    
    // Error state
    val uiState by viewModel.uiState.collectAsState()
    
    // Create a list of all folders with "default" always being first
    val allFolders = remember(availableFolders) {
        val folderNames = availableFolders.map { it.first }
        if (!folderNames.contains("default")) {
            listOf("default") + folderNames.sorted()
        } else {
            listOf("default") + folderNames.filter { it != "default" }.sorted()
        }
    }
    
    LaunchedEffect(selectedFolder) {
        // Update the settings and the ViewModel when selected folder changes
        settingsManager.defaultFolder = selectedFolder
        viewModel.setCurrentFolder(selectedFolder)
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Folder Management Section
            Text(
                text = "Folder Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Organize your expenses into folders",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Create new folder button
            Button(
                onClick = { 
                    newFolderName = ""
                    showCreateFolderDialog = true 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Folder")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current folder display
            Text(
                text = "Current folder: $currentFolder",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Available folders with action buttons
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = allFolders,
                    key = { it }
                ) { folder ->
                    FolderManagementItem(
                        folderName = folder,
                        isSelected = folder == selectedFolder,
                        onSelect = { 
                            selectedFolder = folder
                            viewModel.setCurrentFolder(folder)
                        },
                        onRename = {
                            if (folder != "default") {
                                folderToRename = folder
                                newFolderName = folder
                                showRenameFolderDialog = true
                            }
                        },
                        onDelete = {
                            if (folder != "default") {
                                folderToDelete = folder
                                shouldDeleteContents = false
                                showDeleteFolderDialog = true
                            }
                        },
                        isDefault = folder == "default",
                        expenseCount = availableFolders.find { it.first == folder }?.second ?: 0
                    )
                }
            }
            
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Default Folder Setting Section
            Text(
                text = "Default Folder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Select the default folder for new expenses",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Folder chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allFolders) { folder ->
                    FilterChip(
                        selected = folder == selectedFolder,
                        onClick = { selectedFolder = folder },
                        label = { Text(folder) },
                        leadingIcon = {
                            if (folder == selectedFolder) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Theme Setting
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Theme options
            val themes = listOf("Light", "Dark", "System")
            var selectedTheme by remember { mutableStateOf(
                when (settingsManager.appTheme) {
                    "light" -> "Light"
                    "dark" -> "Dark"
                    else -> "System"
                }
            ) }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(themes) { theme ->
                    FilterChip(
                        selected = theme == selectedTheme,
                        onClick = { 
                            selectedTheme = theme
                            settingsManager.appTheme = theme.lowercase()
                        },
                        label = { Text(theme) },
                        leadingIcon = {
                            when (theme) {
                                "Light" -> Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = "Light theme",
                                    modifier = Modifier.size(18.dp)
                                )
                                "Dark" -> Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = "Dark theme",
                                    modifier = Modifier.size(18.dp)
                                )
                                else -> Icon(
                                    imageVector = Icons.Default.AutoMode,
                                    contentDescription = "System theme",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Accent Color Setting
            Text(
                text = "Accent Color",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Accent color options
            val accentColors = listOf("Blue", "Green", "Purple", "Red", "Orange")
            var selectedColor by remember { mutableStateOf(
                accentColors.find { it.lowercase() == settingsManager.accentColor } ?: "Blue"
            ) }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(accentColors) { color ->
                    FilterChip(
                        selected = color == selectedColor,
                        onClick = { 
                            selectedColor = color
                            settingsManager.accentColor = color.lowercase()
                        },
                        label = { Text(color) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Close button
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.createFolder(newFolderName)
                            showCreateFolderDialog = false
                        }
                    },
                    enabled = newFolderName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Rename Folder Dialog
    if (showRenameFolderDialog) {
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = false },
            title = { Text("Rename Folder") },
            text = {
                Column {
                    Text(
                        "Current name: $folderToRename",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("New Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.renameFolder(folderToRename, newFolderName)
                            showRenameFolderDialog = false
                        }
                    },
                    enabled = newFolderName.isNotBlank() && newFolderName != folderToRename
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Folder Dialog
    if (showDeleteFolderDialog) {
        val availableTargetFolders = allFolders.filter { it != folderToDelete && it != "default" }
        
        AlertDialog(
            onDismissRequest = { showDeleteFolderDialog = false },
            title = { Text("Delete Folder") },
            text = {
                Column {
                    Text(
                        "Are you sure you want to delete the folder '$folderToDelete'?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = shouldDeleteContents,
                            onCheckedChange = { shouldDeleteContents = it }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = if (shouldDeleteContents) 
                                "Delete all contents" 
                            else 
                                "Move contents to another folder",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    if (!shouldDeleteContents && availableTargetFolders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Target folder selection
                        Text(
                            "Select target folder:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableTargetFolders) { folder ->
                                FilterChip(
                                    selected = folder == targetMoveFolder,
                                    onClick = { targetMoveFolder = folder },
                                    label = { Text(folder) }
                                )
                            }
                        }
                    }
                    
                    if (availableTargetFolders.isEmpty() && !shouldDeleteContents) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Contents will be moved to the default folder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (shouldDeleteContents) {
                                viewModel.deleteFolder(folderToDelete)
                            } else {
                                viewModel.deleteFolder(
                                    folderToDelete, 
                                    moveContentsToFolder = if (availableTargetFolders.isEmpty()) "default" else targetMoveFolder
                                )
                            }
                            showDeleteFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Item for displaying a folder with management actions
 */
@Composable
fun FolderManagementItem(
    folderName: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    isDefault: Boolean,
    expenseCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder icon
            Icon(
                imageVector = if (isDefault) Icons.Default.FolderSpecial else Icons.Default.Folder,
                contentDescription = null,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Folder details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$expenseCount ${if (expenseCount == 1) "expense" else "expenses"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    
                    if (isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Default",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // Action buttons
            if (!isDefault) {
                IconButton(
                    onClick = onRename,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename folder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete folder",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}