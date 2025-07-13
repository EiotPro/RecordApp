package com.example.recordapp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.example.recordapp.model.ConnectionQuality
import com.example.recordapp.model.NetworkStatus
import com.example.recordapp.model.NetworkType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connection type identification for internal use
 */
enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    BLUETOOTH,
    OTHER,
    UNKNOWN
}

/**
 * Enhanced connectivity status model
 */
data class SupabaseConnectionStatus(
    val isConnected: Boolean = false,
    val networkType: NetworkType = NetworkType.NONE,
    val latencyMs: Long = -1,
    val serviceStatus: Map<String, Boolean> = emptyMap(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Enhanced utility class for checking and monitoring internet connectivity with Hilt injection
 */
@Singleton
class InternetConnectionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "InternetConnectionChecker"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val checkerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Observable state for network status
    private val _networkStatus = MutableStateFlow(
        NetworkStatus(
            isConnected = false,
            networkType = NetworkType.NONE,
            quality = ConnectionQuality.UNKNOWN,
            lastSyncTime = System.currentTimeMillis()
        )
    )
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()
    
    // Enhanced Supabase connection status
    private val _supabaseStatus = MutableStateFlow(
        SupabaseConnectionStatus(
            isConnected = false
        )
    )
    val supabaseStatus: StateFlow<SupabaseConnectionStatus> = _supabaseStatus.asStateFlow()

    // Network callback for real-time monitoring
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkStatus(true)
            Log.d(TAG, "Network available: $network")
        }

        override fun onLost(network: Network) {
            updateNetworkStatus(false)
            Log.d(TAG, "Network lost: $network")
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            updateNetworkStatus(true, capabilities)
            Log.d(TAG, "Network capabilities changed: $capabilities")
        }
    }

    init {
        registerNetworkCallback()
        // Initial update
        updateNetworkStatus(isConnected())
        
        // Start a background job to periodically check Supabase health
        startSupabaseHealthMonitoring()
    }
    
    /**
     * Start periodic Supabase health monitoring
     */
    private fun startSupabaseHealthMonitoring() {
        checkerScope.launch {
            while(true) {
                try {
                    // Only check if there's network connectivity
                    if (isConnected()) {
                        val client = SupabaseClient.getInstance()
                        val latency = client.checkLatency()
                        val serviceStatus = client.checkSupabaseHealth()
                        val isAllUp = serviceStatus.values.all { it }
                        
                        _supabaseStatus.value = SupabaseConnectionStatus(
                            isConnected = isAllUp,
                            networkType = _networkStatus.value.networkType,
                            latencyMs = latency,
                            serviceStatus = serviceStatus,
                            lastUpdated = System.currentTimeMillis()
                        )
                    } else {
                        _supabaseStatus.value = _supabaseStatus.value.copy(
                            isConnected = false,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during Supabase health monitoring", e)
                    _supabaseStatus.value = _supabaseStatus.value.copy(
                        isConnected = false,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                
                // Check every 30 seconds
                delay(30_000)
            }
        }
    }

    /**
     * Register for network changes
     */
    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Could not register network callback", e)
        }
    }

    /**
     * Update network status based on current connectivity
     */
    private fun updateNetworkStatus(connected: Boolean, capabilities: NetworkCapabilities? = null) {
        val actualCapabilities = capabilities ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        } else null

        val networkType = determineNetworkType(actualCapabilities)
        val quality = determineConnectionQuality(actualCapabilities, networkType)
        val isMetered = isMeteredConnection(actualCapabilities)

        _networkStatus.value = _networkStatus.value.copy(
            isConnected = connected,
            networkType = networkType,
            quality = quality,
            isMetered = isMetered
        )
        
        // Also update Supabase status when network status changes
        if (!connected) {
            _supabaseStatus.value = _supabaseStatus.value.copy(
                isConnected = false,
                networkType = networkType,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    /**
     * Determine the type of network connection
     */
    private fun determineNetworkType(capabilities: NetworkCapabilities?): NetworkType {
        if (capabilities == null) return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }

    /**
     * Determine the quality of the connection based on network type and capabilities
     */
    private fun determineConnectionQuality(
        capabilities: NetworkCapabilities?,
        networkType: NetworkType
    ): ConnectionQuality {
        if (capabilities == null) return ConnectionQuality.UNKNOWN

        return when (networkType) {
            NetworkType.WIFI -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED)) {
                    ConnectionQuality.EXCELLENT
                } else {
                    ConnectionQuality.GOOD
                }
            }
            NetworkType.CELLULAR -> determineCellularQuality()
            NetworkType.ETHERNET -> ConnectionQuality.EXCELLENT
            NetworkType.OTHER -> ConnectionQuality.MODERATE
            else -> ConnectionQuality.UNKNOWN
        }
    }

    /**
     * Determine cellular connection quality based on network type
     * Safely handles permission issues by providing moderate quality as default
     */
    private fun determineCellularQuality(): ConnectionQuality {
        // Don't try to access telephonyManager.dataNetworkType which requires READ_PHONE_STATE permission
        // Instead, always return a safe default value
        return ConnectionQuality.MODERATE
    }

    /**
     * Check if the connection is metered (user pays for data)
     */
    private fun isMeteredConnection(capabilities: NetworkCapabilities?): Boolean {
        if (capabilities == null) return true // Assume metered if unknown
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        } else {
            connectivityManager.isActiveNetworkMetered
        }
    }

    /**
     * Check if the device has an active internet connection
     * 
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.isConnected ?: false
        }
    }
    
    /**
     * Check if the device currently has internet connectivity
     */
    fun isConnectedToInternet(): Boolean {
        updateNetworkStatus(isConnected()) // Update status before returning
        return _networkStatus.value.isConnected
    }
    
    /**
     * Force update of network status
     */
    fun refreshNetworkStatus() {
        updateNetworkStatus(isConnected())
    }

    /**
     * Returns a Flow that emits true when internet is available and false when it's not
     */
    fun observeConnectionStatus(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Initial connectivity status
        trySend(isConnected())
        
        // Callback for network changes
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                launch { trySend(true) }
            }
            
            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                launch { trySend(false) }
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                  networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                Log.d(TAG, "Network capabilities changed, has internet: $hasInternet")
                launch { trySend(hasInternet) }
            }
        }
        
        // Register the callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Remove the callback when the flow is closed
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()
    
    /**
     * Also check connectivity to the Supabase server
     * This could be improved with actual ping/health check
     */
    fun isSupabaseAvailable(): Boolean {
        // For now, we'll just check internet connectivity
        // In a real app, you might want to do a health check on your Supabase instance
        return isConnected() && SupabaseClient.getInstance().isSupabaseAvailable()
    }
    
    /**
     * Observe Supabase connectivity as a Flow
     * This will emit true when Supabase is available, false otherwise
     */
    fun observeSupabaseStatus(): Flow<Boolean> = _supabaseStatus
        .map { it.isConnected }
        .distinctUntilChanged()
    
    /**
     * Get the latest detailed Supabase connection status
     */
    fun getSupabaseConnectionDetails(): SupabaseConnectionStatus {
        return _supabaseStatus.value
    }
    
    /**
     * Manually trigger a refresh of the Supabase health status
     */
    suspend fun refreshSupabaseHealth() {
        try {
            if (isConnected()) {
                val client = SupabaseClient.getInstance()
                val latency = client.checkLatency()
                val serviceStatus = client.checkSupabaseHealth()
                val isAllUp = serviceStatus.values.all { it }
                
                _supabaseStatus.value = SupabaseConnectionStatus(
                    isConnected = isAllUp,
                    networkType = _networkStatus.value.networkType,
                    latencyMs = latency,
                    serviceStatus = serviceStatus,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing Supabase health", e)
        }
    }
} 