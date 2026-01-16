package com.example.gpsnavigation.ads

import android.app.Activity
import android.util.DisplayMetrics
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.example.gpsnavigation.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

var noAppOpenAd = true

fun FrameLayout.loadBanner(adUnitId: String) {
    this.removeAllViews()

    val adView = AdView(context).apply {
        this.adUnitId = adUnitId
        this.setAdSize(AdSize.BANNER)
    }

    val adRequest = AdRequest.Builder().build()

    this.addView(adView)
    adView.loadAd(adRequest)
}

fun Activity.isActivityResumed(): Boolean {
    return (this as? ComponentActivity)?.lifecycle?.currentState == Lifecycle.State.RESUMED
}

fun NativeAdView.populate(nativeAd: NativeAd) {

    fun bindText(id: Int, value: String?) {
        findViewById<TextView>(id)?.text = value ?: ""
    }

    fun bindIcon(id: Int, icon: NativeAd.Image?) {
        icon ?: return
        findViewById<ImageView>(id)?.setImageDrawable(icon.drawable)
    }

    headlineView = findViewById(R.id.ad_headline)
    bodyView = findViewById(R.id.ad_body)
    callToActionView = findViewById(R.id.ad_call_to_action)
    iconView = findViewById(R.id.ad_app_icon)

    bindText(R.id.ad_headline, nativeAd.headline)
    bindText(R.id.ad_body, nativeAd.body)
    bindText(R.id.ad_advertiser, nativeAd.advertiser)
    findViewById<Button>(R.id.ad_call_to_action)?.text = nativeAd.callToAction

    bindIcon(R.id.ad_app_icon, nativeAd.icon)

    setNativeAd(nativeAd)
}


fun NativeAdView.populateWithMedia(nativeAd: NativeAd) {

    headlineView = findViewById(R.id.ad_headline)
    bodyView = findViewById(R.id.ad_body)
    callToActionView = findViewById(R.id.ad_call_to_action)
    advertiserView = findViewById(R.id.ad_advertiser)
    iconView = findViewById(R.id.ad_app_icon)
    mediaView = findViewById(R.id.ad_media)

    findViewById<TextView>(R.id.ad_headline)?.text = nativeAd.headline
    findViewById<TextView>(R.id.ad_body)?.text = nativeAd.body
    findViewById<TextView>(R.id.ad_advertiser)?.text = nativeAd.advertiser
    findViewById<Button>(R.id.ad_call_to_action)?.text = nativeAd.callToAction

    nativeAd.icon?.let {
        findViewById<ImageView>(R.id.ad_app_icon)?.setImageDrawable(it.drawable)
    }

    mediaView?.mediaContent = nativeAd.mediaContent

    setNativeAd(nativeAd)
}



fun FrameLayout.loadAdaptiveBanner(activity: Activity, adUnitId: String) {
//    if (PrefsManager.isPremium(activity.application)) {
        (getTag(R.id.tag_banner_adview) as? AdView)?.destroy()
        setTag(R.id.tag_banner_adview, null)
        setTag(R.id.tag_banner_adunit, null)
        removeAllViews()
        visibility = View.GONE
        return
    /*} else {
        visibility = View.VISIBLE
    }*/

    // If already loaded for same unit in this container -> return
    val existingUnit = getTag(R.id.tag_banner_adunit) as? String
    val existingAdView = getTag(R.id.tag_banner_adview) as? AdView
    if (existingAdView != null && existingUnit == adUnitId && childCount > 0) {
        return
    }

    // Wait until this FrameLayout has correct width
    post {
        val widthPx = if (width > 0) width else activity.resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density
        val adWidth = (widthPx / density).toInt()

        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)

        // Destroy old banner if any (avoid leaks)
        (getTag(R.id.tag_banner_adview) as? AdView)?.destroy()

        val adView = AdView(activity).apply {
            this.adUnitId = adUnitId
            setAdSize(adSize)

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                }

                override fun onAdImpression() {
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                }
            }
        }

        // Attach
        removeAllViews()
        addView(adView)

        setTag(R.id.tag_banner_adview, adView)
        setTag(R.id.tag_banner_adunit, adUnitId)

        // Load
        adView.loadAd(AdRequest.Builder().build())
    }
}
fun FrameLayout.destroyBanner() {
    (getTag(R.id.tag_banner_adview) as? AdView)?.destroy()
    setTag(R.id.tag_banner_adview, null)
    setTag(R.id.tag_banner_adunit, null)
    removeAllViews()
}
private fun getAdaptiveAdSize(activity: Activity): AdSize {

    val display = activity.windowManager.defaultDisplay
    val outMetrics = DisplayMetrics()
    display.getMetrics(outMetrics)

    val widthPixels = outMetrics.widthPixels
    val density = outMetrics.density

    val adWidth = (widthPixels / density).toInt()

    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
        activity,
        adWidth
    )
}
