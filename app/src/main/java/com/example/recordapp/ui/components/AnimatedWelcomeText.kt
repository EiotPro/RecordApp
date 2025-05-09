package com.example.recordapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.recordapp.R
import com.example.recordapp.util.SettingsManager

@Composable
fun AnimatedWelcomeText(
    modifier: Modifier = Modifier
) {
    val text = stringResource(id = R.string.welcome_message)
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val textWidth = screenWidth * 1.5f  // Width estimation for animation
    
    // Check if animations are enabled
    val context = LocalContext.current
    val settings = remember { SettingsManager.getInstance(context) }
    val animationsEnabled = settings.animationsEnabled
    
    // Animation for scrolling text (only if animations are enabled)
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_text_animation")
    val xPosition by if (animationsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = screenWidth.value,
            targetValue = -textWidth.value,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 10000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "text_position"
        )
    } else {
        // Static position if animations are disabled
        remember { mutableStateOf(0f) }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clipToBounds(),
        contentAlignment = if (animationsEnabled) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .then(
                    if (animationsEnabled) {
                        Modifier.offset(x = xPosition.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
                .padding(vertical = 8.dp)
        )
    }
} 