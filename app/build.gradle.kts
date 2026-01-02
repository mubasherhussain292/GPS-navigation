plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")

}

android {
    namespace = "com.example.gpsnavigation"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.gpsnavigation"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndkVersion = "27.0.11718014"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
        lint {
            disable += listOf("NullSafeMutableLiveData")
        }
    }

    buildFeatures{
        viewBinding = true
    }

    packagingOptions {
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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

    // Mapbox Navigation (only if you actually use Navigation APIs)
    implementation("com.mapbox.navigationcore:android-ndk27:3.17.0-rc.2")

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

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
