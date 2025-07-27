package com.example.recordapp.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import com.example.recordapp.ui.components.common.ViewModeToggle



/**
 * A component that allows switching between paginated grid and list layouts for image reordering
 */
@Composable
fun SwitchablePaginatedLayout(
    images: Flow<PagingData<Uri>>,
    onImageClick: (Uri) -> Unit = {},
    onImagesReordered: (List<Uri>) -> Unit = {},
    onDelete: (Uri) -> Unit = {},
    modifier: Modifier = Modifier,
    showViewModeToggle: Boolean = false,
    isGridView: Boolean? = null
) {
    // State to track the current view mode
    var internalIsGridView by remember { mutableStateOf(true) }
    
    // Use external state if provided, otherwise use internal state
    val effectiveIsGridView = isGridView ?: internalIsGridView
    
    Column(modifier = modifier.fillMaxSize()) {
        // View mode toggle (optional, can now be shown in the AppBar)
        if (showViewModeToggle) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewModeToggle(
                    isGridView = effectiveIsGridView,
                    onViewModeChange = { 
                        if (isGridView == null) {
                            internalIsGridView = it
                        }
                        // If external state is provided, changes are handled by parent
                    }
                )
            }
            
            // Add a subtle divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
        
        // Always show reordering controls in both views
        if (effectiveIsGridView) {
            PaginatedImageGrid(
                images = images,
                onImageClick = onImageClick,
                orderingEnabled = true,
                onImagesReordered = onImagesReordered,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PaginatedListImageLayout(
                images = images,
                onImagesReordered = onImagesReordered,
                onDelete = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
} 