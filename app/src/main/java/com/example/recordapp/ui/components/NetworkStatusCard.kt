package com.example.recordapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.recordapp.model.NetworkStatus
import com.example.recordapp.model.NetworkType
import com.example.recordapp.model.ConnectionQuality
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A card displaying the current network status
 */
@Composable
fun NetworkStatusCard(
    networkStatus: NetworkStatus,
    modifier: Modifier = Modifier
) {
    val connectionColor = when {
        !networkStatus.isConnected -> MaterialTheme.colorScheme.error
        networkStatus.quality == ConnectionQuality.POOR -> MaterialTheme.colorScheme.error
        networkStatus.quality == ConnectionQuality.MODERATE -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    
    val connectionIcon = when {
        !networkStatus.isConnected -> Icons.Default.SignalWifiOff
        networkStatus.networkType == NetworkType.WIFI -> Icons.Default.Wifi
        networkStatus.networkType == NetworkType.CELLULAR -> Icons.Default.SignalCellular4Bar
        networkStatus.networkType == NetworkType.ETHERNET -> Icons.Default.Lan
        else -> Icons.Default.Wifi
    }
    
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }
    val lastSyncFormatted = if (networkStatus.lastSyncTime > 0) 
        dateFormatter.format(Date(networkStatus.lastSyncTime)) 
    else 
        "Never"
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (networkStatus.isOfflineMode) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection icon with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(connectionColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = connectionIcon,
                    contentDescription = "Connection Status",
                    tint = connectionColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Connection details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (networkStatus.isOfflineMode) "Working Offline" else
                          if (networkStatus.syncInProgress) "Synchronizing..." else
                          when (networkStatus.networkType) {
                              NetworkType.WIFI -> "Connected to WiFi"
                              NetworkType.CELLULAR -> "Connected to Mobile Data"
                              NetworkType.ETHERNET -> "Connected to Ethernet"
                              else -> "Connected"
                          },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = if (networkStatus.isConnected)
                        "${networkStatus.quality.name.lowercase().replaceFirstChar { it.uppercase() }} connection speed" 
                    else 
                        "No internet connection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (networkStatus.isMetered) {
                    Text(
                        text = "Metered connection - data charges may apply",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Last sync: $lastSyncFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Sync status indicator
            if (networkStatus.syncInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (networkStatus.isConnected) 
                        Icons.Default.CloudUpload
                    else 
                        Icons.Default.Save,
                    contentDescription = "Sync Status",
                    tint = if (networkStatus.isConnected) 
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * Get appropriate icon based on connection type
 */
@Composable
private fun getConnectionIcon(networkStatus: NetworkStatus) = when {
    !networkStatus.isConnected -> Icons.Default.SignalCellularConnectedNoInternet0Bar
    networkStatus.networkType == NetworkType.WIFI -> Icons.Default.SignalWifi4Bar
    networkStatus.networkType == NetworkType.CELLULAR -> Icons.Default.SignalCellular4Bar
    networkStatus.networkType == NetworkType.ETHERNET -> Icons.Default.SignalCellularAlt
    else -> Icons.Default.SignalWifiOff
}

/**
 * Get user-friendly connection type string
 */
private fun getConnectionTypeString(type: NetworkType): String = when(type) {
    NetworkType.WIFI -> "WiFi"
    NetworkType.CELLULAR -> "Mobile Data"
    NetworkType.ETHERNET -> "Ethernet"
    NetworkType.BLUETOOTH -> "Bluetooth"
    NetworkType.OTHER -> "Other"
    NetworkType.UNKNOWN -> "Unknown"
    NetworkType.NONE -> "None"
}

/**
 * Format speed in human-readable format
 */
private fun formatSpeed(speedKbps: Int): String {
    return when {
        speedKbps >= 1024 * 1024 -> String.format("%.2f Gbps", speedKbps / (1024.0 * 1024.0))
        speedKbps >= 1024 -> String.format("%.1f Mbps", speedKbps / 1024.0)
        else -> "$speedKbps Kbps"
    }
}

/**
 * Format timestamp as human-readable date/time
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss, MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 