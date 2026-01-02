package com.example.gpsnavigation.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gpsnavigation.R
import com.example.gpsnavigation.databinding.ActivityMainBinding
import com.example.gpsnavigation.utils.PermissionUtils

class MainActivity : AppCompatActivity() {
    lateinit var biniding: ActivityMainBinding


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

        biniding.compass.setOnClickListener {
            val intent = Intent(this, CompassActivity::class.java)
            startActivity(intent)
        }

        biniding.currentLocation.setOnClickListener {
            val intent = Intent(this, CurrentLocationActivity::class.java)
            startActivity(intent)
        }

        biniding.route.setOnClickListener {
            val intent = Intent(this, RouteInputActivity::class.java)
            startActivity(intent)
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