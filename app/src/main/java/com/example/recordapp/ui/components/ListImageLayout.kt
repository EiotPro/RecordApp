package com.example.recordapp.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.recordapp.util.AppImageLoader

/**
 * A component that displays images in a vertical list layout with reordering capabilities
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListImageLayout(
    imageUris: List<Uri>,
    onReorder: (List<Uri>) -> Unit,
    onDelete: (Uri) -> Unit = {},
    modifier: Modifier = Modifier,
    onResetOrder: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember { AppImageLoader.getInstance(context) }
    val listState = rememberLazyListState()
    
    // State for the current order of images
    var currentOrder by remember(imageUris) { 
        mutableStateOf(imageUris.toList())
    }
    
    // Keep original order for reset functionality
    val originalOrder = remember(imageUris) { imageUris.toList() }
    
    // Flag to show success animation
    var showSuccess by remember { mutableStateOf(false) }
    
    // History for undo functionality
    var orderHistory by remember { mutableStateOf(listOf(imageUris.toList())) }
    
    // Function to apply current order
    val applyCurrentOrder = {
        onReorder(currentOrder)
        showSuccess = true
    }
    
    // Hide success animation after 1 second
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(1000)
            showSuccess = false
        }
    }
    
    // Reset order function
    val handleResetOrder = {
        currentOrder = originalOrder
        orderHistory = listOf(originalOrder)
        onResetOrder()
    }
    
    // Undo last move function
    val handleUndo = {
        if (orderHistory.size > 1) {
            // Remove current state and get previous state
            orderHistory = orderHistory.dropLast(1)
            currentOrder = orderHistory.last()
        }
    }
    
    // Function to move an image up in the order
    val moveImageUp = { index: Int ->
        if (index > 0) {
            val newOrder = currentOrder.toMutableList()
            // Swap with previous item
            val temp = newOrder[index]
            newOrder[index] = newOrder[index - 1]
            newOrder[index - 1] = temp
            
            // Save to history before updating
            orderHistory = orderHistory + listOf(newOrder)
            currentOrder = newOrder
        }
    }
    
    // Function to move an image down in the order
    val moveImageDown = { index: Int ->
        if (index < currentOrder.size - 1) {
            val newOrder = currentOrder.toMutableList()
            // Swap with next item
            val temp = newOrder[index]
            newOrder[index] = newOrder[index + 1]
            newOrder[index + 1] = temp
            
            // Save to history before updating
            orderHistory = orderHistory + listOf(newOrder)
            currentOrder = newOrder
        }
    }
    
    // Function to delete an image
    val handleDelete = { uri: Uri ->
        val newOrder = currentOrder.toMutableList()
        newOrder.remove(uri)
        
        // Save to history before updating
        orderHistory = orderHistory + listOf(newOrder)
        currentOrder = newOrder
        
        // Notify parent about deletion
        onDelete(uri)
    }
    
    // Preload high-quality images for all URIs
    LaunchedEffect(imageUris) {
        imageUris.forEach { uri ->
            val request = coil.request.ImageRequest.Builder(context)
                .data(uri)
                .size(400, 400)
                .memoryCacheKey("${uri}_list_view")
                .build()
            
            imageLoader.enqueue(request)
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top action bar with Undo, Reset, and Save buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo button
            IconButton(
                onClick = handleUndo,
                enabled = orderHistory.size > 1
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (orderHistory.size > 1)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            
            // Reset button
            TextButton(onClick = handleResetOrder) {
                Text("Reset Order")
            }
            
            // Save button with success animation
            Button(
                onClick = applyCurrentOrder,
                colors = ButtonColors(
                    containerColor = if (showSuccess) 
                        MaterialTheme.colorScheme.tertiary 
                    else 
                        MaterialTheme.colorScheme.primary,
                    contentColor = if (showSuccess) 
                        MaterialTheme.colorScheme.onTertiary 
                    else 
                        MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Save Order",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Order")
                }
            }
        }
        
        // List of images
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = currentOrder,
                key = { _, uri -> uri.toString() }
            ) { index, uri ->
                // Calculate item properties
                val isFirst = index == 0
                val isLast = index == currentOrder.lastIndex
                
                // Selection number is just the position + 1
                val selectionNumber = index + 1
                
                ListImageItem(
                    uri = uri,
                    selectionNumber = selectionNumber,
                    onMoveUp = { moveImageUp(index) },
                    onMoveDown = { moveImageDown(index) },
                    onImageClick = { /* We'll use onDelete for backward compatibility */ handleDelete(uri) },
                    isFirst = isFirst,
                    isLast = isLast,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
} 