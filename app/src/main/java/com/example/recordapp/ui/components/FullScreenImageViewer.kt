package com.example.recordapp.ui.components

import android.net.Uri
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Full screen image viewer with zoom, rotate, and pan capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageViewer(
    uri: Uri,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // State for zoom, pan and rotation
        var scale by remember { mutableStateOf(1f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        var rotation by remember { mutableStateOf(0f) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                // Image with gesture detection
                AsyncImage(
                    model = uri,
                    contentDescription = "Fullscreen Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rotationChange ->
                                // Apply zoom with limits
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                
                                // Apply pan with scaled offset
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                    
                                    // Reset offset when scale is 1
                                    if (scale <= 1.01f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                                
                                // Apply rotation (optional, can be disabled)
                                // rotation += rotationChange
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                            rotationZ = rotation
                        ),
                    contentScale = ContentScale.Fit
                )
                
                // Zoom instructions
                if (scale <= 1f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Pinch to zoom • Pan to move • Double tap to reset",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}