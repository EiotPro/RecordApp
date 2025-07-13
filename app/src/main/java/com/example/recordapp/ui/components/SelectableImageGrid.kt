package com.example.recordapp.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.recordapp.util.AppImageLoader

/**
 * A grid of images that can be reordered using Move Up/Down buttons
 * 
 * This component is kept as a reference implementation for non-paginated image grids.
 * Currently, the app uses PaginatedImageGrid for image management.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("unused")
fun SelectableImageGrid(
    imageUris: List<Uri>,
    onReorder: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    onResetOrder: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember { AppImageLoader.getInstance(context) }
    val gridState = rememberLazyGridState()
    
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
    
    // Preload high-quality images for all URIs
    LaunchedEffect(imageUris) {
        imageUris.forEach { uri ->
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(800, 800) // Much larger thumbnail size for better quality
                .memoryCacheKey("${uri}_high_quality")
                .build()
            
            imageLoader.enqueue(request)
        }
    }
    
    Column(modifier = modifier) {
        // Success message
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
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
        
        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Use arrows to change image order",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Undo button
            IconButton(
                onClick = handleUndo,
                enabled = orderHistory.size > 1
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo Last Move",
                    tint = if (orderHistory.size > 1) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            
            // Reset button
            Button(
                onClick = handleResetOrder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Reset Order",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Order")
                }
            }
            
            // Save button
            Button(
                onClick = applyCurrentOrder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
        
        // Create a grid of images
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(8.dp),
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
                
                // Visual properties based on selection
                val borderColor = MaterialTheme.colorScheme.primary
                val elevation = 2f
                val scale = 1f
                
                GridImageItem(
                    uri = uri,
                    onClick = { /* Single click does nothing now */ },
                    selectionNumber = selectionNumber,
                    orderingEnabled = true,
                    onMoveUp = { moveImageUp(index) },
                    onMoveDown = { moveImageDown(index) },
                    isFirst = isFirst,
                    isLast = isLast,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                )
            }
        }
    }
} 