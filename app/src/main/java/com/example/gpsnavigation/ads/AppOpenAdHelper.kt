package com.example.gpsnavigation.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenAdHelper(private val activity: Activity) {

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var loadTime: Long = 0


    fun loadAndShowAd(
        adUnitId: String,
        onFinished: (didShow: Boolean, errorMessage: String?) -> Unit
    ) {
        if (isShowingAd) return

        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            activity,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTime = System.currentTimeMillis()
                    showIfAvailable(onFinished)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    onFinished(false, error.message)
                }
            }
        )
    }

    private fun showIfAvailable(
        onFinished: (didShow: Boolean, errorMessage: String?) -> Unit
    ) {
        if (isShowingAd) return

        if (!isAdAvailable()) {
            onFinished(false, "Ad not available")
            return
        }

        var didShow = false
        isShowingAd = true

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdShowedFullScreenContent() {
                didShow = true
            }

            override fun onAdDismissedFullScreenContent() {
                cleanup()
                onFinished(didShow, null)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                cleanup()
                onFinished(false, adError.message)
            }
        }

        appOpenAd?.show(activity)
    }

    private fun cleanup() {
        isShowingAd = false
        appOpenAd = null
    }

    private fun isAdAvailable(): Boolean {
        val elapsed = System.currentTimeMillis() - loadTime
        return appOpenAd != null && elapsed < 4 * 3600 * 1000 // 4 hours
    }
}

