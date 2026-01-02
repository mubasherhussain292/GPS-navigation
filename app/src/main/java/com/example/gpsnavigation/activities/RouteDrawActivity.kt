package com.example.gpsnavigation.activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.OkHttpResponseAndStringRequestListener
import com.example.gpsnavigation.R
import com.example.gpsnavigation.db.AppDatabase
import com.example.gpsnavigation.db.Favorite
import com.example.gpsnavigation.db.FavoriteDao
import com.example.gpsnavigation.db.Recent
import com.example.gpsnavigation.utils.MyConstants
import com.example.gpsnavigation.utils.setDebouncedClickListener
import com.example.myapplication.gpsappworktest.utilities.Utils.logUserEvent
import com.example.myapplication.gpsappworktest.adapters.MultiRouteAdapter
import com.example.myapplication.gpsappworktest.ads.loadAdaptiveBanner
import com.example.gpsnavigation.mapboxresponse.MapboxResponse
import com.example.myapplication.gpsappworktest.utilities.AdsRemoteConfig
import com.example.myapplication.gpsappworktest.utilities.NewAdsManagerUpdated
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.talymindapps.gps.maps.voice.navigation.driving.directions.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import java.text.DateFormat
import java.util.Date
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.maxBy
import kotlin.collections.minBy
import kotlin.jvm.java
import kotlin.let
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.ranges.coerceIn
import kotlin.ranges.until
import kotlin.text.equals
import kotlin.text.format
import kotlin.text.isNullOrBlank
import kotlin.text.toDouble
import kotlin.to

class RouteDrawActivity : AppCompatActivity() {

    var mapView: MapView? = null
    var routeType: String? = null
    private lateinit var textViewSource: TextView
    private lateinit var textViewDestination: TextView
    var routeDialog: ProgressDialog? = null
    var destlat = "0.0"
    var destlng = "0.0"
    var currentLat = "0.0"
    var currentLang = "0.0"

    var distance = 0.0
    var timeAndDist: String = ""
    lateinit var imageViewMyLocation: ImageView
    lateinit var imageViewCar: ImageView
    lateinit var imageViewBike: ImageView
    lateinit var imageViewWalk: ImageView
    lateinit var timeAndDistanceTv: TextView
    lateinit var textViewDistanceMiles: TextView
    lateinit var textViewTime: TextView
    lateinit var relativeLayoutContinue: RelativeLayout
    lateinit var imageViewBack: ImageView
    lateinit var imageViewShare: ImageView
    private var isRouteDraw = false
    private lateinit var rvMultiRoute: RecyclerView
    private lateinit var adapter: MultiRouteAdapter

    private lateinit var list: MutableList<String>

    private var selectedRouteIndex = 0

    lateinit var imageViewFavourite: ImageView

    private var favoriteItem: Favorite? = null
    private var recentItem: Recent? = null

