package com.example.recordapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Teal80,
    tertiary = Green80,
    // Additional customizations for financial theme
    surfaceVariant = Slate.copy(alpha = 0.15f),
    onSurfaceVariant = Slate,
    inverseSurface = Blue80.copy(alpha = 0.2f)
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Teal40,
    tertiary = Green40,
    // Additional customizations for financial theme
    surfaceVariant = Slate.copy(alpha = 0.08f),
    onSurfaceVariant = Slate,
    inverseSurface = Blue40.copy(alpha = 0.1f),
    // Accent color for special elements
    surfaceTint = Gold.copy(alpha = 0.8f)
)

@Composable
fun RecordAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // Enhance dynamic scheme with our financial accent colors while preserving dynamic base
            dynamicScheme.copy(
                tertiary = if (darkTheme) Green80 else Green40,
                surfaceTint = Gold.copy(alpha = if (darkTheme) 0.9f else 0.8f),
                error = Error
            )
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Get typography scaled according to user settings
    val typography = getScaledTypography()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}