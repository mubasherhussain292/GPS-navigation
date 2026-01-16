package com.example.gpsnavigation.utils

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig

object AdsRemoteConfig {

    private val rc get() = Firebase.remoteConfig
    val App_Open_ID_FT_control: Boolean get() = rc.getBoolean("App_Open_ID_FT_control")
    val GPS_appopen_control: Boolean get() = rc.getBoolean("GPS_appopen_control")
    val GPS_onboarding_int_control: Boolean get() = rc.getBoolean("GPS_onboarding_int_control")
    val GPS_home_largebanner_control: Boolean get() = rc.getBoolean("GPS_home_largebanner_control")
    val GPS_features_int_conntrol: Boolean get() = rc.getBoolean("GPS_features_int_conntrol")
    val GPS_features_banner_control: Boolean get() = rc.getBoolean("GPS_features_banner_control")





    val GPS_onboarding_int_id: String get() = rc.getString("GPS_onboarding_int_id")
    val GPS_features_banner_id: String get() = rc.getString("GPS_features_banner_id")
    val GPS_home_largebanner_id: String get() = rc.getString("GPS_home_largebanner_id")
    val splashbannerAdID: String get() = rc.getString("splashbannerAdID")
    val GPS_appopen_id: String get() = rc.getString("GPS_appopen_id")
    val GPS_features_int_id: String get() = rc.getString("GPS_features_int_id")
    val GPS_onboarding_largebanner_id: String get() = rc.getString("GPS_onboarding_largebanner_id")


    fun logAllSwitches(tag: String = "AdsRemoteConfig") {
        val map = linkedMapOf(
            "App_Open_ID_FT_control" to App_Open_ID_FT_control,

            "GPS_onboarding_int_control" to GPS_onboarding_int_control,
            "GPS_home_largebanner_control" to GPS_home_largebanner_control,
            "GPS_features_int_conntrol" to GPS_features_int_conntrol,
            "GPS_features_banner_control" to GPS_features_banner_control,

            "GPS_appopen_control" to GPS_appopen_control
        )
        map.forEach { (k, v) -> Log.d(tag, "$k = $v") }
    }



    fun logAllAdIds(tag: String = "AdsRemoteConfig") {
        val map = linkedMapOf(
            "splashbannerAdID" to splashbannerAdID,
            "GPS_onboarding_largebanner_id" to GPS_onboarding_largebanner_id,
            "GPS_features_banner_id" to GPS_features_banner_id,
            "GPS_onboarding_int_id" to GPS_onboarding_int_id,
            "GPS_home_largebanner_id" to GPS_home_largebanner_id,
            "GPS_appopen_id" to GPS_appopen_id,
            "GPS_features_int_id" to GPS_features_int_id,
            )
        map.forEach { (k, v) ->
            val printable = if (v.isNullOrBlank()) "(empty)" else v
            Log.d(tag, "$k = $printable")
        }
    }



    private fun getBoolAnyOf(keys: List<String>): Boolean {
        for (k in keys) if (rc.getBoolean(k)) return true
        return false
    }
}

