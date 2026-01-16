package com.example.gpsnavigation.ads

import android.content.Context
import android.os.SystemClock
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd

object NativeHomeAdCache {

    private var nativeAd: NativeAd? = null
    private var isLoading = false
    private var loadTimeMs: Long = 0L
    private var lastAdUnitId: String? = null

    private const val FRESH_MS = 60 * 60 * 1000L // 1 hour

    fun getFresh(): NativeAd? {
        val ad = nativeAd ?: return null
        val age = SystemClock.elapsedRealtime() - loadTimeMs
        return if (age < FRESH_MS) ad else null
    }

    fun clear() {
        nativeAd?.destroy()
        nativeAd = null
        isLoading = false
        loadTimeMs = 0L
        lastAdUnitId = null
    }

    fun preload(
        context: Context,
        adUnitId: String,
        onLoaded: ((NativeAd) -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        val appCtx = context.applicationContext

        getFresh()?.let { cached ->
            if (lastAdUnitId == adUnitId) {
                onLoaded?.invoke(cached)
                return
            } else {
                clear()
            }
        }

        if (isLoading) return
        isLoading = true
        lastAdUnitId = adUnitId

        val adLoader = AdLoader.Builder(appCtx, adUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                loadTimeMs = SystemClock.elapsedRealtime()
                isLoading = false
                onLoaded?.invoke(ad)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    onFailed?.invoke(error)
                }

                override fun onAdImpression() {
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }
}