    private lateinit var dao: FavoriteDao
    private lateinit var db: AppDatabase
    lateinit var container: FrameLayout
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_routedraw)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()
        favoriteItem = intent.getParcelableExtra("favorite_item")
        recentItem = intent.getParcelableExtra("recent_item")

        db = AppDatabase.getDatabase(this)
        dao = db.favoriteDao()
        container = findViewById(R.id.container)

        mapView = findViewById(R.id.mapView)
        imageViewMyLocation = findViewById(R.id.imageViewMyLocation)
        imageViewFavourite = findViewById(R.id.imageViewFavourite)
        imageViewShare = findViewById(R.id.imageViewShare)
        textViewSource = findViewById(R.id.textViewSource)
        textViewDestination = findViewById(R.id.textViewDestination)
        imageViewCar = findViewById(R.id.imageViewCar)
        imageViewBike = findViewById(R.id.imageViewBike)
        imageViewWalk = findViewById(R.id.imageViewWalk)
        imageViewBack = findViewById(R.id.imageViewBack)
        timeAndDistanceTv = findViewById(R.id.time_and_distance_tv)
        textViewDistanceMiles = findViewById(R.id.textViewDistanceMiles)
        textViewTime = findViewById(R.id.textViewTime)
        relativeLayoutContinue = findViewById(R.id.relativeLayoutContinue)
        rvMultiRoute = findViewById(R.id.rvMultiRoute)
        textViewDestination.text = MyConstants.destName ?: "Unknown Destination"

        /*logUserEvent(
            this,
            "findingLocaton",
            mapOf("routes" to "findingLocaton", "screen" to "RouteDrawActivity")
        )*/

        imageViewShare.setDebouncedClickListener {

            try {

                val latitude = MyConstants.destLatLng.latitude
                val longitude = MyConstants.destLatLng.longitude
                val uri = "http://maps.google.com/maps?saddr=$latitude,$longitude"
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                val ShareSub = "Here is my location"
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, ShareSub)
                sharingIntent.putExtra(Intent.EXTRA_TEXT, uri)
                startActivity(Intent.createChooser(sharingIntent, "Share via"))

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        imageViewMyLocation.setOnClickListener {
            moveCameraToLocation(
                MyConstants.currentLatLng.latitude,
                MyConstants.currentLatLng.longitude
            )
        }

        CoroutineScope(Dispatchers.IO).launch {
            val isFav = dao.isFavorite(MyConstants.destName ?: "")
            if (isFav) {
                imageViewFavourite.setImageDrawable(getDrawable(R.drawable.favouritescreenselectorbg))
            } else {
                imageViewFavourite.setImageDrawable(getDrawable(R.drawable.favunselectedbg))
            }
        }


        imageViewFavourite.setDebouncedClickListener {

            Log.d(TAG, "imageViewFavourite Called")

            CoroutineScope(Dispatchers.IO).launch {
                val isFav = dao.isFavorite(MyConstants.destName)

                if (!isFav) {
                    val favorite = Favorite(
                        address = MyConstants.destName,
                        lat = MyConstants.destLatLng.latitude,
                        lon = MyConstants.destLatLng.longitude,
                        dateAndTime = DateFormat.getDateTimeInstance().format(Date()),
                        favoriteCheck = 1
                    )
                    dao.insertFavorite(favorite)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@RouteDrawActivity,
                            "Added to favorites",
                            Toast.LENGTH_SHORT
                        ).show()
                        imageViewFavourite.setImageDrawable(getDrawable(R.drawable.favouritescreenselectorbg))
                    }
                } else {
                    dao.deleteFavoriteByAddress(MyConstants.destName)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@RouteDrawActivity,
                            "Removed from favorites",
                            Toast.LENGTH_SHORT
                        ).show()
                        imageViewFavourite.setImageDrawable(getDrawable(R.drawable.favunselectedbg))
                    }
                }
            }
        }

        imageViewCar.setDebouncedClickListener {
            /*logUserEvent(
                this, "button_click", mapOf(
                    "button_name" to "car_button",
                    "screen" to "RouteDrawActivity"
                )
            )*/
            if (isRouteDraw) {
                // Ignore additional taps while one is in progress
                return@setDebouncedClickListener
            }

            isRouteDraw = true // lock further selections

            routeType = "car"
            navigationAPI("driving")

        }

        imageViewBike.setDebouncedClickListener {
            /*logUserEvent(
                this, "button_click", mapOf(
                    "button_name" to "bike_button",
                    "screen" to "RouteDrawActivity"
                )
            )*/
            if (isRouteDraw) {
                // Ignore additional taps while one is in progress
                return@setDebouncedClickListener
            }

            isRouteDraw = true // lock further selections

            routeType = "bike"
            navigationAPI("cycling")

        }

        imageViewWalk.setDebouncedClickListener {
            /*logUserEvent(
                this, "button_click", mapOf(
                    "button_name" to "walk_button",
                    "screen" to "RouteDrawActivity"
                )
            )*/
            if (isRouteDraw) {
                // Ignore additional taps while one is in progress
                return@setDebouncedClickListener
            }

            isRouteDraw = true // lock further selections
            routeType = "walk"
            navigationAPI("walking")

        }

        relativeLayoutContinue.setDebouncedClickListener {
            startActivity(Intent(this@RouteDrawActivity, NavigationActivity::class.java))

            /*val interstitialId =
                if (BuildConfig.DEBUG) {
                    "ca-app-pub-3940256099942544/1033173712"
                } else {
                    logUserEvent(
                        this, "go_button", mapOf(
                            "button_name" to "continue_button",
                            "screen" to "RouteDrawActivity"
                        )
                    )
                    AdsRemoteConfig.interAddROute_ID
                }


            if (PrefsManager.isPremium(application)) {
                startActivity(Intent(this@RouteDrawActivity, NavigationActivity::class.java))
            } else {
                if (AdsRemoteConfig.interAdRoute) {


                    val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                        progressHelper.barColor = "#A5DC86".toColorInt()
                        setTitleText("Loading  Ad")
                        setContentText("Please Wait")
                        Dialog.setCancelable(false)
                        Dialog.show()
                    }

                    NewAdsManagerUpdated.getInstance().loadAdMobInterstitialAds(
                        this, interstitialId,
                        object : NewAdsManagerUpdated.InterstitalAdListener {
                            override fun isAdLoaded() {
                                Log.d(TAG, "Route interstitial loaded")
                                pDialog.dismiss()
                                // Your existing flow that shows the interstitial, then navigates
                                callNavigationActivityWithAds()
                            }

                            override fun isAdError() {
                                Log.d(TAG, "Route interstitial error")
                                pDialog.dismiss()
                                callNavigationActivityWithAds()
                            }
                        }
                    )
                } else {
                    val intent = Intent(
                        this@RouteDrawActivity,
                        NavigationActivity::class.java
                    )

                    // Assuming routeType is a String? variable defined in RouteDrawActivity
                    intent.putExtra("routetype", routeType)

                    startActivity(intent)
                }
            }*/
        }

        imageViewBack.setDebouncedClickListener {
            /*logUserEvent(
                this, "close_button", mapOf(
                    "button_name" to "close_button",
                    "screen" to "RouteDrawActivity"
                )
            )*/
            CoroutineScope(Dispatchers.IO).launch {
                val isFav = dao.isFavorite(MyConstants.destName)

                withContext(Dispatchers.Main) {
                    if (!isFav) {
                        // Only send back if item was removed
                        val resultIntent = Intent()
                        resultIntent.putExtra(
                            "removed_favorite",
                            favoriteItem
                        ) // the one you received earlier
                        setResult(RESULT_OK, resultIntent)
                    } else {
                        setResult(RESULT_CANCELED)
                    }
                    finish()
                }
            }
        }

        list = mutableListOf()

        rvMultiRoute.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL,
            false
        )
        adapter = MultiRouteAdapter(list) { selectedRoute ->
            val index = list.indexOf(selectedRoute)
            if (index != -1) {
//                drawRoute(MyConstants.mapboxResponse, index)
                selectedRouteIndex = index
                drawAllRoutes(MyConstants.mapboxResponse)
            }

        }
        rvMultiRoute.adapter = adapter

        initMap()

        mapView?.gestures?.addOnMapClickListener { clickedGeoPoint ->

            val mapboxMap = mapView?.getMapboxMap() ?: return@addOnMapClickListener false

            val clickedScreen = mapboxMap.pixelForCoordinate(clickedGeoPoint)

            handleRouteClickScreen(clickedScreen, mapboxMap)

            true
        }

        /*mapView?.gestures?.addOnMapClickListener { point ->
            handleRouteClick(point)
            true
        }*/


        if (AdsRemoteConfig.banneronMapNavigation_control) {
            if (BuildConfig.DEBUG) {
                container.loadAdaptiveBanner(this, "ca-app-pub-3940256099942544/6300978111")
            } else {
                logUserEvent(
                    this@RouteDrawActivity, "routedraw_banner", mapOf(
                        "button_name" to "RouteDrawActivity",
                        "screen" to "RouteDrawActivity"
                    )
                )
                container.loadAdaptiveBanner(this, AdsRemoteConfig.navigationscreenbannerid)
            }
        }

    }

    private fun handleRouteClickScreen(
        clickedScreen: ScreenCoordinate,
        mapboxMap: MapboxMap
    ) {

        logUserEvent(
            this@RouteDrawActivity, "route_change_click", mapOf(
                "button_name" to "RouteDrawActivity",
                "screen" to "RouteDrawActivity"
            )
        )

        val response = MyConstants.mapboxResponse
        if (response == null || response.routes.isEmpty()) return

        val tolerancePx = 25.0

        var closestRouteIndex: Int? = null
        var minDistancePx = Double.MAX_VALUE

        response.routes.forEachIndexed { index, route ->
            val coords = route.geometry?.coordinates ?: return@forEachIndexed

            var prevScreen: ScreenCoordinate? = null

            coords.forEach { coord ->
                val p = Point.fromLngLat(coord[0], coord[1])
                val screen = mapboxMap.pixelForCoordinate(p)

                val prev = prevScreen
                if (prev != null) {
                    val distPx = distancePointToSegmentScreen(
                        clickedScreen,
                        prev,
                        screen
                    )

                    if (distPx < minDistancePx) {
                        minDistancePx = distPx
                        closestRouteIndex = index
                    }
                }
                prevScreen = screen
            }
        }

        if (closestRouteIndex != null && minDistancePx <= tolerancePx) {
            if (closestRouteIndex != selectedRouteIndex) {
                selectedRouteIndex = closestRouteIndex!!
                MyConstants.mapboxResponse?.let { drawAllRoutes(it) }
            }
        } else {
            Log.d("RouteClick", "No route within pixel tolerance")
        }
    }

    private fun distancePointToSegmentScreen(
        p: ScreenCoordinate,
        a: ScreenCoordinate,
        b: ScreenCoordinate
    ): Double {
        val px = p.x
        val py = p.y
        val ax = a.x
        val ay = a.y
        val bx = b.x
        val by = b.y

        val abx = bx - ax
        val aby = by - ay
        val apx = px - ax
        val apy = py - ay

        val ab2 = abx * abx + aby * aby
        val t = if (ab2 == 0.0) {
            0.0
        } else {
            ((apx * abx + apy * aby) / ab2).coerceIn(0.0, 1.0)
        }

        val cx = ax + t * abx
        val cy = ay + t * aby

        val dx = px - cx
        val dy = py - cy

        return sqrt(dx * dx + dy * dy)
    }


    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    Log.d("LOCATION", "Lat: $lat, Lng: $lng")

                    currentLat = lat.toString()
                    currentLang = lng.toString()

                    destlat = lat.toString()
                    destlng = lng.toString()
                    // ✅ Save it globally
                    MyConstants.currentLatLng = LatLng(lat, lng)

                    // Optionally call your navigation API here
                    // navigationAPI("driving") or whatever profile you want
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
            }
    }

    private fun callNavigationActivityWithAds() {

        NewAdsManagerUpdated.getInstance().showInterstitialAd(this) {

            val intent = Intent(
                this@RouteDrawActivity,
                NavigationActivity::class.java
            )

            // Assuming routeType is a String? variable defined in RouteDrawActivity
            intent.putExtra("routetype", routeType)

            startActivity(intent)
        }

    }

    private fun initMap() {
        logUserEvent(
            this@RouteDrawActivity, "map_route_draw", mapOf(
                "no_button" to "no_button",
                "screen" to "RouteDrawActivity"
            )
        )
        try {

            mapView?.getMapboxMap()?.loadStyleUri(MAPBOX_STREETS) { style ->
                addAnnotationToMap()
                showCameratoMap()

                routeType = "car"

                if (MyConstants.sourceName == "Your Location") {
                    initLocationComponent()
                }

                textViewSource.text = MyConstants.sourceName
                if (!isInternetAvailable()) {
                    Toast.makeText(
                        this@RouteDrawActivity,
                        "Please check your internet connection.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    navigationAPI("driving")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun navigationAPI(profile: String) {
        try {
            routeDialog =
                ProgressDialog.show(this@RouteDrawActivity, "", "Calculating Route...", true)
            if (MyConstants.currentLatLng == null) {
                getCurrentLocation()
            } else {
                currentLat = MyConstants.currentLatLng.latitude.toString()
                currentLang = MyConstants.currentLatLng.longitude.toString()

                destlat = MyConstants.destLatLng.latitude.toString()
                destlng = MyConstants.destLatLng.longitude.toString()
            }

            if (routeType.equals("car")) {
                Log.d(TAG, "Route Car Called")
            } else if (routeType.equals("bike")) {
                Log.d(TAG, "Route Bike Called")
            } else if (routeType.equals("walk")) {
                Log.d(TAG, "Route Walk Called")
            } else {
                Log.d(TAG, "Route Invalid Called")
            }

            Log.d(TAG, "Profile is  $profile")

            val accessToken = getString(R.string.mapbox_access_token)

            val url =
                "https://api.mapbox.com/directions/v5/mapbox/$profile/$currentLang%2C$currentLat%3B$destlng%2C$destlat?" +
                        "alternatives=true&banner_instructions=true&geometries=geojson&language=en&overview=full&" +
                        "roundabout_exits=true&steps=true&voice_instructions=true&voice_units=imperial&access_token=$accessToken"

            AndroidNetworking.get(url)
                .setPriority(Priority.HIGH)
                .build()
                .getAsOkHttpResponseAndString(object : OkHttpResponseAndStringRequestListener {
                    override fun onResponse(
                        okHttpResponse: Response?,
                        responseString: String?
                    ) {
                        if (responseString.isNullOrBlank()) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@RouteDrawActivity,
                                    "Empty response from server",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            isRouteDraw = false
                            return
                        }

                        // Parse JSON on IO dispatcher
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val objectMapper = ObjectMapper()
                                val jsonFactory = JsonFactory()
                                val parser = jsonFactory.createParser(responseString)
                                val mapboxResponse =
                                    objectMapper.readValue(parser, MapboxResponse::class.java)

                                // Extract minimal data to validate route existence
                                val hasRoutes = mapboxResponse.routes.isNotEmpty() == true

                                withContext(Dispatchers.Main) {
                                    if (!hasRoutes) {
                                        Toast.makeText(
                                            this@RouteDrawActivity,
                                            "No route available",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        alertMessageNoRoute()
                                    } else {
                                        handleMapboxResponse(mapboxResponse)
                                    }

                                    routeDialog?.let {
                                        if (it.isShowing) {
                                            it.dismiss()
                                        }
                                    }

                                    isRouteDraw = false
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@RouteDrawActivity,
                                        "Failed to parse route data",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    alertMessageNoRoute()
                                    routeDialog?.dismiss()
                                    isRouteDraw = false
                                }
                            }
                        }
                    }

                    override fun onError(anError: ANError?) {
                        Log.e("TAG", "Error: ${anError?.errorDetail}")
                        runOnUiThread {
                            if (routeDialog?.isShowing == true) {
                                routeDialog?.dismiss()
                            }
                            alertMessageNoRoute()
                        }
                        isRouteDraw = false
                    }
                })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun alertMessageNoRoute() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Failed to acquire route, Please try again")
            .setCancelable(false)
            .setPositiveButton(
                "OK"
            ) { dialog, id -> finish() }
        val alert = builder.create()
        alert.show()
    }

    private fun handleMapboxResponse(mapboxResponse: MapboxResponse?) {

        if (mapboxResponse != null && mapboxResponse.routes.isNotEmpty()) {
            MyConstants.mapboxResponse = mapboxResponse

            if (mapboxResponse.routes.size == 0) {
                Log.d(TAG, "No routes found")
                alertMessageNoRoute()

            } else {

                list.clear()

                mapboxResponse.routes.forEachIndexed { index, _ ->
                    list.add("Route ${index + 1}")
                }

                adapter.updateList(list)

                if (routeType == "car") {

                    imageViewCar.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.carselectedbg
                        )
                    )
                    imageViewBike.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.bikeunselectedbg
                        )
                    )
                    imageViewWalk.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.walkunselectedbg
                        )
                    )

                }

                if (routeType == "bike") {

                    imageViewCar.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.carunselectedbg
                        )
                    )
                    imageViewBike.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.bikeselectedbg
                        )
                    )
                    imageViewWalk.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.walkunselectedbg
                        )
                    )
                }

                if (routeType == "walk") {

                    imageViewCar.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.carunselectedbg
                        )
                    )
                    imageViewBike.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.bikeunselectedbg
                        )
                    )
                    imageViewWalk.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this,
                            R.drawable.walkselectedbg
                        )
                    )

                }

