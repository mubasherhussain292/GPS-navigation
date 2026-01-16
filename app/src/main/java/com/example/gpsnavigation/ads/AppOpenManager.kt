package com.example.gpsnavigation.ads

import android.app.Activity
import android.app.Application
import android.util.Log
import com.example.gpsnavigation.utils.AdsRemoteConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenManager(private val app: Application) {

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var loadTime: Long = 0

    private val adUnitId = AdsRemoteConfig.GPS_appopen_id
    private var loadRetryCount = 0
    private val maxRetryCount = 2
    private var isLoadingAd = false


    fun showAdIfAvailable(activity: Activity?) {
        Log.d("TAG", "showAdIfAvailable: ${noAppOpenAd}")

        if (activity == null) return
        if (isShowingAd || !noAppOpenAd) return



        if (isAdAvailable()) {
            isShowingAd = true

            appOpenAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("AppOpenAd", "onAdDismissedFullScreenContent: ")
                        isShowingAd = false
                        appOpenAd = null
                        loadRetryCount = 0
                        loadAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d(
                            "AppOpenAd",
                            "onAdFailedToShowFullScreenContent: ${adError.message}"
                        )
                        isShowingAd = false
                        appOpenAd = null
                        loadRetryCount = 0
                        // Do NOT retry immediately, wait for next foreground
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("AppOpenAd", "onAdShowedFullScreenContent: ")
                    }
                }

            Log.d("AppOpenAd", "Showing AppOpenAd")
            appOpenAd?.show(activity)
        } else {
            Log.d("AppOpenAd", "Ad not available, calling loadAd()")
            loadAd()
        }
    }


    fun loadAd() {

        if (isAdAvailable() || isShowingAd || isLoadingAd ||
            !AdsRemoteConfig.GPS_appopen_control /*|| PrefsManager.isPremium(app)*/
        ) {
            Log.d("AppOpenAd", "Skipping loadAd() - Ad already loaded or loading")
            return
        }

        if (loadRetryCount > maxRetryCount) {
            Log.d("AppOpenAd", "Max retry reached, will not retry anymore")
            return
        }

        isLoadingAd = true

        val request = AdRequest.Builder().build()
        Log.d("RemoteConfig", "resume app open: ${adUnitId}")
        AppOpenAd.load(app, /*if (BuildConfig.DEBUG)*/ "ca-app-pub-3940256099942544/9257395921" /*else adUnitId*/,
            request, object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d("AppOpenAd", "onAdLoaded")
                    appOpenAd = ad
                    loadTime = System.currentTimeMillis()
                    loadRetryCount = 0
                    isLoadingAd = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d("AppOpenAd", "onAdFailedToLoad: ${error.message}")
                    appOpenAd = null
                    isLoadingAd = false

                    loadRetryCount++

                    if (loadRetryCount <= maxRetryCount) {
                        loadAd()
                    } else {
                        Log.d("AppOpenAd", "All retries failed")
                    }
                }
            }
        )
    }


    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadedRecently()
    }

    private fun wasLoadedRecently(): Boolean {
        val elapsed = System.currentTimeMillis() - loadTime
        return elapsed < 4 * 3600 * 1000 // valid for 4 hours
    }
}


