package com.example.gpsnavigation.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.location.LocationManagerCompat.isLocationEnabled
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.gpsnavigation.R
import com.example.gpsnavigation.ads.NewAdsManagerUpdated
import com.example.gpsnavigation.ads.loadAdaptiveBanner
import com.example.gpsnavigation.databinding.ActivityMainBinding
import com.example.gpsnavigation.utils.AdsRemoteConfig
import com.example.gpsnavigation.utils.MyConstants
import com.example.gpsnavigation.utils.PermissionUtils
import com.example.gpsnavigation.utils.setDebouncedClickListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private var interstitialBusy = false
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var progressDialog: ProgressDialog? = null
    private var adProgress: SweetAlertDialog? = null
    private val requestLocationPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fine || coarse) {
                proceedAfterPermissionGranted()
            } else {
                showPermissionDeniedDialog()
            }
        }
    private var shouldCreateLocationRequest = false

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L
        ).apply {
            setMinUpdateIntervalMillis(5_000L)
        }.build()

        startLocationUpdates()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (AdsRemoteConfig.GPS_home_largebanner_control) {
            /*if (BuildConfig.DEBUG) {
                binding.containerAd.loadAdaptiveBanner(this, "ca-app-pub-3940256099942544/6300978111")
            } else {*/
                /*logUserEvent(
                    this@MainActivity, "search_screen_banner", mapOf(
                        "button_name" to "MapBoxSearchCompleteActivity",
                        "screen" to "MapBoxSearchCompleteActivity"
                    )
                )*/
                binding.containerAd.loadAdaptiveBanner(this, AdsRemoteConfig.GPS_home_largebanner_id)

        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (isLocationEnabled()) {
                    onLocationPermissionGranted()
                } else {
                    showEnableLocationDialog()
                }
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(
                        this,
                        "Location permission is necessary for this feature.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showPermissionDeniedDialog()
                }
            }
        }


        binding.cardCompass.setOnClickListener {
            attemptFeatureWithAd(
                AdsRemoteConfig.GPS_features_int_id
            ) {
                val intent = Intent(this, CompassActivity::class.java)
                startActivity(intent)
            }

        }

        binding.cardMyLocation.setOnClickListener {
            attemptFeatureWithAd(
                AdsRemoteConfig.GPS_features_int_id
            ) {
                val intent = Intent(this, CurrentLocationActivity::class.java)
                startActivity(intent)
            }
        }

        binding.cardAreaCalc.setOnClickListener {
            attemptFeatureWithAd(
                AdsRemoteConfig.GPS_features_int_id
            ) {
                val intent = Intent(this, AreaCalculatorActivity::class.java)
                startActivity(intent)
            }
        }

        binding.cardSpeedometer.setOnClickListener {
            attemptFeatureWithAd(
                AdsRemoteConfig.GPS_features_int_id
            ) {
                val intent = Intent(this, SpeedometerActivity::class.java)
                startActivity(intent)
            }
        }

        binding.tvViewAll.setOnClickListener {
            val intent = Intent(this, NearbyPlacesListActivity::class.java)
            startActivity(intent)
        }



        binding.linearLayoutPetrolPump.setDebouncedClickListener {
            /*logUserEvent(
                this, "petrol_pump_button", mapOf(
                    "button_name" to "petrol_pump_button",
                    "screen" to "NewDashboardActivity"
                )
            )*/
            lifecycleScope.launch {
                val hasInternet = isInternetAvailable(applicationContext)

                if (!hasInternet) {
                    Toast.makeText(
                        this@MainActivity,
                        "No internet connection",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                if (checkLocationPermission()) {
                    if (isLocationEnabled()) {
                        onLocationPermissionGranted()
                        if (!AdsRemoteConfig.GPS_features_int_conntrol) {
                            val intent = Intent(applicationContext, DiscoverActivity::class.java)
                            intent.putExtra(
                                "QUERY_VALUE",
                                resources.getString(R.string.pterol_pump)
                            )
                            startActivity(intent)
                        } else {
                            attemptFeatureWithAd(AdsRemoteConfig.GPS_features_int_id) {
                                val intent =
                                    Intent(applicationContext, DiscoverActivity::class.java)
                                intent.putExtra(
                                    "QUERY_VALUE",
                                    resources.getString(R.string.pterol_pump)
                                )
                                startActivity(intent)
                            }
                        }
                    } else {
                        showEnableLocationDialog()
                    }
                } else {
                    requestLocationPermission()
                }
            }
        }

        binding.linearLayoutHospital.setDebouncedClickListener {
            /*logUserEvent(
                this, "hospital_button", mapOf(
                    "button_name" to "hospital_button",
                    "screen" to "NewDashboardActivity"
                )
            )*/
            lifecycleScope.launch {
                val hasInternet = isInternetAvailable(applicationContext)

                if (!hasInternet) {
                    Toast.makeText(
                        this@MainActivity,
                        "No internet connection",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                if (checkLocationPermission()) {
                    if (isLocationEnabled()) {
                        onLocationPermissionGranted()
                        if (!AdsRemoteConfig.GPS_features_int_conntrol) {
                            val intent = Intent(applicationContext, DiscoverActivity::class.java)
                            intent.putExtra("QUERY_VALUE", resources.getString(R.string.hospital))
                            startActivity(intent)
                        } else {
                            attemptFeatureWithAd(AdsRemoteConfig.GPS_features_int_id) {
                                val intent =
                                    Intent(applicationContext, DiscoverActivity::class.java)
                                intent.putExtra(
                                    "QUERY_VALUE",
                                    resources.getString(R.string.hospital)
                                )
                                startActivity(intent)
                            }
                        }

                    } else {
                        showEnableLocationDialog()
                    }
                } else {
                    requestLocationPermission()
                }
            }
        }

        binding.linearLayoutGroceryStore.setDebouncedClickListener {
            /*logUserEvent(
                this, "grocery_store_button", mapOf(
                    "button_name" to "grocery_store_button",
                    "screen" to "NewDashboardActivity"
                )
            )*/
            lifecycleScope.launch {
                val hasInternet = isInternetAvailable(applicationContext)

                if (!hasInternet) {
                    Toast.makeText(
                        this@MainActivity,
                        "No internet connection",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                if (checkLocationPermission()) {
                    if (isLocationEnabled()) {
                        onLocationPermissionGranted()

                        if (!AdsRemoteConfig.GPS_features_int_conntrol) {
                            val intent = Intent(applicationContext, DiscoverActivity::class.java)
                            intent.putExtra(
                                "QUERY_VALUE",
                                resources.getString(R.string.grocery_store)
                            )  // <-- Pass the string
                            startActivity(intent)
                        } else {
                            attemptFeatureWithAd(AdsRemoteConfig.GPS_features_int_id) {
                                val intent =
                                    Intent(applicationContext, DiscoverActivity::class.java)
                                intent.putExtra(
                                    "QUERY_VALUE",
                                    resources.getString(R.string.grocery_store)
                                )  // <-- Pass the string
                                startActivity(intent)
                            }
                        }


                    } else {
                        showEnableLocationDialog()
                    }
                } else {
                    requestLocationPermission()
                }
            }
        }

        binding.linearLayoutBusStop.setDebouncedClickListener {
            /*logUserEvent(
                this, "button_click", mapOf(
                    "button_name" to "bus_stop_button",
                    "screen" to "NewDashboardActivity"
                )
            )*/
            lifecycleScope.launch {
                val hasInternet = isInternetAvailable(applicationContext)

                if (!hasInternet) {
                    Toast.makeText(
                        this@MainActivity,
                        "No internet connection",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                if (checkLocationPermission()) {
                    if (isLocationEnabled()) {
                        onLocationPermissionGranted()

                        if (!AdsRemoteConfig.GPS_features_int_conntrol) {
                            val intent = Intent(applicationContext, DiscoverActivity::class.java)
                            intent.putExtra(
                                "QUERY_VALUE",
                                resources.getString(R.string.bus_stop)
                            )  // <-- Pass the string
                            startActivity(intent)
                        } else {
                            attemptFeatureWithAd(AdsRemoteConfig.GPS_features_int_id) {
                                val intent =
                                    Intent(applicationContext, DiscoverActivity::class.java)
                                intent.putExtra(
                                    "QUERY_VALUE",
                                    resources.getString(R.string.bus_stop)
                                )  // <-- Pass the string
                                startActivity(intent)
                            }
                        }


                    } else {
                        showEnableLocationDialog()
                    }
                } else {
                    requestLocationPermission()
                }
            }
        }


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    MyConstants.currentLatLng = LatLng(location.latitude, location.longitude)
                    MyConstants.sourceName = "Your Location"
                    dismissProgressDialog()

                }
            }
        }

        binding.cardSearch.setOnClickListener {
            val intent = Intent(this, MapBoxSearchCompleteActivity::class.java)
            startActivity(intent)
        }

        binding.cardRouteFinder.setOnClickListener {
            val intent = Intent(this, MapBoxSearchCompleteActivity::class.java)
            startActivity(intent)
        }

        checkPermissionThenLocationStatus()



    }

    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }


    private fun showEnableLocationDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                dialog.cancel()
                shouldCreateLocationRequest = true
                openLocationSettings()
            }
            .setNegativeButton("No", null)


        val alert = builder.create()
        alert.show()
    }


    private fun attemptFeatureWithAd(adUnitId: String, onContinue: () -> Unit) {
        if (interstitialBusy /*|| PrefsManager.isPremium(application)*/) {
            onContinue(); return
        }

        interstitialBusy = true
        adProgress = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
            progressHelper.barColor = "#A5DC86".toColorInt()
            titleText = "Loading Ad"
            contentText = "Please wait"
            setCancelable(false)
            show()
        }

        var responded = false


        lifecycleScope.launch {
            delay(6000)
            if (!responded && !isFinishing && !isDestroyed) {
                dismissAdDialog()
                interstitialBusy = false
                onContinue()
            }
        }


        NewAdsManagerUpdated.getInstance().loadAdMobInterstitialAds(
            this, /*if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else*/ adUnitId,
            object : NewAdsManagerUpdated.InterstitalAdListener {
                override fun isAdLoaded() {
                    if (responded) return
                    responded = true
                    dismissAdDialog()

                    NewAdsManagerUpdated.getInstance()
                        .showInterstitialAd(this@MainActivity) {
                            interstitialBusy = false
                            onContinue()
                        }
                }

                override fun isAdError() {
                    if (responded) return
                    responded = true
                    dismissAdDialog()
                    interstitialBusy = false
                    onContinue()
                }
            }
        )
    }

    private fun dismissAdDialog() {
        try {
            adProgress?.dismiss()
        } catch (_: Throwable) {
        }
        adProgress = null
    }

    private fun dismissProgressDialog() {
        if (isFinishing || isDestroyed) {
            return
        }

        progressDialog?.let {
            if (it.isShowing) {
                try {
                    it.dismiss()
                } catch (e: IllegalArgumentException) {
                    e.message
                }
            }
        }
    }

    private fun checkPermissionThenLocationStatus() {
        val hasFine = PermissionUtils.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarse = PermissionUtils.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (hasFine || hasCoarse) {
            proceedAfterPermissionGranted()
        } else {
            requestLocationPermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun proceedAfterPermissionGranted() {
        if (isDeviceLocationEnabled()) {
            createLocationRequest()
//            startActivity(Intent(this, MapActivity::class.java))
        } else {
            showTurnOnLocationDialog()
        }
    }

    private fun isDeviceLocationEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Works broadly; checks if at least one provider is enabled.
        val gpsEnabled = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        val networkEnabled = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        return gpsEnabled || networkEnabled
    }

    private fun showTurnOnLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Turn on Location")
            .setMessage("Your device location is OFF. Please turn it ON to show your current position on the map.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("Location permission is required to show your current location on the map.")
            .setPositiveButton("OK", null)
            .show()
    }

    suspend fun isInternetAvailable(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val connectivityManager =
                context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return@withContext false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return@withContext false

            if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                return@withContext false

            // Real connectivity test (won’t block UI)
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                return@withContext true
            }
        } catch (e: IOException) {
            e.message
            return@withContext false
        }
    }
    private fun onLocationPermissionGranted() {
        showProgressDialog()
        createLocationRequest()
    }

    private fun showProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Fetching location...")
            setCancelable(false)
            show()
        }
    }
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}