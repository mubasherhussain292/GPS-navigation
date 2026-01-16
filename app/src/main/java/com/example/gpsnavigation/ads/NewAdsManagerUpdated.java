package com.example.gpsnavigation.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import org.jetbrains.annotations.NotNull;

public class NewAdsManagerUpdated {

    private static NewAdsManagerUpdated instance;
    private InterstitialAd mInterstitialAd;
    private final String TAG = "NewAdsManagerUpdated";

    private NewAdsManagerUpdated() {}

    public static synchronized NewAdsManagerUpdated getInstance() {
        if (instance == null) instance = new NewAdsManagerUpdated();
        return instance;
    }

    public void initializeAds(Context context) {
        Context appContext = context.getApplicationContext();
        MobileAds.initialize(appContext, initializationStatus -> {});
    }


    public void loadAdMobInterstitialAds(Context context, String id, InterstitalAdListener listener) {

        Context appContext = context.getApplicationContext();
        AdRequest request = new AdRequest.Builder().build();

        InterstitialAd.load(appContext, id, request, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                mInterstitialAd = ad;
                listener.isAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                mInterstitialAd = null;
                listener.isAdError();
            }
        });
    }

    public void showInterstitialAd(Activity activity, Listener listener) {
        if (mInterstitialAd == null) {
            listener.intersitialAdClosedCallback();
            return;
        }

        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                mInterstitialAd = null;
                listener.intersitialAdClosedCallback();
            }
        });

        mInterstitialAd.show(activity);
    }

    public interface Listener {
        void intersitialAdClosedCallback();
    }

    public interface InterstitalAdListener {
        void isAdLoaded();

        void isAdError();
    }

}