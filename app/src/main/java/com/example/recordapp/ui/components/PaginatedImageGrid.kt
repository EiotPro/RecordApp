package com.example.recordapp.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow

/**
 * A paginated grid of images that efficiently loads only visible images
 * and supports pagination for large collections.
 * Reordering functionality is available but delete is only in list view.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaginatedImageGrid(
    images: Flow<PagingData<Uri>>,
    onImageClick: (Uri) -> Unit = {},
    columns: Int = 2,
    modifier: Modifier = Modifier,
    orderingEnabled: Boolean = false,
    onImagesReordered: (List<Uri>) -> Unit = {}
) {
    val context = LocalContext.current
    val lazyImageItems = images.collectAsLazyPagingItems()
    
    // Build list of visible images
    val visibleImageUris = remember(lazyImageItems.itemCount) {
        buildList {
            for (i in 0 until lazyImageItems.itemCount) {
                lazyImageItems[i]?.let { add(it) }
            }
        }
    }
    
    // State for the current order of images
    var currentOrder by remember(visibleImageUris) { 
        mutableStateOf(visibleImageUris.toList())
    }
    
    // Keep original order for reset functionality
    val originalOrder = remember(visibleImageUris) { visibleImageUris.toList() }
    
    // Selected order indices (maps URI to order number)
    var selectedOrderIndices by remember { mutableStateOf<Map<Uri, Int>>(emptyMap()) }
    
    // Use this to track the next order number to assign
    var nextOrderNumber by remember { mutableStateOf(1) }
    
    // Flag to track if there are unsaved changes
    var hasChanges by remember { mutableStateOf(false) }
    
    var showSuccess by remember { mutableStateOf(false) }
    
    // History for undo functionality
    var orderHistory by remember { mutableStateOf(listOf(visibleImageUris.toList())) }
    
    // State to track if the initial instruction has been shown
    var showInitialInstruction by remember { mutableStateOf(true) }
    
    // Auto-save whenever order changes
    LaunchedEffect(currentOrder) {
        // Skip the initial setting of currentOrder
        if (currentOrder != originalOrder || orderHistory.size > 1) {
            onImagesReordered(currentOrder)
            showSuccess = true
        }
    }
    
    // Hide success animation after 1 second
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(1000)
            showSuccess = false
        }
    }
    
    // Hide instruction after 5 seconds
    LaunchedEffect(Unit) {
        if (showInitialInstruction) {
            kotlinx.coroutines.delay(5000)
            showInitialInstruction = false
        }
    }
    
    // Reset order function
    val handleResetOrder = {
        // Reset to original order
        currentOrder = originalOrder
        orderHistory = listOf(originalOrder)
        // Auto-save will be triggered by the LaunchedEffect
    }
    
    // Undo last move function
    val handleUndo = {
        if (orderHistory.size > 1) {
            // Remove current state and get previous state
            orderHistory = orderHistory.dropLast(1)
            currentOrder = orderHistory.last()
            // Auto-save will be triggered by the LaunchedEffect
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
            // Auto-save will be triggered by the LaunchedEffect
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
            // Auto-save will be triggered by the LaunchedEffect
        }
    }
    
    // Function to handle image click for ordering
    val handleImageClick = { uri: Uri ->
        if (orderingEnabled) {
            selectedOrderIndices = if (selectedOrderIndices.containsKey(uri)) {
                // If already selected, remove it (toggle behavior)
                val updatedMap = selectedOrderIndices.toMutableMap()
                val removedNumber = updatedMap.remove(uri) ?: 0
                
                // Adjust all higher numbers down by 1
                updatedMap.entries.forEach { (key, value) ->
                    if (value > removedNumber) {
                        updatedMap[key] = value - 1
                    }
                }
                
                // Adjust next order number
                nextOrderNumber = updatedMap.values.maxOrNull()?.plus(1) ?: 1
                
                updatedMap
            } else {
                // Add to selection with next order number
                val updatedMap = selectedOrderIndices.toMutableMap()
                updatedMap[uri] = nextOrderNumber
                nextOrderNumber++
                updatedMap
            }
            
            // If we have any selections, rearrange based on the selection
            if (selectedOrderIndices.isNotEmpty()) {
                // Divide into selected and unselected
                val selectedImages = selectedOrderIndices.entries
                    .sortedBy { it.value }
                    .map { it.key }
                    
                val unselectedImages = currentOrder.filter { uri ->
                    !selectedOrderIndices.containsKey(uri)
                }
                
                // Create new order with selected items first, then unselected
                val newOrder = selectedImages + unselectedImages
                currentOrder = newOrder
                hasChanges = true
            }
        } else {
            // In non-ordering mode, just pass through the normal onClick
            onImageClick(uri)
        }
    }
    
    // Function to clear selection
    val handleClearSelection = {
        selectedOrderIndices = emptyMap()
        nextOrderNumber = 1
        // Don't reset the order when clearing selection
    }

    // Function to save changes
    val handleSaveChanges = {
        onImagesReordered(currentOrder)
        showSuccess = true
        hasChanges = false
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Show loading state for initial load
        if (lazyImageItems.loadState.refresh is LoadState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Show error state
        lazyImageItems.loadState.refresh.let { loadState ->
            if (loadState is LoadState.Error) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error loading images: ${loadState.error.localizedMessage}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Success message
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Image order updated!",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Action buttons for manual saving and clearing selection
        AnimatedVisibility(
            visible = (orderingEnabled && (hasChanges || selectedOrderIndices.isNotEmpty())),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (selectedOrderIndices.isNotEmpty()) {
                    // Clear selection button
                    OutlinedButton(
                        onClick = handleClearSelection,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear Selection"
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Clear Selection")
                    }
                }
                
                if (hasChanges) {
                    // Save changes button
                    Button(
                        onClick = handleSaveChanges
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Save Order"
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Save Order")
                    }
                }
                
                if (orderHistory.size > 1) {
                    // Undo button
                    OutlinedButton(
                        onClick = handleUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo"
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Undo")
                    }
                }
            }
        }
        
        // Main content - image grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (orderingEnabled) {
                // When ordering is enabled, use the current order list
                itemsIndexed(
                    items = currentOrder,
                    key = { _, uri -> uri.toString() }
                ) { index, uri ->
                    // Calculate item properties
                    val isFirst = index == 0
                    val isLast = index == currentOrder.lastIndex
                    
                    // Get custom selection number if assigned
                    val customSelectionNumber = selectedOrderIndices[uri]
                    
                    GridImageItem(
                        uri = uri,
                        onClick = { handleImageClick(uri) },
                        selectionNumber = customSelectionNumber ?: index + 1,
                        orderingEnabled = customSelectionNumber != null,
                        onMoveUp = { moveImageUp(index) },
                        onMoveDown = { moveImageDown(index) },
                        isFirst = isFirst,
                        isLast = isLast,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    )
                }
            } else {
                // When not ordering, display images normally
                items(
                    count = lazyImageItems.itemCount,
                    key = { index -> lazyImageItems[index]?.toString() ?: index.toString() }
                ) { index ->
                    val imageUri = lazyImageItems[index]
                    imageUri?.let { uri ->
                        GridImageItem(
                            uri = uri,
                            onClick = { onImageClick(uri) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Show loading state for append (when scrolling down)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (lazyImageItems.loadState.append is LoadState.Loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
        
        // Initial instruction toast - only shows for 5 seconds then disappears
        AnimatedVisibility(
            visible = showInitialInstruction && orderingEnabled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Tap on images to assign order numbers (1, 2, 3, ...)\nLong-press to zoom, double-tap for full screen",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
} 