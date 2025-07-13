package com.example.recordapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.recordapp.util.SettingsManager
import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Theme wrapper that respects the user's theme setting preference
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsManager.getInstance(context) }
    
    // Determine if dark theme should be used based on settings
    val darkTheme = when (settings.appTheme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    
    // Set the status bar color based on theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(darkTheme) {
            val window = (context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            // Set system bars appearance based on theme
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            
            // Enable edge-to-edge experience with transparent system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Set transparent colors for the system bars
            // This is done by setting the background color of the entire window
            val transparentColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
            window.decorView.setBackgroundColor(transparentColor)
            
            onDispose {}
        }
    }
    
    RecordAppTheme(
        darkTheme = darkTheme,
        dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        content()
    }
} 