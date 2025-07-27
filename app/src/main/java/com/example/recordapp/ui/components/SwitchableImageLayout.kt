package com.example.recordapp.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.recordapp.ui.components.common.ViewModeToggle
import com.example.recordapp.ui.components.common.ViewModeToggleStyle

/**
 * A layout that allows switching between grid and list views for image reordering
 */
@Composable
fun SwitchableImageLayout(
    imageUris: List<Uri>,
    onReorder: (List<Uri>) -> Unit,
    onDelete: (Uri) -> Unit = {},
    modifier: Modifier = Modifier,
    onResetOrder: () -> Unit
) {
    // State to track the current view mode
    var isGridView by remember { mutableStateOf(true) }
    
    Column(modifier = modifier.fillMaxSize()) {
        // View mode toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "View mode:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )

            ViewModeToggle(
                isGridView = isGridView,
                onViewModeChange = { isGridView = it },
                style = ViewModeToggleStyle.IconButtons
            )
        }
        
        // Display the appropriate layout based on the selected view mode
        if (isGridView) {
            SelectableImageGrid(
                imageUris = imageUris,
                onReorder = onReorder,
                onResetOrder = onResetOrder,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ListImageLayout(
                imageUris = imageUris,
                onReorder = onReorder,
                onDelete = onDelete,
                onResetOrder = onResetOrder,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
} 