package com.example.recordapp.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.recordapp.util.AppImageLoader
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/**
 * A grid item displaying an image with optimized loading and caching
 * Now with enhanced zoom, double-tap for fullscreen, and animation effects
 */
@Composable
fun GridImageItem(
    uri: Uri,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    selectionNumber: Int? = null,
    orderingEnabled: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    isFirst: Boolean = false,
    isLast: Boolean = false
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
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(4.dp)
                .shadow(elevation = if (selectionNumber != null) 4.dp else 2.dp, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (selectionNumber != null) 2.dp else 0.dp,
                    color = if (selectionNumber != null) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .scale(scale)
                .zIndex(if (zoomed) 1f else 0f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { 
                            if (zoomed) {
                                zoomed = false
                            } else {
                                onClick()
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
            // Load image with high resolution
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .crossfade(true)
                    // Use high resolution for clear images
                    .size(width = 800, height = 800)
                    .build(),
                contentDescription = "Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                imageLoader = imageLoader
            )
            
            // Show selection number if selected
            if (selectionNumber != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(
                            color = if (orderingEnabled) 
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f) // Green color for arrangement mode
                            else 
                                MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectionNumber.toString(),
                        color = if (orderingEnabled)
                            MaterialTheme.colorScheme.onTertiary
                        else
                            MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Show Move Up/Down buttons when ordering is enabled
            if (orderingEnabled) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    // Move Up button
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(36.dp),
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
                        modifier = Modifier.size(36.dp),
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
            Text(
                text = "Double-tap for full screen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
} 