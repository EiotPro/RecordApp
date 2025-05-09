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
            
            // Set system bars appearance
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            
            // Make the system bars transparent
            window.statusBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
            window.navigationBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
            
            // Enable edge-to-edge experience
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
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