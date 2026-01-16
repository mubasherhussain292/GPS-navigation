package com.example.gpsnavigation.activities

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.gpsnavigation.R
import com.example.gpsnavigation.ads.loadAdaptiveBanner
import com.example.gpsnavigation.db.AppDatabase
import com.example.gpsnavigation.db.NavSessionEntity
import com.example.gpsnavigation.extensions.reverseGeocodeName
import com.example.gpsnavigation.mapboxresponse.MapboxResponse
import com.example.gpsnavigation.mapboxresponse.Steps
import com.example.gpsnavigation.utils.AdsRemoteConfig
import com.example.gpsnavigation.utils.MyConstants
import com.example.gpsnavigation.utils.setDebouncedClickListener
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class NavigationActivity : AppCompatActivity() {

    var TAG = NavigationActivity::class.java.simpleName
    var mapViewNavigationActivity: MapView? = null
    var distanceKM = 0.0
    lateinit var textViewInstructionsText: TextView
    private var previousLocation: Point? = null
    private val MIN_DISTANCE_THRESHOLD = 10.0
    private var stepsList: ArrayList<Steps> = ArrayList()
    var currentLat: Double = 0.0
    var currentLng: Double = 0.0
    private var previousLocationSpeed: Location? = null
    private var previousTime: Long = 0
    lateinit var textViewSpeed: TextView
    lateinit var textViewTime: TextView
    val mFeatureList: ArrayList<Point> = ArrayList()
    lateinit var nearestPoint: Point
    val layerIdToCover = "layer-id-to-cover"
    val sourceIdToCover = "source-id-to-cover"
    lateinit var currentStyle: Style
    private var shouldPlay = true
    lateinit var imageButtonStop: ImageView
    lateinit var imageViewSound: ImageView
    lateinit var relativeLayoutDistanceUnit: RelativeLayout
    private var isKM = true
    lateinit var textViewDistanceUnit: TextView
    lateinit var textViewSpeedUnit: TextView
    lateinit var imageViewTurnInstruction: ImageView
    lateinit var relativeLayoutExit: RelativeLayout
    lateinit var relativeLayoutNo: RelativeLayout
    lateinit var relativeLayoutYes: RelativeLayout
    lateinit var imageViewMyLocation: ImageView
    lateinit var textViewDestinationLocation: TextView
    lateinit var textViewRemainingDistance: TextView
    lateinit var imageButtonDrive: ImageView
    private var routeDurationSec: Int = 0
    private var routeStartTimeMillis: Long = 0
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private val announceRadiusMeters = 15.0
    private val spokenSteps = mutableSetOf<Int>()
    private var lastSpokenTimeMillis: Long = 0
    private val minSpeakIntervalMillis = 1500L
    lateinit var container: FrameLayout
    private var movingArrowAnnotation: PointAnnotation? = null
    private var movingArrowManager: PointAnnotationManager? = null
    private var isFollowingUser = true
    private var activeSessionId: Long? = null
    private var trackFilePath: String? = null
    private var trackWriter: BufferedWriter? = null

    private var lastSavedPoint: Point? = null
    private var lastSavedTimeMillis: Long = 0L
    private val MIN_METERS_BETWEEN_SAVES = 100.0
    private val MIN_TIME_BETWEEN_SAVES_MS = 10_000L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val routeType = intent.getStringExtra("routetype")
        mapViewNavigationActivity = findViewById(R.id.mapViewNavigationActivity)
        textViewInstructionsText = findViewById(R.id.textViewInstructionsText)
        textViewSpeed = findViewById(R.id.textViewSpeed)
        textViewTime = findViewById(R.id.textViewTime)
        imageButtonStop = findViewById(R.id.imageButtonStop)
        imageViewSound = findViewById(R.id.imageViewSound)
        relativeLayoutDistanceUnit = findViewById(R.id.relativeLayoutDistanceUnit)
        textViewDistanceUnit = findViewById(R.id.textViewDistanceUnit)
        textViewSpeedUnit = findViewById(R.id.textViewSpeedUnit)
        imageViewTurnInstruction = findViewById(R.id.imageViewTurnInstruction)
        imageViewMyLocation = findViewById(R.id.imageViewMyLocation)
        textViewDestinationLocation = findViewById(R.id.textViewDestinationLocation)
        textViewRemainingDistance = findViewById(R.id.textViewRemainingDistance)
        relativeLayoutExit = findViewById(R.id.relativeLayoutExit)
        relativeLayoutYes = findViewById(R.id.relativeLayoutYes)
        relativeLayoutNo = findViewById(R.id.relativeLayoutNo)
        imageButtonDrive = findViewById(R.id.imageButtonDrive)
        container = findViewById(R.id.container)

        if (AdsRemoteConfig.GPS_features_banner_control) {
            /*if (BuildConfig.DEBUG) {
                container.loadAdaptiveBanner(this, "ca-app-pub-3940256099942544/6300978111")
            } else {*/
                /*logUserEvent(
                    this, "navigation_banner", mapOf(
                        "button_name" to "navigation",
                        "screen" to "navigation"
                    )
                )*/
                container.loadAdaptiveBanner(this, AdsRemoteConfig.GPS_features_banner_id)


        }


        if (MyConstants.currentLatLng != null && MyConstants.destLatLng != null) {
            initMap()
        } else {
            Toast.makeText(
                this@NavigationActivity,
                "Unexpected Error, Please try again",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }

        initTTS()

        imageViewMyLocation.visibility = View.GONE

        /*if (AdsRemoteConfig.banner_route_draw_controll) {
            if (BuildConfig.DEBUG) {
                container.loadAdaptiveBanner(this, "ca-app-pub-3940256099942544/6300978111")
            } else {
                logUserEvent(
                    this, "navigation_banner", mapOf(
                        "button_name" to "navigation",
                        "screen" to "navigation"
                    )
                )
                container.loadAdaptiveBanner(this, AdsRemoteConfig.banner_route_draw)

            }
        }*/

        imageViewMyLocation.setDebouncedClickListener {
            /*logUserEvent(
                this, "my_location_button", mapOf(
                    "button_name" to "my_location_button",
                    "screen" to "NavigationActivity"
                )
            )*/

            isFollowingUser = true
            showRecenterButton(false)


            val targetPoint = movingArrowAnnotation?.point
                ?: Point.fromLngLat(
                    MyConstants.currentLatLng.longitude,
                    MyConstants.currentLatLng.latitude
                )

            moveCameraToLocation(targetPoint)
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                /*logUserEvent(
                    this@NavigationActivity, "stop_button", mapOf(
                        "button_name" to "stop_button",
                        "screen" to "NavigationActivity"
                    )
                )*/
                relativeLayoutExit.visibility = View.VISIBLE
            }
        })

        imageButtonStop.setDebouncedClickListener {
            /*logUserEvent(
                this, "stop_button", mapOf(
                    "button_name" to "stop_button",
                    "screen" to "NavigationActivity"
                )
            )*/
            relativeLayoutExit.visibility = View.VISIBLE

        }

        relativeLayoutYes.setDebouncedClickListener {
            /*logUserEvent(
                this, "exit_navigation", mapOf(
                    "button_name" to "exit_yes_button",
                    "screen" to "NavigationActivity"
                )
            )*/
            endNavSession()
            finish()
        }

        relativeLayoutNo.setDebouncedClickListener {
            /*logUserEvent(
                this, "not_nav_exit", mapOf(
                    "button_name" to "exit_no_button",
                    "screen" to "NavigationActivity"
                )
            )*/
            relativeLayoutExit.visibility = View.INVISIBLE
        }

        imageViewSound.setDebouncedClickListener {
            shouldPlay = !shouldPlay

            if (shouldPlay) {
                imageViewSound.setImageDrawable(getDrawable(R.drawable.navigationsoundbg))
            } else {
                imageViewSound.setImageDrawable(getDrawable(R.drawable.navigationsoundoffbg))
                tts.stop()
            }

        }

        textViewDestinationLocation.text = MyConstants.destName

        when (routeType) {
            "car" -> {
                imageButtonDrive.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@NavigationActivity,
                        R.drawable.carselectedbg
                    )
                )
            }

            "bike" -> {
                imageButtonDrive.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@NavigationActivity,
                        R.drawable.bikeselectedbg
                    )
                )
            }

            "walk" -> {
                imageButtonDrive.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@NavigationActivity,
                        R.drawable.walkselectedbg
                    )
                )
            }

            else -> {}
        }
    }

    private fun initTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale.getDefault()
                val result = tts.setLanguage(locale)
                ttsReady =
                    !(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                Log.d(TAG, "TTS ready=$ttsReady locale=$locale result=$result")
            } else {
                ttsReady = false
                Log.w(TAG, "TTS init failed with status $status")
            }
        }
    }

    override fun onDestroy() {
        try {
            if (::tts.isInitialized) {
                tts.stop()
                tts.shutdown()
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Error shutting down TTS", ex)
        }
        super.onDestroy()
    }

    private fun initMap() {

        /*logUserEvent(
            this, "navigation_map", mapOf(
                "button_name" to "navigation",
                "screen" to "navigation"
            )
        )*/


        mapViewNavigationActivity?.mapboxMap
            ?.loadStyleUri(Style.MAPBOX_STREETS) { style ->

                currentStyle = style
                initLocationComponent()
                showCameratoMap()
                setupGestureListeners()
                if (MyConstants.mapboxResponse != null) {
                    drawRoute(MyConstants.mapboxResponse)
                } else {
                    Toast.makeText(
                        this@NavigationActivity,
                        "Unexpected Error, Please try again",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
    }

    private fun setupGestureListeners() {
        val gestures = mapViewNavigationActivity?.gestures ?: return

        // When user drags the map
        gestures.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                isFollowingUser = false
                showRecenterButton(true)
            }

            override fun onMove(detector: MoveGestureDetector): Boolean = false
            override fun onMoveEnd(detector: MoveGestureDetector) {}
        })

        // When user pinch-zooms the map
        gestures.addOnScaleListener(object : OnScaleListener {
            override fun onScale(detector: StandardScaleGestureDetector) {

            }

            override fun onScaleBegin(detector: StandardScaleGestureDetector) {
                isFollowingUser = false
                showRecenterButton(true)
            }

            override fun onScaleEnd(detector: StandardScaleGestureDetector) {}
        })
    }

    private fun showRecenterButton(visible: Boolean) {
        imageViewMyLocation.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun drawRoute(mapboxResponse: MapboxResponse?) {
        val seconds = mapboxResponse!!.routes[0].duration
        distanceKM = mapboxResponse.routes[0].distance!!
        distanceKM /= 1000

        routeDurationSec = mapboxResponse.routes[0].duration!!.toInt()
        routeStartTimeMillis = System.currentTimeMillis()

        updateRemainingTime()

        val time: String = seconds?.let { calculateTime(it) }!!
        textViewTime.text = time
        showDistance(distanceKM)

        val currentTime = Calendar.getInstance()
        currentTime.add(Calendar.SECOND, seconds.toInt())

        stepsList = mapboxResponse.routes[0].legs[0].steps


        spokenSteps.clear()

        val voiceText =
            mapboxResponse.routes[0].legs[0].steps[0].voiceInstructions[0].announcement

        addInstructionImage(voiceText)
        checkInstructionsUnit(voiceText)

        mapViewNavigationActivity?.mapboxMap?.getStyle {
            mFeatureList.clear()
            for (i in 0 until mapboxResponse.routes[0].geometry!!.coordinates.size) {
                mFeatureList.add(
                    Point.fromLngLat(
                        (mapboxResponse.routes[0].geometry!!.coordinates[i][0]),
                        mapboxResponse.routes[0].geometry!!.coordinates[i][1]
                    )
                )
            }

            nearestPoint = mFeatureList[0]


            val lineString = LineString.fromLngLats(mFeatureList)
            val feature = Feature.fromGeometry(lineString)
            val featureCollection = FeatureCollection.fromFeatures(listOf(feature))

            val geoJsonSource = GeoJsonSource.Builder(sourceIdToCover)
                .featureCollection(featureCollection)
                .build()

            it.addSource(geoJsonSource)

            val lineLayer = LineLayer(layerIdToCover, sourceIdToCover)
                .lineWidth(8.0)
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineColor(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.blueroutedrawselected
                    )
                )
            it.addLayer(lineLayer)


            startNavSessionAndOpenFile(
                routeType = intent.getStringExtra("routetype") ?: "car",
                totalDistanceMeters = (mapboxResponse.routes[0].distance ?: 0.0),
                totalDurationSec = (mapboxResponse.routes[0].duration ?: 0.0).toInt()
            )
        }

        val annotationApi: AnnotationPlugin? =
            mapViewNavigationActivity?.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID)
        val pointAnnotationManager = annotationApi?.createPointAnnotationManager()

        val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(
                MyConstants.destLatLng.longitude,
                MyConstants.destLatLng.latitude
            ))
            .withIconImage(drawableToBitmap(AppCompatResources.getDrawable(this@NavigationActivity, R.drawable.red_marker)!!)!!)
        pointAnnotationManager?.create(pointAnnotationOptions)

        movingArrowManager = annotationApi?.createPointAnnotationManager()

        val startPoint = mFeatureList.first()
        val secondPoint = if (mFeatureList.size > 1) mFeatureList[1] else startPoint
        val initialBearing = bearingBetween(startPoint, secondPoint)

        val startMarkerOptions = PointAnnotationOptions()
            .withPoint(startPoint)
            .withIconImage(
                AppCompatResources.getDrawable(
                    this@NavigationActivity,
                    R.drawable.current_moving_arrow
                )!!.toBitmap()
            )
            .withIconSize(1.2)
            .withIconRotate(initialBearing)

        movingArrowAnnotation = movingArrowManager?.create(startMarkerOptions)


    }


    private fun bearingBetween(p1: Point, p2: Point): Double {
        val lat1 = Math.toRadians(p1.latitude())
        val lon1 = Math.toRadians(p1.longitude())
        val lat2 = Math.toRadians(p2.latitude())
        val lon2 = Math.toRadians(p2.longitude())

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        var brng = Math.toDegrees(atan2(y, x))
        brng = (brng + 360.0) % 360.0
        return brng
    }

    fun updateMovingArrow(currentIndex: Int) {
        val arrow = movingArrowAnnotation ?: return
        val manager = movingArrowManager ?: return
        if (mFeatureList.isEmpty()) return

        val i = currentIndex.coerceIn(0, mFeatureList.size - 1)
        val currentPoint = mFeatureList[i]
        val nextPoint = if (i < mFeatureList.size - 1) mFeatureList[i + 1] else currentPoint

        val bearing = bearingBetween(currentPoint, nextPoint)

        arrow.point = currentPoint
        arrow.iconRotate = bearing

        manager.update(arrow)
    }

    private fun updateRemainingRouteFromIndex(nearestIndex: Int) {
        if (mFeatureList.size < 2) return

        // If user is basically at the end, clear the route
        if (nearestIndex >= mFeatureList.size - 1) {
            mapViewNavigationActivity?.mapboxMap?.getStyle { style ->
                val source = style.getSourceAs<GeoJsonSource>(sourceIdToCover)
                source?.featureCollection(
                    FeatureCollection.fromFeatures(emptyList())
                )
            }
            return
        }

        // Sublist of remaining points (from user position to destination)
        val remainingPoints = mFeatureList.subList(nearestIndex, mFeatureList.size)

        val lineString = LineString.fromLngLats(remainingPoints)
        val feature = Feature.fromGeometry(lineString)
        val featureCollection = FeatureCollection.fromFeature(feature)

        mapViewNavigationActivity?.mapboxMap?.getStyle { style ->
            val source = style.getSourceAs<GeoJsonSource>(sourceIdToCover)
            source?.featureCollection(featureCollection)
        }
    }


    private fun updateRemainingTime() {
        val elapsedSec = ((System.currentTimeMillis() - routeStartTimeMillis) / 1000).toInt()
        val remainingSec = (routeDurationSec - elapsedSec).coerceAtLeast(0)

        textViewTime.text = calculateTime(remainingSec.toDouble())
    }

    private fun maybeAnnounceNextStep(userLat: Double, userLng: Double) {
        if (!ttsReady) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSpokenTimeMillis < minSpeakIntervalMillis) return

        for ((index, step) in stepsList.withIndex()) {
            val coords = step.geometry?.coordinates ?: continue

            val stepLat = coords.last()[1]
            val stepLng = coords.last()[0]

            val distance = calculateDistance(userLat, userLng, stepLat, stepLng)

            if (distance <= announceRadiusMeters && !spokenSteps.contains(index)) {
                val announcement = step.voiceInstructions.firstOrNull()?.announcement
                    ?: step.maneuver?.instruction
                    ?: continue

                speakInstruction(announcement)
                spokenSteps.add(index)
                lastSpokenTimeMillis = currentTime
                break
            }
        }
    }

    private fun speakInstruction(text: String) {
        if (!ttsReady) return
        if (shouldPlay) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }

        // Update UI too
        runOnUiThread {
            checkInstructionsUnit(text)
            addInstructionImage(text)
        }
    }

    private fun showDistance(distanceKM: Double) {

        if (isKM) {
//            textViewDistance.text = String.format("%.2f", distanceKM) + " Km"
        } else {
            val distanceInMiles = kilometersToMiles(distanceKM)
        }

    }

    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        var bitmap: Bitmap?
        val width = drawable.intrinsicWidth.coerceAtLeast(2)
        val height = drawable.intrinsicHeight.coerceAtLeast(2)
        try {
            bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.w(TAG, "Failed to create bitmap from drawable!")
            bitmap = null
        }
        return bitmap
    }

    fun kilometersToMiles(kilometers: Double): Double {
        val conversionFactor = 0.621371
        return kilometers * conversionFactor
    }


    private fun calculateTime(time: Double): String {
        var hour = 0
        var minutes = 0
        hour = (time / 3600).toInt()
        minutes = (time % 3600).toInt() / 60
        //        return "Time : " + hour + "hr, " + minutes + "min";
        return hour.toString() + "hr, " + minutes + "min "
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapViewNavigationActivity!!.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D()
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
    }

    private val onIndicatorPositionChangedListener =
        OnIndicatorPositionChangedListener { location ->

            val lat = location.latitude()
            val lng = location.longitude()

            val locationGoogle = Location("gps")
            locationGoogle.latitude = lat
            locationGoogle.longitude = lng

            if (shouldProcessLocationUpdate(location)) {
                currentLat = location.latitude()
                currentLng = location.longitude()

                // Update arrow + distance first
                if (mFeatureList.isNotEmpty()) {
                    val nearestIndex = findNearestRouteIndex(location)
                    updateMovingArrow(nearestIndex)
                    updateRemainingDistanceFromIndex(nearestIndex)
                    updateRemainingRouteFromIndex(nearestIndex)
                }

                // Then move camera if we're in follow mode
                if (isFollowingUser) {
                    followUserCamera()
                }

                updateRemainingTime()
                maybeAnnounceNextStep(currentLat, currentLng)

                lifecycleScope.launch(Dispatchers.IO) {
                    maybeAppendTrackPoint(location, speedKmh = null)
                }

            }

            val currentTime = System.currentTimeMillis()

            if (previousLocationSpeed != null) {
                val speed = calculateSpeed(locationGoogle, currentTime)

                if (isKM) {

                    if (speed.toInt() == 0) {
                        textViewSpeed.text = "--"

                    } else {
                        textViewSpeed.text = speed.toInt().toString()

                    }

                } else {
                    val distanceInMiles = kilometersToMiles(speed.toDouble())

                    if (distanceInMiles.toInt() == 0) {
                        textViewSpeed.text = "--"
                    } else {
                        textViewSpeed.text = distanceInMiles.toInt().toString()
                    }

                }

            }

            previousLocationSpeed = locationGoogle
            previousTime = currentTime
//            updateCurrentLocationMarker(lat, lng,locationGoogle.altitude)

        }

    private fun endNavSession() {
        val sessionId = activeSessionId ?: return
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                trackWriter?.flush()
            } catch (_: Exception) {
            }
            try {
                trackWriter?.close()
            } catch (_: Exception) {
            }
            trackWriter = null

            db.navSessionDao().endSession(sessionId, System.currentTimeMillis())
        }
    }

    private fun maybeAppendTrackPoint(userPoint: Point, speedKmh: Float?) {
        val writer = trackWriter ?: return

        val now = System.currentTimeMillis()

        val timeOk = (now - lastSavedTimeMillis) >= MIN_TIME_BETWEEN_SAVES_MS
        val distOk = lastSavedPoint?.let { prev ->
            distanceMeters(prev, userPoint) >= MIN_METERS_BETWEEN_SAVES
        } ?: true

        if (!timeOk && !distOk) return

        val line = buildString {
            append("""{"t":$now,"lat":${userPoint.latitude()},"lng":${userPoint.longitude()}""")
            if (speedKmh != null) append(""","spd":$speedKmh""")
            append("}\n")
        }

        writer.write(line)
        writer.flush()

        lastSavedPoint = userPoint
        lastSavedTimeMillis = now
    }

    private fun followUserCamera() {
        val mapboxMap = mapViewNavigationActivity?.mapboxMap ?: return

        // Prefer arrow position; fallback to currentLat/currentLng
        val targetPoint = movingArrowAnnotation?.point
            ?: Point.fromLngLat(currentLng, currentLat)

        val currentState = mapboxMap.cameraState

        val camera = CameraOptions.Builder()
            .center(targetPoint)
            .zoom(currentState.zoom)
            .pitch(currentState.pitch)
            .bearing(currentState.bearing)
            .build()

        mapboxMap.easeTo(
            camera,
            MapAnimationOptions.mapAnimationOptions {
                duration(800L)
            }
        )
    }

    private fun startNavSessionAndOpenFile(
        routeType: String,
        totalDistanceMeters: Double,
        totalDurationSec: Int
    ) {
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val startedAt = System.currentTimeMillis()

            val file = File(File(filesDir, "nav_tracks"), "session_$startedAt.ndjson")
            file.parentFile?.mkdirs()


            val startLat = MyConstants.currentLatLng.latitude
            val startLng = MyConstants.currentLatLng.longitude

            val startNameResolved = this@NavigationActivity.reverseGeocodeName(startLat, startLng)

            val sessionId = db.navSessionDao().insertSession(
                NavSessionEntity(
                    startedAtMillis = startedAt,
                    routeType = routeType,
                    startName = startNameResolved,
                    startLat = MyConstants.currentLatLng.latitude,
                    startLng = MyConstants.currentLatLng.longitude,
                    endName = MyConstants.destName,
                    endLat = MyConstants.destLatLng.latitude,
                    endLng = MyConstants.destLatLng.longitude,
                    trackFilePath = file.absolutePath,
                    totalDistanceMeters = totalDistanceMeters,
                    totalDurationSec = totalDurationSec
                )
            )
            activeSessionId = sessionId
            trackFilePath = file.absolutePath

            trackWriter =
                BufferedWriter(OutputStreamWriter(FileOutputStream(file, true), Charsets.UTF_8))

            lastSavedPoint = null
            lastSavedTimeMillis = 0L
        }
    }


    private fun updateRemainingDistanceFromIndex(nearestIndex: Int) {
        if (mFeatureList.size < 2) return

        var remainingMeters = 0.0

        for (i in nearestIndex until mFeatureList.size - 1) {
            val a = mFeatureList[i]
            val b = mFeatureList[i + 1]
            remainingMeters += distanceMeters(a, b)
        }

        val remainingKm = remainingMeters / 1000.0

        if (isKM) {
            textViewRemainingDistance.text =
                String.format(Locale.getDefault(), "%.2f km", remainingKm)
        } else {
            val remainingMiles = kilometersToMiles(remainingKm)
            textViewRemainingDistance.text =
                String.format(Locale.getDefault(), "%.2f miles", remainingMiles)
        }
    }

    private fun distanceMeters(a: Point, b: Point): Double {
        val R = 6371000.0

        val lat1 = Math.toRadians(a.latitude())
        val lon1 = Math.toRadians(a.longitude())
        val lat2 = Math.toRadians(b.latitude())
        val lon2 = Math.toRadians(b.longitude())

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val sinLat = sin(dLat / 2)
        val sinLon = sin(dLon / 2)

        val aa = sinLat * sinLat + cos(lat1) * cos(lat2) * sinLon * sinLon
        val c = 2 * atan2(sqrt(aa), sqrt(1 - aa))

        return R * c
    }

    private fun findNearestRouteIndex(point: Point): Int {
        if (mFeatureList.isEmpty()) return 0

        var minDist = Double.MAX_VALUE
        var minIndex = 0

        mFeatureList.forEachIndexed { index, routePoint ->
            val d = distanceBetweenPoints(point, routePoint)
            if (d < minDist) {
                minDist = d
                minIndex = index
            }
        }

        return minIndex
    }

    private fun distanceBetweenPoints(a: Point, b: Point): Double {
        val dx = a.longitude() - b.longitude()
        val dy = a.latitude() - b.latitude()
        return sqrt(dx * dx + dy * dy)
    }

    private fun updateCamera() {

        val mapAnimationOptions = MapAnimationOptions.Builder().duration(1500L).build()
        mapViewNavigationActivity!!.camera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(currentLng, currentLat))
                .zoom(18.0)
                .padding(EdgeInsets(500.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptions
        )
    }

    private fun calculateSpeed(currentLocation: Location, currentTime: Long): Float {
        val distance = previousLocationSpeed?.distanceTo(currentLocation) ?: 0f
        val timeInSeconds = (currentTime - previousTime) / 1000.0f
        val speed = distance / timeInSeconds
        return speed * 3.6f
    }


    private fun checkInstructionsUnit(voiceText: String?) {

        if (isKM) {


            if (voiceText!!.contains("mile", ignoreCase = true)) {
                val updatedVoiceText = voiceText.replace("mile", "km", ignoreCase = true)
                textViewInstructionsText.text = updatedVoiceText
            } else {
                textViewInstructionsText.text = voiceText
            }
        } else {
            if (voiceText!!.contains("km", ignoreCase = true)) {
                val updatedVoiceText = voiceText.replace("km", "mile", ignoreCase = true)
                textViewInstructionsText.text = updatedVoiceText
            } else {
                textViewInstructionsText.text = voiceText
            }
        }

    }

    private fun addInstructionImage(voiceText: String?) {

        val lowerCaseVoiceText = voiceText!!.lowercase(Locale.getDefault())
        if (lowerCaseVoiceText.contains("left")) {
            println("The voiceText contains the word 'left'.")
            imageViewTurnInstruction.setImageDrawable(getDrawable(R.drawable.turnleftinstructionsbg))

        } else if (lowerCaseVoiceText.contains("right")) {
            println("The voiceText contains the word 'right'.")
            imageViewTurnInstruction.setImageDrawable(getDrawable(R.drawable.turnrightinstructionsbg))
        } else {
            imageViewTurnInstruction.setImageDrawable(getDrawable(R.drawable.straightinstructionsbg))

        }

    }

    private fun shouldProcessLocationUpdate(newLocation: Point): Boolean {

        if (previousLocation == null) {
            previousLocation = newLocation
            return true
        }

        val distance = calculateDistance(
            previousLocation!!.latitude(),
            previousLocation!!.longitude(),
            newLocation.latitude(),
            newLocation.longitude()
        )

        if (distance >= MIN_DISTANCE_THRESHOLD) {
            previousLocation = newLocation
            return true
        }

        return false
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }


    fun calculateDistancee(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        // The radius of the Earth in kilometers
        val earthRadius = 6371.0

        // Convert latitude and longitude from degrees to radians
        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        // Haversine formula
        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad

        val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Calculate the distance in kilometers
        val distance = earthRadius * c

        return distance
    }


    private fun showCameratoMap() {

        val cameraPosition = CameraOptions.Builder()
            .center(
                Point.fromLngLat(
                    MyConstants.currentLatLng.longitude,
                    MyConstants.currentLatLng.latitude
                )
            )
            .zoom(16.0)
            .build()

        mapViewNavigationActivity!!.getMapboxMap().setCamera(cameraPosition)

    }

    fun moveCameraToLocation(targetPoint: Point, zoom: Double? = null) {
        val mapboxMap = mapViewNavigationActivity?.getMapboxMap() ?: return

        val finalZoom = zoom ?: mapboxMap.cameraState.zoom

        mapboxMap.flyTo(
            CameraOptions.Builder()
                .center(targetPoint)
                .zoom(finalZoom)
                .build(),
            MapAnimationOptions.mapAnimationOptions {
                duration(1000L)
            }
        )
    }


}