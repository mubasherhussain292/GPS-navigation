package com.example.gpsnavigation.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.gpsnavigation.ads.AppOpenAdHelper
import com.example.gpsnavigation.ads.OnboardingInterstitialCache
import com.example.gpsnavigation.databinding.ActivitySplashBinding
import com.example.gpsnavigation.utils.AdsRemoteConfig
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val splashMinDurationMs = 1200L          // minimum time to show splash
    private val remoteConfigTimeoutMs = 1800L        // max time to wait for RC
    private var navigationDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1) Set default RC values
        val defaults = mapOf("InterShowCount" to 1L)
        val rc = Firebase.remoteConfig
        rc.setDefaultsAsync(defaults)

        // 2) Start flow: show splash at least X ms, fetch RC (but don't block forever)
        lifecycleScope.launch {
            val minDelayJob = async { delay(splashMinDurationMs) }
            val rcJob = async { fetchRemoteConfigWithTimeout(rc) }

            // Wait for minimum splash time
            minDelayJob.await()

            // Wait for RC OR timeout (whichever happens first)
            rcJob.await()

            // Navigate once
            goNext()
        }
    }

    private suspend fun fetchRemoteConfigWithTimeout(rc: FirebaseRemoteConfig) {
        withTimeoutOrNull(remoteConfigTimeoutMs) {
            suspendCancellableCoroutine<Unit> { cont ->
                rc.fetchAndActivate().addOnCompleteListener { task ->
                    try {
                        if (task.isSuccessful) {
                            lifecycleScope.launch {
                                AdsRemoteConfig.logAllSwitches(TAG)
                                AdsRemoteConfig.logAllAdIds(TAG)
                            }
                            preloadOnboardingInterstitialFromSplash()
                        }
                    } finally {
                        if (cont.isActive) cont.resume(Unit) {}
                    }
                }
            }
        } ?: run {

            // Timeout happened: still safe to proceed (optional fallback preload)
            preloadOnboardingInterstitialFromSplashFallback()
        }
    }

    private fun goNext() {
        if (navigationDone) return
        navigationDone = true
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this@SplashActivity, OnBoardingActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun preloadOnboardingInterstitialFromSplash() {
        // If you want to skip preload for premium, uncomment:
        // if (PrefsManager.isPremium(application)) return

        val interId =
            /*if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712"
            else*/ AdsRemoteConfig.GPS_onboarding_largebanner_id

        OnboardingInterstitialCache.preload(applicationContext, interId)
    }

    private fun preloadOnboardingInterstitialFromSplashFallback() {
        // Fallback preload (optional) if RC didn't arrive in time
        val interId =
            /*if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712"
            else*/ AdsRemoteConfig.GPS_onboarding_largebanner_id

        OnboardingInterstitialCache.preload(applicationContext, interId)
    }

    companion object {
        private const val TAG = "SplashActivity"
    }
}


private const val TAG = "SplashActivity"