package com.example.recordapp.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.recordapp.model.NetworkType
import com.example.recordapp.network.InternetConnectionChecker
import com.example.recordapp.network.SupabaseConnectionStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SupabaseConnectionDialog(
    connectionChecker: InternetConnectionChecker,
    onDismiss: () -> Unit
) {
    val connectionStatus = connectionChecker.getSupabaseConnectionDetails()
    val isConnected = connectionStatus.isConnected
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Supabase Connection Status",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            coroutineScope.launch {
                                connectionChecker.refreshSupabaseHealth()
                                isRefreshing = false
                            }
                        },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Overall Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConnected) 
                            Icons.Default.SignalWifi4Bar else Icons.Default.SignalWifiOff,
                        contentDescription = if (isConnected) "Connected" else "Disconnected",
                        tint = if (isConnected) Color(0xFF3ECF8E) else Color.Red,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = if (isConnected) "Connected" else "Disconnected",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isConnected) Color(0xFF3ECF8E) else Color.Red
                        )
                        Text(
                            text = "Network: ${connectionStatus.networkType.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Latency
                if (connectionStatus.latencyMs > 0) {
                    Text(
                        text = "Latency: ${connectionStatus.latencyMs}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Color-coded latency indicator
                    val latencyColor = when {
                        connectionStatus.latencyMs < 100 -> Color(0xFF3ECF8E) // Good
                        connectionStatus.latencyMs < 300 -> Color(0xFFFFA500) // Medium
                        else -> Color.Red // Poor
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(getLatencyPercentage(connectionStatus.latencyMs))
                                .height(6.dp),
                            color = latencyColor,
                            shape = RoundedCornerShape(3.dp)
                        ) {}
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Service Status
                Text(
                    text = "Service Status",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // List each service
                connectionStatus.serviceStatus.forEach { (service, isUp) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isUp) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (isUp) "Service Up" else "Service Down",
                            tint = if (isUp) Color(0xFF3ECF8E) else Color.Red
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = service,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Last updated timestamp
                Text(
                    text = "Last updated: ${formatTimestamp(connectionStatus.lastUpdated)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getLatencyPercentage(latencyMs: Long): Float {
    // Convert latency to a percentage for the progress bar
    // 0-50ms: 100%, 500+ms: 0%
    return when {
        latencyMs <= 50 -> 1f
        latencyMs >= 500 -> 0.1f
        else -> 1f - ((latencyMs - 50) / 450f)
    }
} 