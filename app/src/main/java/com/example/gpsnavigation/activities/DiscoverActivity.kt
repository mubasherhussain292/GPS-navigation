package com.example.gpsnavigation.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.gpsnavigation.R
import com.example.gpsnavigation.db.Recent
import com.example.gpsnavigation.models.NearbyPlacesDetails
import com.example.gpsnavigation.utils.MyConstants
import com.example.gpsnavigation.utils.Utils.toPoint
import com.example.gpsnavigation.utils.setDebouncedClickListener
import com.google.android.gms.maps.model.LatLng
import com.mapbox.android.gestures.Utils.dpToPx
import com.mapbox.common.location.LocationProvider
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.search.common.DistanceCalculator
import com.mapbox.search.discover.Discover
import com.mapbox.search.discover.DiscoverAddress
import com.mapbox.search.discover.DiscoverOptions
import com.mapbox.search.discover.DiscoverQuery
import com.mapbox.search.discover.DiscoverResult
import com.mapbox.search.result.SearchAddress
import com.mapbox.search.result.SearchResultType
import com.mapbox.search.ui.view.place.SearchPlace
import com.mapbox.search.ui.view.place.SearchPlaceBottomSheetView
import kotlinx.coroutines.withTimeoutOrNull
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import kotlin.collections.set

class DiscoverActivity : AppCompatActivity() {
    private lateinit var discover: Discover
    private lateinit var locationProvider: LocationProvider

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapMarkersManager: MapMarkersManager

    private lateinit var tvHeader: TextView

    private lateinit var imageViewBack: ImageView

    private lateinit var searchPlaceView: SearchPlaceBottomSheetView

    private fun defaultDeviceLocationProvider(): LocationProvider =
        LocationServiceFactory.getOrCreate()
            .getDeviceLocationProvider(null)
            .value
            ?: throw kotlin.Exception("Failed to get device location provider")

    private fun Context.showToast(@StringRes resId: Int): Unit =
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show()

