package com.example.gpsnavigation.extensions

import android.content.Context
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.CountDownLatch

suspend fun Context.reverseGeocodeName(
    lat: Double,
    lng: Double
): String? = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(this@reverseGeocodeName, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ async style
            var result: String? = null
            val latch = CountDownLatch(1)

            geocoder.getFromLocation(lat, lng, 1) { list ->
                val addr = list.firstOrNull()
                result = addr?.let {
                    // choose what you want to save as "startName"
                    it.locality
                        ?: it.subAdminArea
                        ?: it.adminArea
                        ?: it.featureName
                        ?: it.getAddressLine(0)
                }
                latch.countDown()
            }

            latch.await()
            result
        } else {
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(lat, lng, 1)
            val addr = list?.firstOrNull()
            addr?.let {
                it.locality
                    ?: it.subAdminArea
                    ?: it.adminArea
                    ?: it.featureName
                    ?: it.getAddressLine(0)
            }
        }
    } catch (e: Exception) {
        null
    }
}


suspend fun Context.hasInternet(): Boolean = withContext(Dispatchers.IO) {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return@withContext false
    val caps = cm.getNetworkCapabilities(network) ?: return@withContext false
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}