package com.example.gpsnavigation.ads

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd

object NativeOnboardAdStore {

    private var currentAd: NativeAd? = null
    private val listeners = mutableSetOf<(NativeAd) -> Unit>()

    fun observe(owner: Any, listener: (NativeAd) -> Unit) {
        listeners.add(listener)
        currentAd?.let(listener)
    }

    fun remove(listener: (NativeAd) -> Unit) {
        listeners.remove(listener)
    }

    fun clear() {
        currentAd?.destroy()
        currentAd = null
    }

    fun load(context: Context, adUnitId: String, onFailed: ((LoadAdError) -> Unit)? = null) {
        if (currentAd != null) return

        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                currentAd?.destroy()
                currentAd = ad
                listeners.forEach { it(ad) }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    onFailed?.invoke(error)
                }
            })
            .build()

        loader.loadAd(AdRequest.Builder().build())
    }
}
