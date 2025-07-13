package com.example.recordapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.recordapp.R
import com.example.recordapp.model.Expense
import com.example.recordapp.ui.components.FullScreenImageViewer
import com.example.recordapp.util.AppImageLoader
import com.example.recordapp.viewmodel.ExpenseViewModel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: String,
    viewModel: ExpenseViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expense by viewModel.getExpenseById(expenseId).collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expense_details)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            expense?.let { exp ->
                                if (exp.imagePath != null) {
                                    try {
                                        // Use FileProvider to get correct URI
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_STREAM, exp.imagePath)
                                            type = "image/jpeg"
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_image)))
                                    } catch (e: Exception) {
                                        // Fallback to PDF if sharing fails
                                        scope.launch {
                                            viewModel.generateSingleExpensePdf(expenseId) { pdfFile ->
                                                pdfFile?.let { file ->
                                                    val intent = viewModel.getViewPdfIntent(file)
                                                    context.startActivity(intent)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Fallback to PDF if no image is available
                                    scope.launch {
                                        viewModel.generateSingleExpensePdf(expenseId) { pdfFile ->
                                            pdfFile?.let { file ->
                                                val intent = viewModel.getViewPdfIntent(file)
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.share_image)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            expense?.let { exp ->
                ExpenseDetails(expense = exp, viewModel = viewModel)
            } ?: run {
                // Show loading or not found message
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Show loading indicator if needed
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Show error message if any
            if (!uiState.errorMessage.isNullOrBlank()) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(uiState.errorMessage.orEmpty())
                }
            }
        }
    }
}

@Composable
fun ExpenseDetails(expense: Expense, viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State for full screen viewing
    var showFullScreenViewer by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Timestamp
        Text(
            text = stringResource(R.string.timestamp, expense.getFormattedTimestamp()),
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Folder
        if (expense.folderName != "default") {
            Text(
                text = "Folder: ${expense.folderName}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Serial Number
        if (expense.serialNumber.isNotBlank()) {
            Text(
                text = "Serial Number: ${expense.serialNumber}",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Amount 
        if (expense.amount > 0) {
            Text(
                text = expense.getFormattedAmount(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Description
        if (expense.description.isNotBlank()) {
            Text(
                text = stringResource(R.string.description),
                style = MaterialTheme.typography.titleSmall
            )
            
            Text(
                text = expense.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Image
        if (expense.imagePath != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clickable { showFullScreenViewer = true }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(expense.imagePath)
                            .memoryCacheKey("${expense.imagePath?.toString()}-${System.currentTimeMillis()}")
                            .diskCacheKey("${expense.imagePath?.toString()}-${System.currentTimeMillis()}")
                            .build(),
                        contentDescription = "Expense image",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
                
                // Image Rotation Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotate Left Button
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                viewModel.rotateExpenseImage(expense.id, -90f)
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                            contentDescription = "Rotate Left",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(64.dp))
                    
                    // Rotate Right Button
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                viewModel.rotateExpenseImage(expense.id, 90f)
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate Right",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Show full screen viewer when requested
            if (showFullScreenViewer) {
                FullScreenImageViewer(
                    uri = expense.imagePath!!,
                    onDismiss = { showFullScreenViewer = false }
                )
            }
        }
    }
} 