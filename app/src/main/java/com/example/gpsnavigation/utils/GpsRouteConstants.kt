package com.example.gpsnavigation.utils

object GpsRouteConstants {
    @Volatile var currentLat: Double? = null
    @Volatile var currentLng: Double? = null

    @Volatile var destLat: Double? = null
    @Volatile var destLng: Double? = null
}