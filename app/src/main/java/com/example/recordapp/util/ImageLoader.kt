package com.example.recordapp.util

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Optimized ImageLoader for the application
 * Provides enhanced caching, memory management, and performance optimizations.
 * Use this singleton for all image loading operations throughout the app.
 */
object AppImageLoader {
    private var imageLoader: ImageLoader? = null
    
    /**
     * Get the singleton instance of ImageLoader with optimized settings
     */
    fun getInstance(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            imageLoader ?: createImageLoader(context.applicationContext).also { imageLoader = it }
        }
    }
    
    /**
     * Create a new ImageLoader instance with optimal configuration
     */
    private fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            // Memory cache with optimized capacity
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 25% of available memory
                    .build()
            }
            // Disk cache configuration
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(200 * 1024 * 1024) // 200MB
                    .build()
            }
            // Cache policies
            .respectCacheHeaders(false) // Ignore cache-control headers from network
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Configure network client
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
            }
            // Crossfade duration when loading images
            .crossfade(true)
            .crossfade(300)
            // Use IO dispatcher for background operations
            .dispatcher(Dispatchers.IO)
            // Use hardware acceleration when possible
            .allowHardware(true)
            .build()
    }
    
    /**
     * Clear memory and disk caches
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearCaches() {
        imageLoader?.let { loader ->
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
        }
    }
    
    /**
     * Clear cache for a specific image URI
     * This is useful when an image has been modified and we want to force a reload
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearCacheForUri(context: Context, uri: Uri) {
        try {
            val loader = getInstance(context)
            
            // Create a key for the URI
            val request = ImageRequest.Builder(context)
                .data(uri)
                .build()
            
            // Clear from memory cache
            val key = request.memoryCacheKey
            if (key != null) {
                loader.memoryCache?.remove(key)
            }
            
            // Clear from disk cache if possible
            val diskCacheKey = uri.toString()
            loader.diskCache?.remove(diskCacheKey)
            
            android.util.Log.d("AppImageLoader", "Cleared cache for URI: $uri")
        } catch (e: Exception) {
            android.util.Log.e("AppImageLoader", "Error clearing cache for URI: $uri", e)
        }
    }
    
    /**
     * Reset the singleton instance (useful for testing or when memory pressure is high)
     */
    fun resetInstance() {
        synchronized(this) {
            imageLoader?.shutdown()
            imageLoader = null
        }
    }
} 