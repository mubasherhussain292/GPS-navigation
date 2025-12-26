plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.gpsnavigation"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gpsnavigation"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.mapbox.maps:android:11.4.0")
    implementation( "com.mapbox.navigation:android:2.20.0")

    implementation ("com.mapbox.search:mapbox-search-android:2.3.0")

    // Location support
    implementation ("com.mapbox.navigation:ui-maps:2.20.0")
    implementation( "com.mapbox.navigation:ui-components:2.20.0")
    implementation ("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.github.anastr:speedviewlib:1.6.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")

}