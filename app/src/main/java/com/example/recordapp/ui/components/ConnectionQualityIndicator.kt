package com.example.recordapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet4Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recordapp.model.ConnectionQuality
import com.example.recordapp.network.InternetConnectionChecker

/**
 * Displays the current connection quality in the status bar
 */
@Composable
fun ConnectionQualityIndicator(
    connectionChecker: InternetConnectionChecker,
    modifier: Modifier = Modifier
) {
    val networkStatus by connectionChecker.networkStatus.collectAsState()
    val supabaseStatus by connectionChecker.supabaseStatus.collectAsState()

    val IsConnected = networkStatus.isConnected && supabaseStatus.isConnected
    val quality = networkStatus.quality
    
    val (icon, label, color) = when {
        !networkStatus.isConnected -> Triple(
            Icons.Default.SignalCellularConnectedNoInternet4Bar,
            "Offline",
            Color.Red
        )
        !supabaseStatus.isConnected -> Triple(
            Icons.Default.SignalCellularConnectedNoInternet4Bar,
            "Supabase Offline",
            Color(0xFFFFA500) // Orange
        )
        quality == ConnectionQuality.EXCELLENT -> Triple(
            Icons.Default.SignalCellular4Bar,
            "Excellent",
            Color(0xFF3ECF8E) // Green
        )
        quality == ConnectionQuality.GOOD -> Triple(
            Icons.Default.SignalCellularAlt,
            "Good",
            Color(0xFF9ACD32) // Yellow-green
        )
        else -> Triple(
            Icons.Default.SignalCellularAlt1Bar,
            "Poor",
            Color(0xFFFFA500) // Orange
        )
    }
    
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 500),
        label = "colorAnimation"
    )
    
    // Latency info
    val latency = if (supabaseStatus.latencyMs > 0) "${supabaseStatus.latencyMs}ms" else ""
    
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Connection quality: $label",
            tint = animatedColor,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = label,
            color = animatedColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        
        if (latency.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "($latency)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
} 