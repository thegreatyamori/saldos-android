package com.thegreatyamori.saldos.work

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkPolicy {

    fun isOnWifi(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
