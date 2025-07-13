package com.example.recordapp.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.recordapp.util.AppImageLoader
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.scale
import androidx.compose.ui.composed
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextAlign

/**
 * A list item displaying an image with reordering controls in a horizontal row layout
 * Now supports clicking on the image to assign an order number and zoom feature
 */
@Composable
fun ListImageItem(
    uri: Uri,
    selectionNumber: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onImageClick: () -> Unit = {},
    customSelectionNumber: Int? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = AppImageLoader.getInstance(context)
    
    // Zoom state
    var zoomed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (zoomed) 2.0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    // Animate the height when zoomed
    val rowHeight by animateDpAsState(
        targetValue = if (zoomed) 180.dp else 120.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    // Full screen image dialog
    var showFullScreenImage by remember { mutableStateOf(false) }
    
    // State to track instruction visibility
    var showInstruction by remember { mutableStateOf(false) }
    
    // When zoomed changes, show instructions briefly
    LaunchedEffect(zoomed) {
        if (zoomed) {
            showInstruction = true
            kotlinx.coroutines.delay(3000)
            showInstruction = false
        }
    }
    
    // Show the enhanced full screen image viewer
    if (showFullScreenImage) {
        FullScreenImageViewer(
            uri = uri,
            onDismiss = { showFullScreenImage = false }
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(rowHeight)
            .shadow(elevation = if (customSelectionNumber != null) 4.dp else 2.dp, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (customSelectionNumber != null) 2.dp else 1.dp,
                color = if (customSelectionNumber != null) 
                    MaterialTheme.colorScheme.tertiary 
                else 
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image thumbnail with click for ordering and zooming
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .aspectRatio(1.5f)  // Make the image wider to match grid layout aspect
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .scale(scale)
                .zIndex(if (zoomed) 1f else 0f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (zoomed) {
                                zoomed = false
                            } else {
                                onImageClick()
                            }
                        },
                        onLongPress = { 
                            zoomed = !zoomed
                        },
                        onDoubleTap = {
                            showFullScreenImage = true
                        }
                    )
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .crossfade(true)
                    .size(width = 800, height = 800)  // Higher resolution just like in GridImageItem
                    .build(),
                contentDescription = "Image Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                imageLoader = imageLoader
            )
            
            // If there's a custom selection number, show it on the image
            customSelectionNumber?.let { number ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Right side content
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            // Position number in list
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (customSelectionNumber != null) 
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                        else 
                            MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectionNumber.toString(),
                    color = if (customSelectionNumber != null) 
                        MaterialTheme.colorScheme.onTertiary
                    else 
                        MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Move Up button
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.size(40.dp),
                    enabled = !isFirst
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        tint = if (isFirst) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
                
                // Move Down button
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(40.dp),
                    enabled = !isLast
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        tint = if (isLast) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // Show instructions toast when zoomed
    AnimatedVisibility(
        visible = showInstruction && zoomed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Double-tap to view full screen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Extension to handle both click and long click
fun Modifier.longClickable(onLongClick: () -> Unit) = composed {
    val context = LocalContext.current
    var startTime by remember { mutableStateOf(0L) }
    
    this.pointerInput(Unit) {
        detectTapGestures(
            onPress = { startTime = System.currentTimeMillis() },
            onTap = {
                val duration = System.currentTimeMillis() - startTime
                if (duration > 500) { // Long press threshold (500ms)
                    onLongClick()
                }
            }
        )
    }
} 