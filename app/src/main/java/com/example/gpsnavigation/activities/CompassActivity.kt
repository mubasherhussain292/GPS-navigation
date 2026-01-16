package com.example.gpsnavigation.activities

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gpsnavigation.R

class CompassActivity : AppCompatActivity() , SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private lateinit var compassNeedle: ImageView

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    private var currentAzimuth = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_compass)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<View>(R.id.ivBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        compassNeedle = findViewById(R.id.iv_compass_needle)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, gravity, 0, 3)
            Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, geomagnetic, 0, 3)
            else -> return
        }

        val Rm = FloatArray(9)
        val Im = FloatArray(9)
        val success = SensorManager.getRotationMatrix(Rm, Im, gravity, geomagnetic)
        if (!success) return

        val orientation = FloatArray(3)
        SensorManager.getOrientation(Rm, orientation)

        // Azimuth in degrees
        val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val fixedAzimuth = (azimuth + 360f) % 360f

        // Rotate needle (negative to match compass direction)
        compassNeedle.rotation = -fixedAzimuth

        currentAzimuth = fixedAzimuth
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}