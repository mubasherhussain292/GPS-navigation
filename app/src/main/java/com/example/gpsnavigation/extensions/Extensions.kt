package com.example.gpsnavigation.extensions

import android.content.Context
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.CountDownLatch

private val Context.gpsDataStore by preferencesDataStore(name = GPS_PREFS_STORE_NAME)
private const val GPS_PREFS_STORE_NAME = "gps_navigation_prefs"
private const val GPS_SHARED_PREFS_NAME = "gps_navigation_prefs"


object GpsPrefsKeys {
    val APP_OPEN_DID_SHOW = booleanPreferencesKey("app_open_did_show")
}
object GpsAdsKeys {
    val ONBOARDING_INTERSTITIAL_DID_SHOW =
        booleanPreferencesKey("onboarding_interstitial_did_show")

    /**
     * When true: next feature interstitial attempt should be skipped once.
     * This becomes true if AppOpen OR Onboarding interstitial actually showed.
     */
    val SKIP_NEXT_FEATURE_INTERSTITIAL =
        booleanPreferencesKey("skip_next_feature_interstitial")
}

suspend fun Context.markSkipNextFeatureInterstitial() {
    gpsDataStore.edit { prefs ->
        prefs[GpsAdsKeys.SKIP_NEXT_FEATURE_INTERSTITIAL] = true
    }
}

suspend fun Context.setOnboardingInterstitialDidShow(value: Boolean) {
    gpsDataStore.edit { prefs ->
        prefs[GpsAdsKeys.ONBOARDING_INTERSTITIAL_DID_SHOW] = value
    }

    // If onboarding interstitial actually showed => mark skip-next-feature-interstitial
    if (value) {
        markSkipNextFeatureInterstitial()
    }
}

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