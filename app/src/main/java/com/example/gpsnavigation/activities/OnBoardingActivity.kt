package com.example.gpsnavigation.activities

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.gpsnavigation.R
import com.example.gpsnavigation.adapters.OnboardingAdapter
import com.example.gpsnavigation.ads.OnboardingInterstitialCache
import com.example.gpsnavigation.databinding.ActivityOnBoardingBinding
import com.example.gpsnavigation.extensions.markSkipNextFeatureInterstitial
import com.example.gpsnavigation.extensions.setOnboardingInterstitialDidShow
import com.example.gpsnavigation.models.OnboardingItem
import com.example.gpsnavigation.utils.AdsRemoteConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.launch
import kotlin.text.clear

class OnBoardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnBoardingBinding
    private var interstitialAd: InterstitialAd? = null
    private var isShowingInterstitial = false
    private var isLoadingInterstitial = false
    private var interstitialLoadTimeMs: Long = 0L
    private var pendingShowOnLoad = false

    private val INTERSTITIAL_TTL_MS = 55 * 60 * 1000L
    private val CLICK_WAIT_TIMEOUT_MS = 1500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val items = listOf(
            OnboardingItem(R.drawable.img_1, "", ""),
            OnboardingItem(R.drawable.img_2, "", ""),
            OnboardingItem(R.drawable.img_3, "", "")
        )

        binding.viewPager.adapter = OnboardingAdapter(items)


        loadRectangleAd(binding.adContainer, "ca-app-pub-3940256099942544/9214589741")


        binding.btnNext.setOnClickListener {
            val pager = binding.viewPager
            if (pager.currentItem < items.size - 1) {
                pager.currentItem = pager.currentItem + 1
            } else {
                showInterstitialOrGoNext()
//                startActivity(Intent(this, MainActivity::class.java))
//                finish()
            }
        }
    }

    private fun preloadInterstitial() {
        if (!AdsRemoteConfig.GPS_onboarding_int_control /*|| PrefsManager.isPremium(application)*/) return

        val interId =
            /*if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712"
            else*/ AdsRemoteConfig.GPS_onboarding_int_id

        OnboardingInterstitialCache.preload(
            context = applicationContext,
            adUnitId = interId,
            onLoaded = { ad ->
                // If user already clicked and we were waiting, show immediately.
                if (pendingShowOnLoad && !isShowingInterstitial && !isFinishing && !isDestroyed) {
                    pendingShowOnLoad = false
                    showOnboardingInterstitial(ad)
                }
            },
            onFailed = {
                if (pendingShowOnLoad) {
                    pendingShowOnLoad = false
                    saveOnboardingInterstitialDidShow(false)
                    goNext()
                }
            }
        )
    }
    private fun goNext() {
        /*if (AdsRemoteConfig.premiumscreen) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
        } else {*/
            startActivity(Intent(this, MainActivity::class.java))
            finish()

    }
    private fun saveOnboardingInterstitialDidShow(value: Boolean) {
        lifecycleScope.launch {
            applicationContext.setOnboardingInterstitialDidShow(value)
        }
    }

    private fun showOnboardingInterstitial(ad: InterstitialAd) {
        if (isShowingInterstitial || isFinishing || isDestroyed) {
            saveOnboardingInterstitialDidShow(false)
            goNext()
            return
        }

        var didShow = false
        isShowingInterstitial = true

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdShowedFullScreenContent() {
                didShow = true
                saveOnboardingInterstitialDidShow(true)

                lifecycleScope.launch {
                    applicationContext.markSkipNextFeatureInterstitial()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                saveOnboardingInterstitialDidShow(false)
                isShowingInterstitial = false
                OnboardingInterstitialCache.clear()
                pendingShowOnLoad = false
                goNext()
            }

            override fun onAdDismissedFullScreenContent() {
                isShowingInterstitial = false
                OnboardingInterstitialCache.clear()
                pendingShowOnLoad = false

                // If it never actually showed, treat as false
                if (!didShow) saveOnboardingInterstitialDidShow(false)

                goNext()

            }
        }

        // Important for higher show reliability
        ad.setImmersiveMode(true)

        ad.show(this)
    }


    private fun showInterstitialOrGoNext() {
        /*if (PrefsManager.isPremium(application) || isFinishing || isDestroyed) {
            saveOnboardingInterstitialDidShow(false)
            goNext()
            return
        }*/

        val ad = OnboardingInterstitialCache.getAd()
        if (ad != null && !isShowingInterstitial) {
            showOnboardingInterstitial(ad)
            return
        }

        // Not ready yet → start loading and wait a bit
        pendingShowOnLoad = true
        preloadInterstitial()

        window.decorView.postDelayed({
            if (pendingShowOnLoad) {
                pendingShowOnLoad = false
                saveOnboardingInterstitialDidShow(false)
                goNext()
            }
        }, CLICK_WAIT_TIMEOUT_MS) // Recommend 1500 or 2000
    }


    private var mrecAdView: AdView? = null

    fun loadRectangleAd(container: FrameLayout, adUnitId: String) {
        /*if (PrefsManager.isPremium(application)) return*/
        val adView = AdView(container.context).apply {
            this.adUnitId =
                /*if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9214589741" else */adUnitId
            setAdSize(AdSize.MEDIUM_RECTANGLE)
        }

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        container.removeAllViews()
        container.addView(adView, lp)
        adView.loadAd(AdRequest.Builder().build())
        mrecAdView = adView
    }

}