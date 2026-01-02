package com.example.gpsnavigation.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.gpsnavigation.databinding.ActivityCurrentLocationBinding
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.viewport

class CurrentLocationActivity : AppCompatActivity() {


    private lateinit var binding: ActivityCurrentLocationBinding

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted =
                (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                        (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)

            if (granted) {
                enableUserLocation()
            } else {
                Toast.makeText(this, "Location permission is required to show your current location.", Toast.LENGTH_LONG).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrentLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.awBtnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Apply only TOP inset to the top bar (keep map full screen)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.awTopBar.updatePadding(top = bars.top)
            insets
        }

        // Load style then enable location if permitted
        binding.mapView.mapboxMap.loadStyle(Style.STANDARD) {
            if (hasLocationPermission()) {
                enableUserLocation()
            } else {
                requestLocationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun enableUserLocation() = with(binding.mapView) {
        // Location component
        location.locationPuck = createDefault2DPuck(withBearing = true)
        // If your SDK complains about Context param, use this instead:
        // location.locationPuck = location.createDefault2DPuck(this@CurrentLocationActivity, withBearing = true)

        location.enabled = true
        location.puckBearing = PuckBearing.HEADING
        location.puckBearingEnabled = true
        location.pulsingEnabled = true

        // Camera follows puck (current location)
        viewport.transitionTo(
            targetState = viewport.makeFollowPuckViewportState(),
            transition = viewport.makeImmediateViewportTransition()
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }
}