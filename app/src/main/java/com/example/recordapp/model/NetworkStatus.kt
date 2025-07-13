package com.example.recordapp.model

/**
 * Network connection type
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    BLUETOOTH,
    OTHER,
    UNKNOWN,
    NONE
}

/**
 * Connection quality levels
 */
enum class ConnectionQuality {
    POOR,
    MODERATE,
    GOOD,
    EXCELLENT,
    UNKNOWN
}

/**
 * Represents the current network status
 */
data class NetworkStatus(
    val isConnected: Boolean = false,
    val networkType: NetworkType = NetworkType.NONE,
    val quality: ConnectionQuality = ConnectionQuality.UNKNOWN,
    val isMetered: Boolean = false,
    val isOfflineMode: Boolean = false,
    val syncInProgress: Boolean = false,
    val lastSyncTime: Long = 0L
) 