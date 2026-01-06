package com.example.gpsnavigation.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gpsnavigation.R
import com.example.gpsnavigation.databinding.ActivityMainBinding
import com.example.gpsnavigation.utils.MyConstants
import com.example.gpsnavigation.utils.PermissionUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity() {
    lateinit var biniding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var progressDialog: ProgressDialog? = null
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


    private lateinit var locationCallback: LocationCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        biniding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(biniding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        biniding.compass.setOnClickListener {
            val intent = Intent(this, CompassActivity::class.java)
            startActivity(intent)
        }

        biniding.currentLocation.setOnClickListener {
            val intent = Intent(this, CurrentLocationActivity::class.java)
            startActivity(intent)
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

        biniding.route.setOnClickListener {
            val intent = Intent(this, MapBoxSearchCompleteActivity::class.java)
            startActivity(intent)
        }
        checkPermissionThenLocationStatus()



    }


    private fun showProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Fetching location...")
            setCancelable(false)
            show()
        }
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


}