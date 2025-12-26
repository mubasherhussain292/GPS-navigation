package com.example.gpsnavigation.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gpsnavigation.databinding.ActivitySpeedometerBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

class SpeedometerActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySpeedometerBinding

    private lateinit var fusedClient: FusedLocationProviderClient

    private var isRunning = false
    private var startElapsedRealtimeMs = 0L
    private var pendingStart = false

    private val uiHandler = Handler(Looper.getMainLooper())
    private val timeTicker = object : Runnable {
        override fun run() {
            if (!isRunning) return
            val elapsed = SystemClock.elapsedRealtime() - startElapsedRealtimeMs
            binding.tvRunningTime.text = formatElapsed(elapsed)
            uiHandler.postDelayed(this, 1000L)
        }
    }

    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(1000L)
            .setWaitForAccurateLocation(false)
            .build()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            val kmh = (loc.speed * 3.6f).coerceAtLeast(0f)
            binding.tvSpeed.text = String.format(Locale.US, "%.0f km/h", kmh)
            binding.speedView.speedTo(kmh.coerceAtMost(binding.speedView.maxSpeed))
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fine = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val ok = fine || coarse
        if (ok && pendingStart) {
            pendingStart = false
            start()
        } else {
            pendingStart = false
            stop()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySpeedometerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        binding.speedView.maxSpeed = 200f
        binding.speedView.speedTo(0f)
        binding.tvSpeed.text = "0 km/h"
        binding.tvRunningTime.text = "00:00:00"
        binding.btnStartStop.text = "Start"

        binding.btnStartStop.setOnClickListener {
            if (isRunning) stop() else start()
        }
    }

    private fun start() {
        if (!hasLocationPermission()) {
            pendingStart = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        if (isRunning) return
        isRunning = true
        binding.btnStartStop.text = "Stop"

        startElapsedRealtimeMs = SystemClock.elapsedRealtime()
        uiHandler.removeCallbacks(timeTicker)
        uiHandler.post(timeTicker)

        binding.speedView.speedTo(0f)
        binding.tvSpeed.text = "0 km/h"

        fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun stop() {
        if (!isRunning) {
            binding.btnStartStop.text = "Start"
            return
        }

        isRunning = false
        binding.btnStartStop.text = "Start"

        uiHandler.removeCallbacks(timeTicker)
        fusedClient.removeLocationUpdates(locationCallback)

        binding.speedView.speedTo(0f)
        binding.tvSpeed.text = "0 km/h"
        binding.tvRunningTime.text = "00:00:00"
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        uiHandler.removeCallbacks(timeTicker)
        super.onDestroy()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun formatElapsed(ms: Long): String {
        val totalSec = ms / 1000L
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        val s = totalSec % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }
}