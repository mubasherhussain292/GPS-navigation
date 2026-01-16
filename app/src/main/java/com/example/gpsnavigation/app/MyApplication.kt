package com.example.gpsnavigation.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.gpsnavigation.activities.MainActivity
import com.example.gpsnavigation.activities.OnBoardingActivity
import com.example.gpsnavigation.activities.SplashActivity
import com.example.gpsnavigation.ads.AppOpenManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

class MyApplication : Application(), Application.ActivityLifecycleCallbacks,
    LifecycleEventObserver {


    lateinit var appOpenManager: AppOpenManager
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)


        val rc = Firebase.remoteConfig

        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
            fetchTimeoutInSeconds = 10
        }
        rc.setConfigSettingsAsync(settings)
        appOpenManager = AppOpenManager(this)
        rc.fetchAndActivate().addOnCompleteListener {}
    }


    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_START) {

            if (currentActivity is com.google.android.gms.ads.AdActivity || currentActivity is SplashActivity || currentActivity is OnBoardingActivity) return
            Handler(Looper.getMainLooper()).postDelayed({
                appOpenManager.showAdIfAvailable(currentActivity)
            }, 150)

        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
        currentActivity = activity
        if(currentActivity is MainActivity){
            appOpenManager.loadAd()
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

}