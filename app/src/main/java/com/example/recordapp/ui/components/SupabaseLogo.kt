package com.example.recordapp.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.recordapp.R
import com.example.recordapp.network.InternetConnectionChecker

/**
 * A composable that displays the Supabase logo with different colors based on connectivity status
 * Green when connected, Red when disconnected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseLogo(
    connectionChecker: InternetConnectionChecker,
    modifier: Modifier = Modifier,
    size: Int = 40,
    showConnectionDetails: Boolean = true
) {
    val isConnected by connectionChecker.observeSupabaseStatus()
        .collectAsState(initial = connectionChecker.isSupabaseAvailable())
    
    var showDialog by remember { mutableStateOf(false) }
    
    if (showDialog && showConnectionDetails) {
        SupabaseConnectionDialog(
            connectionChecker = connectionChecker,
            onDismiss = { showDialog = false }
        )
    }
    
    SupabaseLogoWithState(
        isConnected = isConnected,
        modifier = modifier.clickable(enabled = showConnectionDetails) { 
            showDialog = true 
        },
        size = size
    )
}

/**
 * A shared implementation for the Supabase logo with connectivity state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupabaseLogoWithState(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    val tint = if (isConnected) Color(0xFF3ECF8E) else Color.Red
    val tooltipState = rememberTooltipState()
    val statusText = if (isConnected) "Connected to Supabase" else "Disconnected from Supabase"
    
    // Animation for disconnected state
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnimation")
    val alpha by infiniteTransition.animateFloat(
        initialValue = if (isConnected) 1f else 0.4f,
        targetValue = if (isConnected) 1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaAnimation"
    )
    
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(statusText)
            }
        },
        state = tooltipState
    ) {
        Image(
            painter = painterResource(id = R.drawable.supabase_logo),
            contentDescription = statusText,
            modifier = modifier
                .size(size.dp)
                .alpha(alpha),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}

/**
 * A simpler version that just takes a boolean for connectivity status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseLogoWithConnection(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    SupabaseLogoWithState(isConnected, modifier, size)
} 