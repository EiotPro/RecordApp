package com.example.recordapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.recordapp.model.ConnectionQuality
import com.example.recordapp.model.NetworkStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card for controlling sync operations
 */
@Composable
fun SyncControlsCard(
    networkStatus: NetworkStatus,
    onSyncNow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sync Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Network quality status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Network quality indicator for sync viability
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Network Quality",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Quality bar
                    LinearProgressIndicator(
                        progress = { 
                            when(networkStatus.quality) {
                                ConnectionQuality.EXCELLENT -> 1.0f
                                ConnectionQuality.GOOD -> 0.75f
                                ConnectionQuality.MODERATE -> 0.5f
                                ConnectionQuality.POOR -> 0.25f
                                else -> 0.0f
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = when(networkStatus.quality) {
                            ConnectionQuality.EXCELLENT -> MaterialTheme.colorScheme.primary
                            ConnectionQuality.GOOD -> MaterialTheme.colorScheme.secondary
                            ConnectionQuality.MODERATE -> MaterialTheme.colorScheme.tertiary
                            ConnectionQuality.POOR -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Quality description
                    Text(
                        text = if (networkStatus.isConnected) {
                            when(networkStatus.quality) {
                                ConnectionQuality.EXCELLENT -> "Excellent for syncing"
                                ConnectionQuality.GOOD -> "Good for syncing"
                                ConnectionQuality.MODERATE -> "May experience delays"
                                ConnectionQuality.POOR -> "Limited sync capability"
                                else -> "Not recommended for sync"
                            }
                        } else "Offline - Sync unavailable",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Sync button
                if (networkStatus.isConnected) {
                    Button(
                        onClick = onSyncNow,
                        enabled = networkStatus.isConnected
                    ) {
                        // Pulsating sync icon
                        val infiniteTransition = rememberInfiniteTransition(label = "syncIconPulse")
                        val iconScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "syncIconScale"
                        )
                        
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                modifier = Modifier.size(24.dp * iconScale)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Now")
                    }
                } else {
                    OutlinedButton(
                        onClick = {},
                        enabled = false
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Unavailable",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Unavailable")
                    }
                }
            }
        }
    }
}

/**
 * Card showing sync history and status
 */
@Composable
fun SyncHistoryCard(
    networkStatus: NetworkStatus,
    syncStatus: String?,
    lastSyncTime: Long,
    pendingOperationsCount: Int
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sync Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Last sync time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Last sync: " + if (lastSyncTime > 0) 
                        dateFormatter.format(Date(lastSyncTime)) 
                    else 
                        "Never",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pending operations
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PendingActions,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Pending operations: $pendingOperationsCount",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (pendingOperationsCount > 0) {
                        Text(
                            text = if (networkStatus.isConnected) 
                                "Will be synced automatically" 
                            else 
                                "Will sync when connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Sync optimization tip based on network quality
            if (networkStatus.isConnected && pendingOperationsCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                val batchSize = when (networkStatus.quality) {
                    ConnectionQuality.EXCELLENT -> "large batches"
                    ConnectionQuality.GOOD -> "medium batches"
                    ConnectionQuality.MODERATE -> "small batches"
                    else -> "very small batches"
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tip: Given your current connection quality, sync in $batchSize for optimal performance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            // Current sync status
            if (!syncStatus.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = syncStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
} 