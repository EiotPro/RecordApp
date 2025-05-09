package com.example.recordapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.recordapp.R
import com.example.recordapp.model.Expense
import com.example.recordapp.util.animateDeletion
import com.example.recordapp.util.fadeInOut
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExpenseList(
    expenses: List<Expense>,
    onDeleteExpense: (String) -> Unit,
    onUndoDelete: (Expense) -> Unit,
    onExportExpense: (String) -> Unit,
    onMoveExpense: (String, String) -> Unit = { _, _ -> },
    onExpenseClick: (String) -> Unit = {}
) {
    var recentlyDeletedExpense by remember { mutableStateOf<Expense?>(null) }
    var showUndoSnackbar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (expenses.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_expenses),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = expenses,
                key = { it.id }
            ) { expense ->
                var isDeleting by remember { mutableStateOf(false) }
                var showDeleteConfirmation by remember { mutableStateOf(false) }

                Box {
                    ExpenseItem(
                        expense = expense,
                        isDeleting = isDeleting,
                        onDelete = {
                            showDeleteConfirmation = true
                        },
                        onMove = { onMoveExpense(expense.id, expense.folderName) },
                        onClick = { onExpenseClick(expense.id) },
                        modifier = Modifier.animateDeletion(isDeleting) {
                            scope.launch {
                                isDeleting = true
                                delay(500)
                                recentlyDeletedExpense = expense
                                onDeleteExpense(expense.id)
                                showUndoSnackbar = true
                            }
                        }
                    )
                    if (isDeleting) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (showDeleteConfirmation) {
                    ConfirmationDialog(
                        title = stringResource(R.string.delete_expense_title),
                        message = stringResource(R.string.delete_expense_message),
                        onConfirm = {
                            isDeleting = true
                            showDeleteConfirmation = false
                            scope.launch {
                                delay(500)
                                recentlyDeletedExpense = expense
                                onDeleteExpense(expense.id)
                                showUndoSnackbar = true
                            }
                        },
                        onDismiss = {
                            showDeleteConfirmation = false
                        }
                    )
                }
            }
        }
    }

    if (showUndoSnackbar && recentlyDeletedExpense != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = {
                    // Actually restore the expense
                    recentlyDeletedExpense?.let { expense ->
                        onUndoDelete(expense)
                    }
                    showUndoSnackbar = false
                    recentlyDeletedExpense = null
                }) {
                    Text(stringResource(R.string.undo))
                }
            }
        ) {
            Text(stringResource(R.string.expense_deleted))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseItem(
    expense: Expense,
    isDeleting: Boolean = false,
    onDelete: () -> Unit,
    onMove: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Timestamp and Image Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.timestamp,
                        expense.getFormattedTimestamp()
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                // Visual indicator for expenses with/without images
                Icon(
                    imageVector = if (expense.imagePath != null) Icons.Default.Info else Icons.Default.Add,
                    contentDescription = if (expense.imagePath != null) stringResource(R.string.has_image) else stringResource(R.string.no_image_indicator),
                    tint = if (expense.imagePath != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            
            // Display folder name
            if (expense.folderName != "default") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = expense.folderName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Show default folder indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Default Folder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            // Image
            if (expense.imagePath != null) {
                AsyncImage(
                    model = expense.imagePath,
                    contentDescription = "Expense image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_image),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expense.amount > 0) 
                           "₹ %.2f".format(expense.amount) 
                           else stringResource(R.string.details_hidden),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (expense.amount > 0) androidx.compose.ui.text.font.FontWeight.Bold else null,
                    color = if (expense.amount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                
                // Move folder button
                IconButton(onClick = onMove) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Move to folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.fadeInOut(!isDeleting)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
} 