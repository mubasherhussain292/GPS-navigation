package com.example.gpsnavigation.activities

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.animation.BounceInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gpsnavigation.R
import com.example.gpsnavigation.utils.setDebouncedClickListener
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.turf.TurfMeasurement
import java.io.File
import java.io.FileOutputStream

class AreaCalculatorActivity : AppCompatActivity() {

    private lateinit var imageViewBack: ImageView
    private lateinit var ivInfo: ImageView

    private lateinit var areaText: TextView
    private lateinit var linearLayoutReset: LinearLayout
    private lateinit var linearLayoutUndo: LinearLayout
    private lateinit var linearLayoutCopy: LinearLayout
    private lateinit var linearLayoutShare: LinearLayout
    private lateinit var mapView: MapView
    private lateinit var pointManager: PointAnnotationManager
    private lateinit var polygonManager: PolygonAnnotationManager
    private lateinit var lineManager: PolylineAnnotationManager
    private lateinit var adContainer: FrameLayout

    private val tappedPoints = mutableListOf<Point>()
    private val pointAnnotations = mutableListOf<PointAnnotation>()


    private var lastKnownLocation: Point? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_area_calculator)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageViewBack = findViewById(R.id.imageViewBack)
        adContainer = findViewById(R.id.adContainer)
        ivInfo = findViewById(R.id.ivInfo)
        mapView = findViewById(R.id.map_view)
        areaText = findViewById(R.id.areaText)
        linearLayoutReset = findViewById(R.id.linearLayoutReset)
        linearLayoutUndo = findViewById(R.id.linearLayoutUndo)
        linearLayoutCopy = findViewById(R.id.linearLayoutCopy)
        linearLayoutShare = findViewById(R.id.linearLayoutShare)





        imageViewBack.setDebouncedClickListener {
            finish()
        }

        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            val annotationApi = mapView.getPlugin<AnnotationPlugin>(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID)!!

            pointManager = annotationApi.createPointAnnotationManager()
            polygonManager = annotationApi.createPolygonAnnotationManager()
            lineManager = annotationApi.createPolylineAnnotationManager()

            // ✅ Add red pin image to the style so we can use it
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.location_flag)
            style.addImage("red-pin", bitmap)

            enableUserLocation()

            // Listen for map clicks
            mapView.getMapboxMap().addOnMapClickListener { point ->
                handleTap(point)
                true
            }
        }

        linearLayoutReset.setOnClickListener {
            resetMap()
        }

        linearLayoutUndo.setOnClickListener {
            undoLastPoint()
        }

        linearLayoutCopy.setOnClickListener {
            copyAreaToClipboard()
        }

        linearLayoutShare.setOnClickListener {
            val areaTextStr = areaText.text.toString()
            if (areaTextStr.isNotEmpty() && !areaTextStr.contains("Tap once on the map to mark a point")) {
                // Extract numeric value from "Area: X km²"
                val areaKm = areaTextStr.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
                shareMapSnapshot(areaKm)
            } else {
                Toast.makeText(this, "No area calculated yet", Toast.LENGTH_SHORT).show()
            }

        }

        ivInfo.setOnClickListener {
            val areaTextStr = areaText.text.toString()
            if (areaTextStr.isNotEmpty() && !areaTextStr.contains("Tap once on the map to mark a point")) {
                // Extract numeric value from "Area: X km²"
                val areaKm = areaTextStr.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
                showConvertedAreaDialog(areaKm)
            } else {
                Toast.makeText(this, "No area calculated yet", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun resetMap() {
        tappedPoints.clear()
        pointManager.deleteAll()
        polygonManager.deleteAll()
        lineManager.deleteAll()

        areaText.text = "Tap once on the map to mark a point"

        // 👇 Recenter map if we have last known location
        lastKnownLocation?.let { point ->
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(14.0)
                    .build()
            )
        }
    }

    private fun undoLastPoint() {
        if (tappedPoints.isNotEmpty()) {
            // Remove last point and corresponding annotation
//            tappedPoints.removeLast()
            tappedPoints.removeAt(tappedPoints.lastIndex)

//            val lastAnnotation = pointAnnotations.removeLastOrNull()
//            lastAnnotation?.let { pointManager.delete(it) }

            val lastAnnotation = if (pointAnnotations.isNotEmpty())
                pointAnnotations.removeAt(pointAnnotations.lastIndex)
            else
                null

            if(lastAnnotation!=null){
                pointManager.delete(lastAnnotation)
            }

            // Clear lines and polygons since geometry changed
            lineManager.deleteAll()
            polygonManager.deleteAll()

            // Redraw connections if needed
            when (tappedPoints.size) {
                0, 1 -> areaText.text = "Tap once on the map to mark a point"
                2 -> drawConnectingLine()
                else -> drawPolygon()
            }
        }
    }

    private fun copyAreaToClipboard() {
        val areaTextStr = areaText.text.toString()
        if (areaTextStr.isNotEmpty() && !areaTextStr.contains("Tap once on the map to mark a point")) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Map Area", areaTextStr)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Area copied to clipboard", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No area to copy", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareMapSnapshot(areaKm2: Double) {
        val hectares = areaKm2 * 100
        val kanal = areaKm2 * 247.105
        val marla = areaKm2 * 4942.1
        val sqFeet = areaKm2 * 10_763_910.4

        val myShareString =
            "Kilometers: %.3f km²".format(areaKm2) + "\n" +
                    "Hectares: %.3f ha".format(hectares) + "\n" +
                    "Kanals: %.2f".format(kanal) + "\n" +
                    "Marlas: %.2f".format(marla) + "\n" +
                    "Sq.Feet: %.0f".format(sqFeet)

        mapView.snapshot { bitmap ->
            val cachePath = File(cacheDir, "images").apply { mkdirs() }
            val file = File(cachePath, "map_snapshot.png")

            FileOutputStream(file).use { out ->
                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

            // WhatsApp direct (tries caption)
            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, myShareString)
                putExtra(Intent.EXTRA_TITLE, myShareString)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }

            if (whatsappIntent.resolveActivity(packageManager) != null) {
                startActivity(whatsappIntent)
                return@snapshot
            }

            // Generic chooser (Gmail etc.)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, myShareString)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Map Snapshot"))
        }
    }


    private fun handleTap(point: Point) {
        tappedPoints.add(point)

        // Just to be safe — ensure manager is initialized
        if (!::pointManager.isInitialized) return

        // Add a small upward offset so it looks like the marker drops down
        val dropHeight = 0.0005 // ~50 meters
        val startLat = point.latitude() + dropHeight
        val startPoint = Point.fromLngLat(point.longitude(), startLat)

        // Create annotation
        val annotation = pointManager.create(
            PointAnnotationOptions()
                .withPoint(startPoint)
                .withIconImage("red-pin")
                .withIconSize(1.0)
        )

        pointAnnotations.add(annotation)

        // Animate marker falling down with bounce
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = BounceInterpolator()
            addUpdateListener { anim ->
                val progress = anim.animatedValue as Float
                val currentLat = startLat - (dropHeight * progress)
                annotation.point = Point.fromLngLat(point.longitude(), currentLat)
                pointManager.update(annotation)
            }
        }
        animator.start()

        // Draw polygon or line as before
        if (tappedPoints.size >= 3) {
            drawPolygon()
        } else if (tappedPoints.size == 2) {
            drawConnectingLine()
        }
    }

    private fun drawConnectingLine() {
        lineManager.deleteAll()
        lineManager.create(
            PolylineAnnotationOptions()
                .withPoints(tappedPoints)
                .withLineColor("#FF0000")
                .withLineWidth(2.0)
        )
    }

    private fun drawPolygon() {
        polygonManager.deleteAll()
        lineManager.deleteAll()

        val closed = tappedPoints + tappedPoints.first()
        polygonManager.create(
            PolygonAnnotationOptions()
                .withPoints(listOf(closed))
                .withFillColor("#33FF0000")
                .withFillOpacity(0.4)
        )

        // Draw outline lines
        lineManager.create(
            PolylineAnnotationOptions()
                .withPoints(closed)
                .withLineColor("#FF0000")
                .withLineWidth(2.5)
        )

        calculateAndDisplayArea(closed)
    }

    private fun calculateAndDisplayArea(points: List<Point>) {
        val polygon = Polygon.fromLngLats(listOf(points))
        val areaMeters = TurfMeasurement.area(polygon)
        val areaKm2 = areaMeters / 1_000_000.0

        areaText.text = "Area: %.3f km²".format(areaKm2)
    }

    private fun enableUserLocation() {
        val locationComponent = mapView.location

        locationComponent.updateSettings {
            enabled = true
            pulsingEnabled = true
            locationPuck = LocationPuck2D(
                bearingImage = null,
                shadowImage = null,
                scaleExpression = null
            )
        }

        // Define listener as a variable so we can remove it after first update
        val listener = object : (Point) -> Unit {
            override fun invoke(point: Point) {
                lastKnownLocation = point // 👈 Save latest location
                mapView.getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(14.0)
                        .build()
                )
                // Remove listener after first location fix
                locationComponent.removeOnIndicatorPositionChangedListener(this)
            }
        }

        // Add listener
        locationComponent.addOnIndicatorPositionChangedListener(listener)
    }

    private fun showConvertedAreaDialog(areaKm2: Double) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_converted_area, null)

        val kmText = dialogView.findViewById<TextView>(R.id.kmText)
        val hectareText = dialogView.findViewById<TextView>(R.id.hectareText)
        val kanalText = dialogView.findViewById<TextView>(R.id.kanalText)
        val marlaText = dialogView.findViewById<TextView>(R.id.marlaText)
        val sqFeetText = dialogView.findViewById<TextView>(R.id.sqFeetText)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        // Conversion formulas
        val hectares = areaKm2 * 100
        val kanal = areaKm2 * 247.105   // 1 km² ≈ 247.105 kanals
        val marla = areaKm2 * 4942.1    // 1 km² ≈ 4942.1 marlas
        val sqFeet = areaKm2 * 10_763_910.4  // 1 km² = 10,763,910.4 sq ft

        // Set text
        kmText.text = "Kilometers: %.3f km²".format(areaKm2)
        hectareText.text = "Hectares: %.3f ha".format(hectares)
        kanalText.text = "Kanals: %.2f".format(kanal)
        marlaText.text = "Marlas: %.2f".format(marla)
        sqFeetText.text = "Sq.Feet: %.0f".format(sqFeet)

        // Build dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) // 👈 important

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


}