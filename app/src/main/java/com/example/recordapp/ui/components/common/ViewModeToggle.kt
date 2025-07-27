package com.example.recordapp.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A reusable toggle component for switching between grid and list views
 * Consolidated from multiple duplicate implementations across the app
 */
@Composable
fun ViewModeToggle(
    isGridView: Boolean,
    onViewModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    style: ViewModeToggleStyle = ViewModeToggleStyle.Compact
) {
    when (style) {
        ViewModeToggleStyle.Compact -> CompactViewModeToggle(
            isGridView = isGridView,
            onViewModeChange = onViewModeChange,
            modifier = modifier
        )
        ViewModeToggleStyle.IconButtons -> IconButtonViewModeToggle(
            isGridView = isGridView,
            onViewModeChange = onViewModeChange,
            modifier = modifier
        )
    }
}

/**
 * Compact surface-based toggle
 */
@Composable
private fun CompactViewModeToggle(
    isGridView: Boolean,
    onViewModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grid view button
            IconButton(
                onClick = { onViewModeChange(true) },
                modifier = Modifier
                    .size(36.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Rounded.GridView else Icons.Outlined.GridView,
                    contentDescription = "Grid View",
                    tint = if (isGridView) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // List view button
            IconButton(
                onClick = { onViewModeChange(false) },
                modifier = Modifier
                    .size(36.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (!isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.AutoMirrored.Outlined.ViewList,
                    contentDescription = "List View",
                    tint = if (!isGridView) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Icon toggle button style
 */
@Composable
private fun IconButtonViewModeToggle(
    isGridView: Boolean,
    onViewModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Grid view toggle button
        IconToggleButton(
            checked = isGridView,
            onCheckedChange = { onViewModeChange(true) }
        ) {
            Icon(
                imageVector = Icons.Filled.GridView,
                contentDescription = "Grid View",
                tint = if (isGridView)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // List view toggle button
        IconToggleButton(
            checked = !isGridView,
            onCheckedChange = { onViewModeChange(false) }
        ) {
            Icon(
                imageVector = Icons.Filled.ViewList,
                contentDescription = "List View",
                tint = if (!isGridView)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Style options for the view mode toggle
 */
enum class ViewModeToggleStyle {
    Compact,      // Surface-based compact toggle
    IconButtons   // Separate icon toggle buttons
}
