package com.example.recordapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.recordapp.model.Expense
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

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
    // Context is not directly used in this function but keeping it for consistency
    // with other widget functions and potential future use
    
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
                    recentExpenses.forEach { expense ->
                        TransactionItem(
                            expense = expense,
                            onClick = { 
                                try {
                                    onExpenseClick(expense.id)
                                } catch (e: Exception) {
                                    // Handle click error silently
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // View all button
                TextButton(
                    onClick = { 
                        try {
                            onViewAllClick()
                        } catch (e: Exception) {
                            // Handle click error silently
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("View All")
                }
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
    // This variable is used when getting the formatted amount
    SettingsManager.getInstance(context)
    
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
                    text = expense.description.ifBlank { "Expense" },
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
    // This variable is used below
    SettingsManager.getInstance(context)
    
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
 * Bottom sheet for settings
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
        try {
            settingsManager.defaultFolder = selectedFolder
            viewModel.setCurrentFolder(selectedFolder)
        } catch (e: Exception) {
            // Handle setting update error silently
        }
    }
    
    // Create folder dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                Column {
                    Text("Enter a name for the new folder:")
                    Spacer(modifier = Modifier.height(8.dp))
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
                        if (newFolderName.isNotBlank()) {
                            scope.launch {
                                viewModel.createFolder(newFolderName)
                                newFolderName = ""
                                showCreateFolderDialog = false
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Rename folder dialog
    if (showRenameFolderDialog) {
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = false },
            title = { Text("Rename Folder") },
            text = {
                Column {
                    Text("Enter a new name for '$folderToRename':")
                    Spacer(modifier = Modifier.height(8.dp))
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
                        if (newFolderName.isNotBlank() && newFolderName != folderToRename) {
                            scope.launch {
                                viewModel.renameFolder(folderToRename, newFolderName)
                                newFolderName = ""
                                folderToRename = ""
                                showRenameFolderDialog = false
                            }
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRenameFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete folder dialog
    if (showDeleteFolderDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteFolderDialog = false },
            title = { Text("Delete Folder") },
            text = {
                Column {
                    Text("What would you like to do with the contents of '$folderToDelete'?")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Option 1: Delete contents
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { shouldDeleteContents = true }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = shouldDeleteContents,
                            onClick = { shouldDeleteContents = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Delete all contents",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "All expenses in this folder will be permanently deleted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Option 2: Move contents to another folder
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { shouldDeleteContents = false }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = !shouldDeleteContents,
                            onClick = { shouldDeleteContents = false }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Move contents to another folder",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Only show dropdown if "Move to" is selected
                            if (!shouldDeleteContents) {
                                val moveToOptions = allFolders.filter { it != folderToDelete }
                                ExposedDropdownMenuBox(
                                    expanded = false,
                                    onExpandedChange = { },
                                ) {
                                    Column {
                                        Text(
                                            text = "Select destination folder:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Simple chip selection instead of dropdown
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(moveToOptions) { folder ->
                                                FilterChip(
                                                    selected = folder == targetMoveFolder,
                                                    onClick = { targetMoveFolder = folder },
                                                    label = { Text(folder) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (shouldDeleteContents) {
                                viewModel.deleteFolder(folderToDelete, null)
                            } else {
                                viewModel.deleteFolder(folderToDelete, targetMoveFolder)
                            }
                            folderToDelete = ""
                            targetMoveFolder = "default"
                            shouldDeleteContents = false
                            showDeleteFolderDialog = false
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
            
            // Default folder section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Default Folder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                TextButton(
                    onClick = { 
                        newFolderName = ""
                        showCreateFolderDialog = true 
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "Create Folder",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create New")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Default folder chips
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Folder Management Section
            Text(
                text = "Folder Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Folder list with management options
            availableFolders.forEach { (folder, count) ->
                FolderManagementItem(
                    folderName = folder,
                    isSelected = folder == currentFolder,
                    onSelect = { 
                        selectedFolder = folder 
                        viewModel.setCurrentFolder(folder)
                    },
                    onRename = {
                        folderToRename = folder
                        newFolderName = folder
                        showRenameFolderDialog = true
                    },
                    onDelete = {
                        folderToDelete = folder
                        targetMoveFolder = if (allFolders.contains("default") && folder != "default") {
                            "default"
                        } else {
                            allFolders.firstOrNull { it != folder } ?: ""
                        }
                        showDeleteFolderDialog = true
                    },
                    isDefault = folder == "default",
                    expenseCount = count
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Image Compression Setting
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Image Compression Section
            var imageCompression by remember { mutableFloatStateOf(settingsManager.imageCompression.toFloat()) }
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Image Compression",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Adjust image quality when saving (higher = better quality but larger files)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Low",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Slider(
                        value = imageCompression,
                        onValueChange = { 
                            imageCompression = it
                            settingsManager.imageCompression = it.toInt()
                        },
                        valueRange = 20f..100f,
                        steps = 8,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = "High",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "${imageCompression.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            
            // Buttons at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
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