//                drawRoute(mapboxResponse,0)

                drawAllRoutes(mapboxResponse)
            }
        }
    }

    private fun drawRoute(mapboxResponse: MapboxResponse?, routeIndex: Int = 0) {
        val seconds = mapboxResponse!!.routes[routeIndex].duration
        Log.d(TAG, "Time Seconds $seconds")

        distance = mapboxResponse.routes[0].distance!!
        distance = distance / 1000

        // Convert distance from kilometers to miles
        val distanceInMiles = distance * 0.621371

        timeAndDist = seconds?.let { calculateTime(it) }!!
        val timeAndDistFinal = timeAndDist + " (" + String.format("%.2f", distance) + " km)"

        val nameAndDistFinal =
            MyConstants.destName + " (" + String.format("%.2f", distance) + " km)"

        val nameAndDistMilesFinal = " (" + String.format("%.2f", distanceInMiles) + " miles)"

        timeAndDistanceTv.text = nameAndDistFinal
        textViewDistanceMiles.text = nameAndDistMilesFinal
        textViewTime.text = timeAndDist

        mapView?.getMapboxMap()?.loadStyleUri(
            MAPBOX_STREETS
        ) {
            val mFeatureList: ArrayList<Point> = kotlin.collections.ArrayList()
            for (i in 0 until mapboxResponse.routes[0].geometry!!.coordinates.size) {
                mFeatureList.add(
                    Point.fromLngLat(
                        (mapboxResponse.routes[0].geometry!!.coordinates[i][0]),
                        mapboxResponse.routes[0].geometry!!.coordinates[i][1]
                    )
                )
            }

            val lineString = LineString.fromLngLats(mFeatureList)
            val feature = Feature.fromGeometry(lineString)
            val featureCollection = FeatureCollection.fromFeatures(listOf(feature))

            val geoJsonSource = GeoJsonSource.Builder("source-id")
                .featureCollection(featureCollection)
                .build()

            it.addSource(geoJsonSource)

            val lineLayer = LineLayer("layer-id", "source-id")
                .lineWidth(8.0)
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
            it.addLayer(lineLayer)

//            val bitmap = (getDrawable(R.drawable.ic_launcher_background) as BitmapDrawable).bitmap
        }
// Create an instance of the Annotation API and get the PointAnnotationManager.
        val annotationApi = mapView?.getPlugin<AnnotationPlugin>(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID)

//        val annotationApi = mapView?.annotations
        val pointAnnotationManager = annotationApi?.createPointAnnotationManager()
// Set options for the resulting symbol layer.
        val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
            // Define a geographic coordinate.
            .withPoint(Point.fromLngLat(destlng.toDouble(), destlat.toDouble()))
            .withIconImage(
                bitmapFromDrawableRes(
                    this@RouteDrawActivity,
                    R.drawable.locationicon
                )!!
            )
// Add the resulting pointAnnotation to the map.
        pointAnnotationManager?.create(pointAnnotationOptions)

        focusOnPoints(
            mapboxResponse,
            LatLng(currentLat.toDouble(), currentLang.toDouble()),
            LatLng(destlat.toDouble(), destlng.toDouble())
        )

        val symbolLayer = SymbolLayer("layer_id", "source_id")
            .iconImage("blue")
            .iconAnchor(IconAnchor.BOTTOM)
    }

    fun focusOnPoints(res: MapboxResponse?, point1: LatLng, point2: LatLng) {

        val originPoint = Point.fromLngLat(currentLang.toDouble(), currentLat.toDouble())
        val destinationPoint = Point.fromLngLat(destlng.toDouble(), destlat.toDouble())

        mapView!!.getMapboxMap().easeTo(
            mapView!!.getMapboxMap().cameraForCoordinateBounds(
                CoordinateBounds(originPoint, destinationPoint, false),
                EdgeInsets(100.0, 100.0, 100.0, 100.0),
                null,
                null
            ),
            mapAnimationOptions {
                duration(5000L)
            }
        )

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
        val locationComponentPlugin = mapView!!.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
//                bearingImage = AppCompatResources.getDrawable(
//                    this@RouteDrawActivity,
//                    R.drawable.mapbox_user_puck_icon_app,
//                ),
//                scaleExpression = interpolate {
//                    linear()
//                    zoom()
//                    stop {
//                        literal(0.0)
//                        literal(0.6)
//                    }
//                    stop {
//                        literal(20.0)
//                        literal(1.0)
//                    }
//                }.toJson()
            )
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        locationComponentPlugin.addOnIndicatorBearingChangedListener(
            onIndicatorBearingChangedListener
        )
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {

    }

    private val onIndicatorPositionChangedListener =
        OnIndicatorPositionChangedListener { location ->
        }

    private fun showCameratoMap() {

        try {

            val cameraPosition = CameraOptions.Builder()
                .center(
                    Point.fromLngLat(
                        MyConstants.destLatLng.longitude,
                        MyConstants.destLatLng.latitude
                    )
                )
                .zoom(16.0)
                .build()

            mapView!!.getMapboxMap().setCamera(cameraPosition)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun addAnnotationToMap() {
        try {

            bitmapFromDrawableRes(
                this@RouteDrawActivity,
                R.drawable.locationiconred
            )?.let {
                val annotationApi =
                    mapView?.getPlugin<AnnotationPlugin>(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID)
//                val annotationApi = mapView?.annotations
                val pointAnnotationManager = annotationApi?.createPointAnnotationManager()
                val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(
                        Point.fromLngLat(
                            MyConstants.destLatLng.longitude,
                            MyConstants.destLatLng.latitude
                        )
                    )
                    .withIconImage(it)
                pointAnnotationManager?.create(pointAnnotationOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) return null
        val maxWidth = 100
        val maxHeight = 120

        return if (sourceDrawable is BitmapDrawable) {
            val originalBitmap = sourceDrawable.bitmap
            // Scale down if larger than max dimensions
            if (originalBitmap.width > maxWidth || originalBitmap.height > maxHeight) {
                originalBitmap.scale(maxWidth, maxHeight, false)
            } else {
                originalBitmap
            }
        } else {
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()

            val width = kotlin.comparisons.minOf(drawable.intrinsicWidth, maxWidth)
            val height = kotlin.comparisons.minOf(drawable.intrinsicHeight, maxHeight)

            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun drawAllRoutes(mapboxResponse: MapboxResponse) {

        logUserEvent(
            this@RouteDrawActivity, "route_draw", mapOf(
                "button_name" to "RouteDrawActivity",
                "screen" to "RouteDrawActivity"
            )
        )

        val mapboxMap = mapView?.getMapboxMap() ?: return

        mapboxMap.getStyle { style ->
            mapboxResponse.routes.forEachIndexed { index, _ ->
                style.removeStyleLayer("route-layer-$index")
                style.removeStyleLayer("route-hit-layer-$index")
                style.removeStyleSource("route-source-$index")
            }

            val allBoundsPoints = mutableListOf<Point>()

            mapboxResponse.routes.forEachIndexed { index, route ->

                val coords = route.geometry!!.coordinates.map {
                    Point.fromLngLat(it[0], it[1])
                }

                allBoundsPoints.addAll(coords)

                val lineString = LineString.fromLngLats(coords)

                val source = GeoJsonSource.Builder("route-source-$index")
                    .geometry(lineString)
                    .build()

                style.addSource(source)

                val routeColor = if (index == selectedRouteIndex)
                    ContextCompat.getColor(this, R.color.colorPrimary)
                else
                    ContextCompat.getColor(this, R.color.blueroutedrawunselected)

                val routeLayer = LineLayer("route-layer-$index", "route-source-$index")
                    .lineJoin(LineJoin.ROUND)
                    .lineCap(LineCap.ROUND)
                    .lineWidth(if (index == selectedRouteIndex) 8.0 else 5.0)
                    .lineColor(routeColor)

                style.addLayer(routeLayer)

                val hitLayer = LineLayer("route-hit-layer-$index", "route-source-$index")
                    .lineWidth(30.0)
                    .lineColor(Color.TRANSPARENT)

                style.addLayerBelow(hitLayer, "route-layer-$index")

                if (index == selectedRouteIndex) {

                    val seconds = route.duration ?: 0.0
                    val distanceKm = (route.distance ?: 0.0) / 1000
                    val distanceMiles = distanceKm * 0.621371

                    val timeStr = calculateTime(seconds)

                    timeAndDistanceTv.text =
                        "${MyConstants.destName} (${String.format("%.2f", distanceKm)} km)"

                    textViewDistanceMiles.text =
                        " (${String.format("%.2f", distanceMiles)} miles)"

                    textViewTime.text = timeStr
                }
            }

            if (allBoundsPoints.isNotEmpty()) {

                val bounds = CameraBoundsOptions.Builder()
                    .bounds(
                        CoordinateBounds(
                            allBoundsPoints.minBy { it.latitude() }.let {
                                Point.fromLngLat(
                                    allBoundsPoints.minBy { it.longitude() }.longitude(),
                                    it.latitude()
                                )
                            },
                            allBoundsPoints.maxBy { it.latitude() }.let {
                                Point.fromLngLat(
                                    allBoundsPoints.maxBy { it.longitude() }.longitude(),
                                    it.latitude()
                                )
                            }
                        )
                    )
                    .build()

                try {
                    val camera = mapboxMap.cameraForCoordinateBounds(
                        bounds.bounds!!,
                        EdgeInsets(160.0, 160.0, 160.0, 160.0)
                    )



                    mapboxMap.easeTo(
                        camera,
                        mapAnimationOptions {
                            duration(1200L)
                        }
                    )

                } catch (e: Exception) {
                    Log.e("MapZoom", "Zoom error: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun handleRouteClick(clicked: Point) {
        val response = MyConstants.mapboxResponse
        if (response == null || response.routes.isEmpty()) {
            return
        }
        val toleranceMeters = 50.0

        var closestRouteIndex: Int? = null
        var minDistance = Double.MAX_VALUE

        response.routes.forEachIndexed { index, route ->
            val coords = route.geometry?.coordinates ?: return@forEachIndexed

            for (i in 0 until coords.size - 1) {
                val p1 = Point.fromLngLat(coords[i][0], coords[i][1])
                val p2 = Point.fromLngLat(coords[i + 1][0], coords[i + 1][1])

                val dist = distancePointToSegment(clicked, p1, p2)
                if (dist < minDistance) {
                    minDistance = dist
                    closestRouteIndex = index
                }
            }
        }

        if (closestRouteIndex != null && minDistance <= toleranceMeters) {
            if (closestRouteIndex != selectedRouteIndex) {
                selectedRouteIndex = closestRouteIndex!!
                drawAllRoutes(MyConstants.mapboxResponse)
            }
        } else {
            Log.d("RouteClick", "No route within tolerance")
        }
    }

    private fun distancePointToSegment(p: Point, v: Point, w: Point): Double {
        // Haversine distance calculation
        fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val R = 6371000.0 // meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return R * c
        }

        val l2 = haversine(v.latitude(), v.longitude(), w.latitude(), w.longitude()).pow(2)
        if (l2 == 0.0) return haversine(p.latitude(), p.longitude(), v.latitude(), v.longitude())

        // Projection factor
        val t = max(
            0.0, min(
                1.0,
                ((p.longitude() - v.longitude()) * (w.longitude() - v.longitude()) +
                        (p.latitude() - v.latitude()) * (w.latitude() - v.latitude())) /
                        ((w.longitude() - v.longitude()).pow(2) + (w.latitude() - v.latitude()).pow(
                            2
                        ))
            )
        )

        val proj = Point.fromLngLat(
            v.longitude() + t * (w.longitude() - v.longitude()),
            v.latitude() + t * (w.latitude() - v.latitude())
        )

        return haversine(p.latitude(), p.longitude(), proj.latitude(), proj.longitude())
    }

    override fun onBackPressed() {
        CoroutineScope(Dispatchers.IO).launch {
            val isFav = dao.isFavorite(MyConstants.destName)

            withContext(Dispatchers.Main) {
                if (!isFav) {
                    // Only send back if item was removed
                    val resultIntent = Intent()
                    resultIntent.putExtra(
                        "removed_favorite",
                        favoriteItem
                    ) // the one you received earlier
                    setResult(RESULT_OK, resultIntent)
                } else {
                    setResult(RESULT_CANCELED)
                }
                finish()
            }
        }
    }

    fun moveCameraToLocation(latitude: Double, longitude: Double) {
        val targetPoint = Point.fromLngLat(longitude, latitude)

        mapView!!.getMapboxMap().flyTo(
            CameraOptions.Builder()
                .center(targetPoint)
                .zoom(15.0)
                .build(),
            mapAnimationOptions {
                duration(2000L)
            }
        )
    }

}

private const val TAG = "RouteDrawActivity"