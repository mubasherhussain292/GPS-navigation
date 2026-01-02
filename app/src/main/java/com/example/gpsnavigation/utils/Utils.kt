package com.example.gpsnavigation.utils

import android.content.Context
import android.location.Geocoder
import androidx.core.content.edit
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import java.util.Locale

object Utils {

    fun saveOnBoardingClicked(context: Context, clicked: Boolean) {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit { putBoolean("onboarding_clicked", clicked) }
    }

    fun getSaveOnBoardingClicked(context: Context): Boolean {

        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        return prefs.getBoolean("onboarding_clicked", false)
    }

    fun saveTermConditionsClicked(context: Context, clicked: Boolean) {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit { putBoolean("terms_conditions_clicked", clicked) }
    }

    fun getSaveTermConditionsClicked(context: Context): Boolean {

        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        return prefs.getBoolean("terms_conditions_clicked", false)
    }

    fun saveTemperatureUnit(context: Context, unit: String) {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit { putString("temperature_unit", unit) }
    }

    fun getTemperatureUnit(context: Context): String {

        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        return prefs.getString("temperature_unit", "C") ?: "C"
    }

    fun saveSpeedUnit(context: Context, unit: String) {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit { putString("speed_unit", unit) }
    }

    fun getSpeedUnit(context: Context): String {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        return prefs.getString("speed_unit", "km/h") ?: "km/h"
    }

    fun logUserEvent(context: Context, eventName: String, params: Map<String, Any?> = emptyMap()) {
        /*val analytics = FirebaseAnalytics.getInstance(context)
        val bundle = Bundle()
        for ((key, value) in params) {
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                else -> bundle.putString(key, value?.toString())
            }
        }
        analytics.logEvent(eventName, bundle)*/
    }


    fun getAddressFromLocation(context: Context, latitude: Double, longitude: Double): Pair<String?, String?> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].locality
                val country = addresses[0].countryName
                Pair(city, country)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
        }
    }

    fun Location.toPoint(): Point {
        return Point.fromLngLat(this.longitude, this.latitude)
    }
}