    private fun Context.isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_discover)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val queryValue = intent.getStringExtra("QUERY_VALUE")

        imageViewBack = findViewById(R.id.imageViewBack)
        tvHeader = findViewById(R.id.tvHeader)

        tvHeader.text = queryValue

        imageViewBack.setDebouncedClickListener {
            finish()
        }

        discover = Discover.create()
        locationProvider = defaultDeviceLocationProvider()

        mapView = findViewById(R.id.map_view)
        mapMarkersManager = MapMarkersManager(mapView)
        mapView.mapboxMap.also { mapboxMap ->
            this.mapboxMap = mapboxMap


            mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
                mapView.location.updateSettings {
                    enabled = true
                }

                mapView.location.addOnIndicatorPositionChangedListener(object :
                    OnIndicatorPositionChangedListener {
                    override fun onIndicatorPositionChanged(point: Point) {
                        mapView.mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(point)
                                .zoom(14.0)
                                .build()
                        )

                        mapView.location.removeOnIndicatorPositionChangedListener(this)
                    }
                })
            }
        }

        locationProvider.getLastLocation { location ->
            if (location == null) {
                return@getLastLocation
            }

            lifecycleScope.launchWhenStarted {

                if (isInternetSlowOrDisconnected(this@DiscoverActivity)) {
                    showToast(R.string.slow_internet_message)
                    return@launchWhenStarted
                }

                val response = withTimeoutOrNull(10_000L) {
                    discover.search(
                        query = when (queryValue) {
                            resources.getString(R.string.pterol_pump) -> DiscoverQuery.Category.GAS_STATION
                            resources.getString(R.string.cafe) -> DiscoverQuery.Category.COFFEE_SHOP_CAFE
                            resources.getString(R.string.grocery_store) -> DiscoverQuery.Category.SUPERMARKET_GROCERY
                            resources.getString(R.string.bus_stop) -> DiscoverQuery.Category.BUS_STATION
                            resources.getString(R.string.college) -> DiscoverQuery.Category.create("College")
                            resources.getString(R.string.bakery) -> DiscoverQuery.Category.RESTAURANTS
                            resources.getString(R.string.hospital) -> DiscoverQuery.Category.HOSPITAL
                            else -> DiscoverQuery.Category.RESTAURANTS
                        },
                        proximity = location.toPoint(),
                        options = DiscoverOptions(limit = 20)
                    )
                }

                if (response == null) {
                    showToast(R.string.slow_internet_message)
                    return@launchWhenStarted
                }

                response.onValue { results ->
                    mapMarkersManager.showResults(results)
                }.onError { e ->
                    showToast(R.string.slow_internet_message)
                }
            }
        }

        mapMarkersManager.onResultClickListener = { result ->
            mapMarkersManager.adjustMarkersForOpenCard()
            searchPlaceView.open(result.toSearchPlace())
            locationProvider.userDistanceTo(result.coordinate) { distance ->
                distance?.let { searchPlaceView.updateDistance(distance) }
            }
        }

        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    private fun LocationProvider.userDistanceTo(destination: Point, callback: (Double?) -> Unit) {
        getLastLocation { location ->
            if (location == null) {
                callback(null)
            } else {
                val distance = DistanceCalculator.instance(latitude = location.latitude)
                    .distance(location.toPoint(), destination)
                callback(distance)
            }
        }
    }

    private class MapMarkersManager(private val mapView: MapView) {

        private val annotations = mutableMapOf<String, DiscoverResult>()
        private val mapboxMap: MapboxMap = mapView.mapboxMap
        val annotationApi = mapView.getPlugin<AnnotationPlugin>(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID)!!
        val pointAnnotationManager = annotationApi.createPointAnnotationManager()

        var onResultClickListener: ((DiscoverResult) -> Unit)? = null

        init {
            pointAnnotationManager.addClickListener {
                val result = annotations[it.id]
                result?.let { res ->
                    extractNearbyPlaceDetails(
                        res,
                        (mapView.context as DiscoverActivity).locationProvider
                    ) { details ->
                        MyConstants.nearbyPlacesDetails = details

                        if (MyConstants.currentLatLng != null) {
                            val name = MyConstants.nearbyPlacesDetails.getName()
                            MyConstants.destLatLng = LatLng(
                                MyConstants.nearbyPlacesDetails.getLat(),
                                MyConstants.nearbyPlacesDetails.getLon()
                            )
                            MyConstants.destName = name
                            val formattedDate = DateFormat.getDateTimeInstance().format(Date())
                            val recent = Recent(
                                MyConstants.destName,
                                MyConstants.destLatLng.latitude,
                                MyConstants.destLatLng.longitude,
                                formattedDate
                            )


                            val intent = Intent(mapView.context, RouteDrawActivity::class.java)
                            intent.putExtra("recent_item", recent)
                            mapView.context.startActivity(intent)
                        }
                    }
                }
                true
            }


        }


        fun clearMarkers() {
            pointAnnotationManager.deleteAll()
            annotations.clear()
        }

        fun adjustMarkersForOpenCard() {
            val coordinates = annotations.values.map { it.coordinate }
            if (coordinates.isEmpty()) return
            mapboxMap.cameraForCoordinates(
                coordinates, CameraOptions.Builder().zoom(17.5).build(), MARKERS_INSETS, null, null
            ) { cameraOptions ->
                mapboxMap.setCamera(cameraOptions)
            }

        }


        fun showResults(results: List<DiscoverResult>) {
            clearMarkers()
            if (results.isEmpty()) return

            val coordinates = kotlin.collections.ArrayList<Point>(results.size)

            results.forEach { result ->
                val iconBitmap = getCategoryIcon(mapView.context, result)

                val options = PointAnnotationOptions()
                    .withPoint(result.coordinate)
                    .withIconImage(iconBitmap)
                    .withIconAnchor(IconAnchor.BOTTOM)

                val annotation = pointAnnotationManager.create(listOf(options)).first()
                coordinates.add(result.coordinate)
            }

            mapboxMap.cameraForCoordinates(
                coordinates, CameraOptions.Builder().build(),
                MARKERS_INSETS, null, null
            ) { mapboxMap.setCamera(it) }
        }

        // 🏷️ Category → Icon Mapping
        private fun getCategoryIcon(context: Context, result: DiscoverResult): Bitmap {
            val categoryName = result.categories.firstOrNull()?.lowercase().orEmpty()

            fun hasAny(vararg keywords: String): Boolean =
                keywords.any { kw -> categoryName.contains(kw, ignoreCase = true) }

            val iconRes = when {
                // 🏥 Health
                hasAny("hospital", "clinic", "medical", "health", "doctor", "pharmacy") ->
                    R.drawable.ic_hospital

                // ☕ Cafe
                hasAny("cafe", "café") ->
                    R.drawable.ic_cafe

                // 🍞 Bakery / confectionary
                hasAny("bakery", "confectionary") ->
                    R.drawable.ic_bakery

                // 🏦 Bank / ATM
                hasAny("bank", "atm", "finance") ->
                    R.drawable.ic_bank

                // 🍽 Restaurant
                hasAny("restaurant", "eatery", "food") ->
                    R.drawable.ic_restaurant

                // ⛽ Fuel
                hasAny("fuel", "gas", "petrol") ->
                    R.drawable.ic_gas_station

                // 🚌 Transport
                hasAny("bus", "bus stop", "transport", "transit", "station") ->
                    R.drawable.ic_bus_station

                // 🛒 Shopping
                hasAny("grocery", "supermarket", "store", "mart") ->
                    R.drawable.ic_super_market

                // 🎓 Education
                hasAny("college", "school", "university") ->
                    R.drawable.ic_school

                else -> R.drawable.red_marker
            }

            return getBitmapFromVectorDrawable(context, iconRes)
        }


        private fun getBitmapFromVectorDrawable(
            context: Context,
            @DrawableRes drawableId: Int
        ): Bitmap {
            val drawable = AppCompatResources.getDrawable(context, drawableId)
                ?: throw kotlin.IllegalArgumentException("Resource not found: $drawableId")

            val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }

        private fun extractNearbyPlaceDetails(
            result: DiscoverResult,
            locationProvider: LocationProvider,
            callback: (NearbyPlacesDetails) -> Unit
        ) {
            val name = result.name
            val address = result.address
            val city = address.place ?: ""
            val country = address.country ?: ""
            val lat = result.coordinate.latitude()
            val lon = result.coordinate.longitude()
            val placeID = result.id
            val placeType = result.categories.firstOrNull() ?: ""
            val placeAddress = address.formattedAddress ?: ""

            // 🔹 Get user's last known location to calculate distance
            locationProvider.getLastLocation { location ->
                val distance = if (location != null) {
                    DistanceCalculator.instance(location.latitude)
                        .distance(location.toPoint(), result.coordinate)
                } else 0.0

                val details = NearbyPlacesDetails(
                    name,
                    city,
                    country,
                    lat,
                    lon,
                    distance,
                    placeID,
                    placeType,
                    placeAddress
                )

                callback(details)
            }
        }

    }

    private companion object {

        const val PERMISSIONS_REQUEST_LOCATION = 0

        val MARKERS_BOTTOM_OFFSET = dpToPx(176f).toDouble()
        val MARKERS_EDGE_OFFSET = dpToPx(64f).toDouble()
        val PLACE_CARD_HEIGHT = dpToPx(300f).toDouble()

        val MARKERS_INSETS = EdgeInsets(
            MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, MARKERS_BOTTOM_OFFSET, MARKERS_EDGE_OFFSET
        )

        val MARKERS_INSETS_OPEN_CARD = EdgeInsets(
            MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, PLACE_CARD_HEIGHT, MARKERS_EDGE_OFFSET
        )

        fun DiscoverAddress.toSearchAddress(): SearchAddress {
            return SearchAddress(
                houseNumber = houseNumber,
                street = street,
                neighborhood = neighborhood,
                locality = locality,
                postcode = postcode,
                place = place,
                district = district,
                region = region,
                country = country
            )
        }

        fun DiscoverResult.toSearchPlace(): SearchPlace {
            return SearchPlace(
                id = name + UUID.randomUUID().toString(),
                name = name,
                descriptionText = null,
                address = address.toSearchAddress(),
                resultTypes = listOf(SearchResultType.POI),
                record = null,
                coordinate = coordinate,
                routablePoints = routablePoints,
                categories = categories,
                makiIcon = makiIcon,
                metadata = null,
                distanceMeters = null,
                feedback = null,
            )
        }
    }

    private fun isInternetSlowOrDisconnected(context: Context): Boolean {
        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return true
        val capabilities = cm.getNetworkCapabilities(network) ?: return true

        // Check if it's metered or poor quality
        val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

        // Optional: treat mobile data as potentially slow if signal is weak
        if (isCellular) {
            return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }

        // For Wi-Fi or Ethernet, assume OK if available
        return !(isWifi || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }


}