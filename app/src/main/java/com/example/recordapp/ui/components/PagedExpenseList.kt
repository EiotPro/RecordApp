package com.example.recordapp.ui.components
import androidx.compose.ui.composed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.recordapp.R
import com.example.recordapp.model.Expense
import com.example.recordapp.util.animateDeletion
import com.example.recordapp.util.fadeInOut
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

@Composable
fun PagedExpenseList(
    expenses: Flow<PagingData<Expense>>,
    onDeleteExpense: (String) -> Unit,
    onUndoDelete: (Expense) -> Unit,
    onMoveExpense: (String, String) -> Unit = { _, _ -> },
    onExpenseClick: (String) -> Unit = {}
) {
    val lazyExpenseItems: LazyPagingItems<Expense> = expenses.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    
    // Track expense to delete with confirmation dialog
    val expenseToDelete = remember { mutableStateOf<String?>(null) }
    val isDeleting = remember { mutableStateOf(false) }
    val showUndoSnackbar = remember { mutableStateOf(false) }
    val deletedExpense = remember { mutableStateOf<Expense?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // Show loading state for initial load
            lazyExpenseItems.loadState.refresh is LoadState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Show error message if initial load fails
            lazyExpenseItems.loadState.refresh is LoadState.Error -> {
                val error = (lazyExpenseItems.loadState.refresh as LoadState.Error).error
                Text(
                    text = "Error: ${error.localizedMessage ?: "Unknown error"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Show empty state if no items
            lazyExpenseItems.itemCount == 0 -> {
                Text(
                    text = stringResource(R.string.no_expenses),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Show the list
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        count = lazyExpenseItems.itemCount,
                        key = lazyExpenseItems.itemKey { it.id }
                    ) { index ->
                        val expense = lazyExpenseItems[index]
                        expense?.let {
                            ExpenseItem(
                                expense = it,
                                onDelete = { 
                                    deletedExpense.value = it
                                    expenseToDelete.value = it.id 
                                },
                                onMove = { onMoveExpense(it.id, it.folderName) },
                                onClick = { onExpenseClick(it.id) }
                            )
                        }
                    }
                    
                    // Show loading state for append (when scrolling down)
                    item {
                        if (lazyExpenseItems.loadState.append is LoadState.Loading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Show confirmation dialog when expenseToDelete has a value
    expenseToDelete.value?.let { expenseId ->
        ConfirmationDialog(
            title = stringResource(R.string.delete_expense_title),
            message = stringResource(R.string.delete_expense_message),
            onConfirm = {
                isDeleting.value = true
                expenseToDelete.value = null
                scope.launch {
                    delay(500) // Simulate deletion delay
                    onDeleteExpense(expenseId)
                    showUndoSnackbar.value = true
                }
            },
            onDismiss = {
                expenseToDelete.value = null
            }
        )
    }
    
    // Show undo snackbar
    if (showUndoSnackbar.value && deletedExpense.value != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { 
                    // Actually restore the expense
                    deletedExpense.value?.let { expense ->
                        onUndoDelete(expense)
                    }
                    showUndoSnackbar.value = false
                    deletedExpense.value = null
                }) {
                    Text(stringResource(R.string.undo))
                }
            }
        ) {
            Text(stringResource(R.string.expense_deleted))
        }
    }
} 