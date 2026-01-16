package com.example.gpsnavigation.ads

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object OnboardingInterstitialCache {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading: Boolean = false
    private var loadTimeMs: Long = 0L
    private var lastAdUnitId: String? = null

    private const val FRESH_MS = 4 * 60 * 60 * 1000L

    fun isLoading(): Boolean = isLoading

    fun isFresh(): Boolean {
        val ad = interstitialAd ?: return false
        val age = SystemClock.elapsedRealtime() - loadTimeMs
        return age < FRESH_MS && ad != null
    }

    fun getAd(): InterstitialAd? = if (isFresh()) interstitialAd else null

    fun clear() {
        interstitialAd = null
        loadTimeMs = 0L
        lastAdUnitId = null
        isLoading = false
    }

    fun preload(
        context: Context,
        adUnitId: String,
        onLoaded: ((InterstitialAd) -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        val appCtx = context.applicationContext

        if (isFresh() && lastAdUnitId == adUnitId) {
            onLoaded?.invoke(interstitialAd!!)
            return
        }

        if (isLoading) return

        isLoading = true
        lastAdUnitId = adUnitId

        Log.d("TAG", "preloaded: ")
        InterstitialAd.load(
            appCtx,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    loadTimeMs = SystemClock.elapsedRealtime()
                    isLoading = false
                    onLoaded?.invoke(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    clear()
                    onFailed?.invoke(error)
                }
            }
        )
    }
}
