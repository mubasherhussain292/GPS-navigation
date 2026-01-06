package com.example.gpsnavigation.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.gpsnavigation.R
import com.example.gpsnavigation.databinding.ActivityRouteMapBinding
import com.example.gpsnavigation.utils.RouteNavExtras
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class RouteMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteMapBinding

    private val okHttp by lazy { OkHttpClient() }

    private var originLat = 0.0
    private var originLng = 0.0
    private var destLat = 0.0
    private var destLng = 0.0

    private var routeSource: GeoJsonSource? = null
    private var origin: Point? = null
    private var destination: Point? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRouteMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.awBtnBack.setOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.awTopBar.updatePadding(top = bars.top)
            insets
        }

        if (!readExtras()) {
            Toast.makeText(this, "Route data missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnGo.setOnClickListener {
            val o = origin
            val d = destination
            if (o == null || d == null) {
                Toast.makeText(this, "Origin/Destination missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }

        binding.mapView.mapboxMap.loadStyle(Style.STANDARD) { style ->
            // Add empty source/layer once, then update after route fetch
            ensureRouteLayer(style)

            // Fetch + draw route
            lifecycleScope.launch {
                val origin = Point.fromLngLat(originLng, originLat)
                val dest = Point.fromLngLat(destLng, destLat)

                val geometry = fetchDirectionsPolyline6(origin, dest)
                if (geometry == null) {
                    Toast.makeText(
                        this@RouteMapActivity,
                        "Failed to fetch route.",
                        Toast.LENGTH_SHORT
                    ).show()
                    // fallback: fit to 2 points
                    fitCamera(listOf(origin, dest))
                    return@launch
                }

                val points: List<Point> = try {
                    PolylineUtils.decode(geometry, 6) // polyline6 :contentReference[oaicite:4]{index=4}
                } catch (e: Exception) {
                    Toast.makeText(
                        this@RouteMapActivity,
                        "Route decode failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    fitCamera(listOf(origin, dest))
                    return@launch
                }

                drawRoute(points)
                fitCamera(points)
            }
        }

        origin = readPoint(
            RouteNavExtras.EXTRA_ORIGIN_LAT,
            RouteNavExtras.EXTRA_ORIGIN_LNG
        )
        destination = readPoint(
            RouteNavExtras.EXTRA_DEST_LAT,
            RouteNavExtras.EXTRA_DEST_LNG
        )



    }
    private fun readPoint(extraLat: String, extraLng: String): Point? {
        val lat = intent.getDoubleExtra(extraLat, Double.NaN)
        val lng = intent.getDoubleExtra(extraLng, Double.NaN)
        if (lat.isNaN() || lng.isNaN()) return null
        return Point.fromLngLat(lng, lat)
    }
    private fun readExtras(): Boolean {
        val i = intent ?: return false
        val hasAll =
            i.hasExtra(EXTRA_ORIGIN_LAT) &&
                    i.hasExtra(EXTRA_ORIGIN_LNG) &&
                    i.hasExtra(EXTRA_DEST_LAT) &&
                    i.hasExtra(EXTRA_DEST_LNG)

        if (!hasAll) return false

        originLat = i.getDoubleExtra(EXTRA_ORIGIN_LAT, 0.0)
        originLng = i.getDoubleExtra(EXTRA_ORIGIN_LNG, 0.0)
        destLat = i.getDoubleExtra(EXTRA_DEST_LAT, 0.0)
        destLng = i.getDoubleExtra(EXTRA_DEST_LNG, 0.0)
        return true
    }

    private fun ensureRouteLayer(style: Style) {
        // Create source once
        if (style.styleSourceExists(ROUTE_SOURCE_ID).not()) {
            routeSource = geoJsonSource(ROUTE_SOURCE_ID) {
                // put a tiny placeholder line initially to avoid "empty" states
                geometry(
                    LineString.fromLngLats(
                        listOf(
                            Point.fromLngLat(originLng, originLat),
                            Point.fromLngLat(destLng, destLat)
                        )
                    )
                )
            }
            style.addSource(routeSource!!)
        }

        // Create layer once
        if (style.styleLayerExists(ROUTE_LAYER_ID).not()) {
            style.addLayer(
                lineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID) {
                    lineColor("#2F80ED")
                    lineWidth(6.0)
                    lineJoin(LineJoin.ROUND)
                    lineCap(LineCap.ROUND)
                    // If you use Standard style and want stable placement, you can add:
                    // slot("middle")
                }
            )
        }
    }

    private fun drawRoute(points: List<Point>) {
        val style = binding.mapView.mapboxMap.getStyle() ?: return
        val src = routeSource ?: run {
            // If not cached, re-create safely (should not happen normally)
            routeSource = geoJsonSource(ROUTE_SOURCE_ID) { }
            style.addSource(routeSource!!)
            routeSource!!
        }

        val line = LineString.fromLngLats(points)
        src.geometry(line) // v11 GeoJsonSource.geometry(...) :contentReference[oaicite:5]{index=5}
    }

    private fun fitCamera(points: List<Point>) {
        // cameraForCoordinates requires map ready; we call after style loaded + draw :contentReference[oaicite:6]{index=6}
        val padding = EdgeInsets(220.0, 80.0, 220.0, 80.0)
        val camera = binding.mapView.mapboxMap.cameraForCoordinates(
            points,
            EdgeInsets(padding.top, padding.left, padding.bottom, padding.right),
            bearing = null,
            pitch = null
        )
        binding.mapView.mapboxMap.setCamera(camera)
    }

    private suspend fun fetchDirectionsPolyline6(origin: Point, dest: Point): String? {
        val token = getString(R.string.mapbox_access_token)

        // Directions API: /directions/v5/mapbox/driving-traffic/{origin};{dest} :contentReference[oaicite:7]{index=7}
        val url =
            "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
                    "${origin.longitude()},${origin.latitude()};${dest.longitude()},${dest.latitude()}" +
                    "?alternatives=false&geometries=polyline6&overview=full&access_token=$token"

        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(url).get().build()
                okHttp.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val body = resp.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    val routes = json.optJSONArray("routes") ?: return@withContext null
                    if (routes.length() == 0) return@withContext null
                    routes.getJSONObject(0).optString("geometry", null)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    // MapView lifecycle forwarding
    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        binding.mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        binding.mapView.onDestroy()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ORIGIN_LAT = "extra_origin_lat"
        private const val EXTRA_ORIGIN_LNG = "extra_origin_lng"
        private const val EXTRA_DEST_LAT = "extra_dest_lat"
        private const val EXTRA_DEST_LNG = "extra_dest_lng"

        private const val ROUTE_SOURCE_ID = "route-source"
        private const val ROUTE_LAYER_ID = "route-layer"

        fun start(
            context: Context,
            originLat: Double,
            originLng: Double,
            destLat: Double,
            destLng: Double
        ) {
            val i = Intent(context, RouteMapActivity::class.java).apply {
                putExtra(EXTRA_ORIGIN_LAT, originLat)
                putExtra(EXTRA_ORIGIN_LNG, originLng)
                putExtra(EXTRA_DEST_LAT, destLat)
                putExtra(EXTRA_DEST_LNG, destLng)
            }
            context.startActivity(i)
        }
    }
}