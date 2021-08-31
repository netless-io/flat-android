package io.agora.flat.di.interfaces

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlatNetworkObserver @Inject constructor() : StartupInitializer, NetworkObserver {
    companion object {
        const val TAG = "FlatNetworkObserver"
    }

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var state: MutableStateFlow<Boolean>

    override fun init(context: Context) {
        connectivityManager = context.getSystemService()!!
        state = MutableStateFlow(connectivityManager.allNetworks.any { it.isOnline() })

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun observeNetworkActive(): Flow<Boolean> = state.asStateFlow()

    override fun isOnline(): Boolean {
        return state.value
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = onConnectivityChange(network, true)
        override fun onLost(network: Network) = onConnectivityChange(network, false)
    }

    private fun onConnectivityChange(network: Network, isOnline: Boolean) {
        val isAnyOnline = connectivityManager.allNetworks.any {
            if (it == network) {
                // Don't trust the network capabilities for the network that just changed.
                isOnline
            } else {
                it.isOnline()
            }
        }
        state.value = isAnyOnline
        Log.d(TAG, "onConnectivityChange $isAnyOnline")
    }

    private fun Network.isOnline(): Boolean {
        val capabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(this)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

interface NetworkObserver {
    fun observeNetworkActive(): Flow<Boolean>
    fun isOnline(): Boolean
}