import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.example.gpsnavigation"
    compileSdk = 36

    // Put ndkVersion at android-level (recommended)
    ndkVersion = "27.0.11718014"

    defaultConfig {
        applicationId = "com.sgs.gps.navigation.map.streetview"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    // Lint config belongs to android-level, NOT inside defaultConfig
    lint {
        disable += "NullSafeMutableLiveData"
    }

    buildFeatures {
        viewBinding = true
    }

    // AGP newer DSL (recommended)
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

/**
 * Kotlin 2.x: migrate away from kotlinOptions { jvmTarget = "17" }
 */
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // AndroidX / Material
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.process)

    // Google / Maps / Location
    implementation(libs.places)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.github.thomper:sweet-alert-dialog:v1.4.0")
    // UI utilities
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.github.anastr:speedviewlib:1.6.1")

    // Mapbox (NDK27 for 16KB alignment)
    implementation("com.mapbox.maps:android-ndk27:11.16.2")

    // Mapbox Search (ALL SAME VERSION)
    implementation("com.mapbox.search:mapbox-search-android-ndk27:2.17.1")
    implementation("com.mapbox.search:mapbox-search-android-ui-ndk27:2.17.1")
    implementation("com.mapbox.search:discover-ndk27:2.17.1")
    implementation("com.mapbox.search:place-autocomplete-ndk27:2.17.1")

    // Mapbox Navigation
    implementation("com.mapbox.navigationcore:android-ndk27:3.17.0-rc.2")
    implementation("com.google.android.gms:play-services-ads:24.8.0")
    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("com.github.amitshekhariitbhu.Fast-Android-Networking:android-networking:1.0.4") {
        exclude(group = "com.android.support", module = "support-compat")
    }
    // Room
    val roomVersion = "2.7.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")

}
