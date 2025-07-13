package com.example.recordapp.util

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.recordapp.util.AppImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// Constants for image preloading
private const val PREFETCH_DISTANCE = 5        // Start preloading when within 5 items
private const val VISIBLE_BUFFER_SIZE = 10     // Load 10 items ahead in visible direction

/**
 * Manager for preloading images intelligently based on scroll state
 */
class ImagePreloadManager(
    private val context: Context,
    private val preloadDistance: Int = 5,
    private val preloadScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val imageLoader: ImageLoader = AppImageLoader.getInstance(context)
    private val activePreloadJobs = mutableSetOf<String>()
    
    // Maps to track loading state of images
    private val loadingImages = mutableSetOf<String>()
    private val preloadedImages = mutableSetOf<String>()
    
    /**
     * Preload images with priority based on visibility
     * @param imageUris List of all image URIs in the collection
     * @param currentIndex Current visible index
     * @param isScrollingForward Direction of scrolling
     */
    suspend fun preloadImagesWithPriority(
        imageUris: List<Uri>,
        currentIndex: Int,
        isScrollingForward: Boolean
    ) {
        withContext(Dispatchers.IO) {
            // Priority 1: Current visible item and immediate neighbors
            val visibleRange = if (isScrollingForward) {
                currentIndex..(currentIndex + 2).coerceAtMost(imageUris.lastIndex)
            } else {
                (currentIndex - 2).coerceAtLeast(0)..currentIndex
            }
            
            // Priority 2: Items in the direction of scrolling (buffer zone)
            val bufferRange = if (isScrollingForward) {
                (currentIndex + 3).coerceAtMost(imageUris.lastIndex)..
                    (currentIndex + VISIBLE_BUFFER_SIZE).coerceAtMost(imageUris.lastIndex)
            } else {
                (currentIndex - VISIBLE_BUFFER_SIZE).coerceAtLeast(0)..
                    (currentIndex - 3).coerceAtLeast(0)
            }
            
            // Load visible items first (high priority)
            visibleRange.forEach { index ->
                if (index in imageUris.indices) {
                    val uri = imageUris[index]
                    preloadImageWithPriority(uri, isHighPriority = true)
                }
            }
            
            // Then load buffer items (medium priority)
            bufferRange.forEach { index ->
                if (index in imageUris.indices) {
                    val uri = imageUris[index]
                    preloadImageWithPriority(uri, isHighPriority = false)
                }
            }
        }
    }
    
    /**
     * Preload a single image with priority
     */
    private fun preloadImageWithPriority(uri: Uri, isHighPriority: Boolean) {
        val uriString = uri.toString()
        
        // Skip if already loaded or in progress
        if (uriString in preloadedImages || uriString in loadingImages) return
        
        loadingImages.add(uriString)
        
        // Create appropriate request based on priority
        val request = ImageRequest.Builder(context)
            .data(uri)
            .listener(
                onSuccess = { _, _ ->
                    loadingImages.remove(uriString)
                    preloadedImages.add(uriString)
                },
                onError = { _, _ ->
                    loadingImages.remove(uriString)
                }
            )
            .apply {
                if (isHighPriority) {
                    // High priority: full resolution with unique memory cache key
                    memoryCacheKey("${uriString}_fullres")
                    scale(coil.size.Scale.FILL) // Ensure proper scaling
                } else {
                    // Medium priority: smaller size for buffer zone with unique memory cache key
                    memoryCacheKey("${uriString}_thumbnail")
                    size(250, 250)
                    scale(coil.size.Scale.FILL) // Ensure proper scaling
                }
            }
            .build()
        
        imageLoader.enqueue(request)
    }
    
    /**
     * Clear preload tracking information
     */
    fun clearPreloadCache() {
        preloadedImages.clear()
        loadingImages.clear()
    }
}

/**
 * Hook up the ImagePreloadManager to a LazyGridState
 */
@Composable
fun rememberGridPreloader(
    gridState: LazyGridState,
    imageUris: List<Uri>
): ImagePreloadManager {
    val context = LocalContext.current
    val preloadManager = remember { ImagePreloadManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Track first visible item for preloading
    val firstVisibleItemIndex = remember { derivedStateOf { gridState.firstVisibleItemIndex } }
    var lastIndex by remember { mutableStateOf(-1) }
    var isScrollingForward by remember { mutableStateOf(true) }
    
    // React to scroll changes
    LaunchedEffect(firstVisibleItemIndex.value, imageUris) {
        val currentIndex = firstVisibleItemIndex.value
        
        // Determine scroll direction
        isScrollingForward = currentIndex >= lastIndex
        lastIndex = currentIndex
        
        // Check if we should trigger preloading
        val shouldPreload = currentIndex >= 0 && 
            ((isScrollingForward && currentIndex + PREFETCH_DISTANCE < imageUris.size) || 
             (!isScrollingForward && currentIndex - PREFETCH_DISTANCE >= 0))
            
        if (shouldPreload && imageUris.isNotEmpty()) {
            coroutineScope.launch {
                preloadManager.preloadImagesWithPriority(
                    imageUris = imageUris,
                    currentIndex = currentIndex,
                    isScrollingForward = isScrollingForward
                )
            }
        }
    }
    
    // Clear cache when uri list changes completely
    DisposableEffect(imageUris) {
        onDispose {
            preloadManager.clearPreloadCache()
        }
    }
    
    return preloadManager
}

/**
 * Hook up the ImagePreloadManager to a LazyListState
 */
@Composable
fun rememberListPreloader(
    listState: LazyListState,
    imageUris: List<Uri>
): ImagePreloadManager {
    val context = LocalContext.current
    val preloadManager = remember { ImagePreloadManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Track first visible item for preloading
    val firstVisibleItemIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }
    var lastIndex by remember { mutableStateOf(-1) }
    var isScrollingForward by remember { mutableStateOf(true) }
    
    // React to scroll changes
    LaunchedEffect(firstVisibleItemIndex.value, imageUris) {
        val currentIndex = firstVisibleItemIndex.value
        
        // Determine scroll direction
        isScrollingForward = currentIndex >= lastIndex
        lastIndex = currentIndex
        
        // Check if we should trigger preloading
        val shouldPreload = currentIndex >= 0 && 
            ((isScrollingForward && currentIndex + PREFETCH_DISTANCE < imageUris.size) || 
             (!isScrollingForward && currentIndex - PREFETCH_DISTANCE >= 0))
            
        if (shouldPreload && imageUris.isNotEmpty()) {
            coroutineScope.launch {
                preloadManager.preloadImagesWithPriority(
                    imageUris = imageUris,
                    currentIndex = currentIndex,
                    isScrollingForward = isScrollingForward
                )
            }
        }
    }
    
    // Clear cache when uri list changes completely
    DisposableEffect(imageUris) {
        onDispose {
            preloadManager.clearPreloadCache()
        }
    }
    
    return preloadManager
} 