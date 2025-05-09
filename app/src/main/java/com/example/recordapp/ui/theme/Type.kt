package com.example.recordapp.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.recordapp.util.SettingsManager

// Get scale factor based on text size setting
private fun getTextSizeScale(context: Context): Float {
    val settingsManager = SettingsManager.getInstance(context)
    return when (settingsManager.textSize) {
        "small" -> 0.9f
        "large" -> 1.2f
        else -> 1.0f // medium
    }
}

// Base Typography with medium text size and enhanced hierarchy
val BaseTypography = Typography(
    // Display styles for major headers and prominent UI elements
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = -0.25.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    // Headline styles for section headers
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // Title styles with enhanced visual hierarchy
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,  // Increased from 16sp for better distinction
        lineHeight = 26.sp, // Increased from 24sp
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,  // Increased from 14sp
        lineHeight = 22.sp, // Increased from 20sp
        letterSpacing = 0.1.sp
    ),
    // Body text styles
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // Label styles for UI elements
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// Get scaled Typography based on settings
@Composable
fun getScaledTypography(): Typography {
    val context = LocalContext.current
    val scale = remember { getTextSizeScale(context) }
    
    return Typography(
        // Display styles
        displayLarge = BaseTypography.displayLarge.copy(
            fontSize = BaseTypography.displayLarge.fontSize * scale,
            lineHeight = BaseTypography.displayLarge.lineHeight * scale
        ),
        displayMedium = BaseTypography.displayMedium.copy(
            fontSize = BaseTypography.displayMedium.fontSize * scale,
            lineHeight = BaseTypography.displayMedium.lineHeight * scale
        ),
        displaySmall = BaseTypography.displaySmall.copy(
            fontSize = BaseTypography.displaySmall.fontSize * scale,
            lineHeight = BaseTypography.displaySmall.lineHeight * scale
        ),
        // Headline styles
        headlineLarge = BaseTypography.headlineLarge.copy(
            fontSize = BaseTypography.headlineLarge.fontSize * scale,
            lineHeight = BaseTypography.headlineLarge.lineHeight * scale
        ),
        headlineMedium = BaseTypography.headlineMedium.copy(
            fontSize = BaseTypography.headlineMedium.fontSize * scale,
            lineHeight = BaseTypography.headlineMedium.lineHeight * scale
        ),
        // Title styles
        titleLarge = BaseTypography.titleLarge.copy(
            fontSize = BaseTypography.titleLarge.fontSize * scale,
            lineHeight = BaseTypography.titleLarge.lineHeight * scale
        ),
        titleMedium = BaseTypography.titleMedium.copy(
            fontSize = BaseTypography.titleMedium.fontSize * scale,
            lineHeight = BaseTypography.titleMedium.lineHeight * scale
        ),
        titleSmall = BaseTypography.titleSmall.copy(
            fontSize = BaseTypography.titleSmall.fontSize * scale,
            lineHeight = BaseTypography.titleSmall.lineHeight * scale
        ),
        // Body styles
        bodyLarge = BaseTypography.bodyLarge.copy(
            fontSize = BaseTypography.bodyLarge.fontSize * scale,
            lineHeight = BaseTypography.bodyLarge.lineHeight * scale
        ),
        bodyMedium = BaseTypography.bodyMedium.copy(
            fontSize = BaseTypography.bodyMedium.fontSize * scale,
            lineHeight = BaseTypography.bodyMedium.lineHeight * scale
        ),
        bodySmall = BaseTypography.bodySmall.copy(
            fontSize = BaseTypography.bodySmall.fontSize * scale,
            lineHeight = BaseTypography.bodySmall.lineHeight * scale
        ),
        // Label styles
        labelLarge = BaseTypography.labelLarge.copy(
            fontSize = BaseTypography.labelLarge.fontSize * scale,
            lineHeight = BaseTypography.labelLarge.lineHeight * scale
        ),
        labelMedium = BaseTypography.labelMedium.copy(
            fontSize = BaseTypography.labelMedium.fontSize * scale,
            lineHeight = BaseTypography.labelMedium.lineHeight * scale
        ),
        labelSmall = BaseTypography.labelSmall.copy(
            fontSize = BaseTypography.labelSmall.fontSize * scale,
            lineHeight = BaseTypography.labelSmall.lineHeight * scale
        )
    )
}