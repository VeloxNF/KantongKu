package com.example.data.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = checkCurrentOnline()
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkCurrentOnline()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            _isOnline.value = hasUsableInternet(networkCapabilities) || checkCurrentOnline()
        }
    }

    init {
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (_: Exception) {
            _isOnline.value = checkCurrentOnline()
        }
    }

    fun refreshNow(): Boolean {
        val current = checkCurrentOnline()
        _isOnline.value = current
        return current
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
    }

    private fun checkCurrentOnline(): Boolean {
        val active = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return false
        return hasUsableInternet(caps)
    }

    private fun hasUsableInternet(caps: NetworkCapabilities): Boolean {
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            true
        }
        return hasInternet && isValidated
    }
}
