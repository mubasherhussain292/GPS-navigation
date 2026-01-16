package com.example.gpsnavigation.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.OkHttpResponseAndStringRequestListener
import com.example.gpsnavigation.R
import com.example.gpsnavigation.adapters.RecentAdapter
import com.example.gpsnavigation.db.AppDatabase
import com.example.gpsnavigation.db.Recent
import com.example.gpsnavigation.utils.AdsRemoteConfig
import com.example.gpsnavigation.utils.MyConstants
import com.example.gpsnavigation.utils.Utils.logUserEvent
import com.example.gpsnavigation.utils.setDebouncedClickListener
import com.example.myapplication.gpsappworktest.db.RecentDao
import com.google.android.gms.maps.model.LatLng
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.search.autocomplete.PlaceAutocomplete
import com.mapbox.search.autocomplete.PlaceAutocompleteOptions
import com.mapbox.search.autocomplete.PlaceAutocompleteSuggestion
import com.mapbox.search.autocomplete.PlaceAutocompleteType
import com.mapbox.search.result.SearchAddress
import com.mapbox.search.ui.adapter.autocomplete.PlaceAutocompleteUiAdapter
import com.mapbox.search.ui.view.CommonSearchViewConfiguration
import com.mapbox.search.ui.view.SearchResultsView
import com.mapbox.search.ui.view.place.SearchPlace
import com.mapbox.search.ui.view.place.SearchPlaceBottomSheetView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Response
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.also
import kotlin.apply
import kotlin.collections.first
import kotlin.collections.isNotEmpty
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.toMutableList
import kotlin.coroutines.resume
import kotlin.jvm.java
import kotlin.ranges.rangeTo
import kotlin.text.format
import kotlin.text.isNotEmpty
import kotlin.text.isNullOrBlank
import kotlin.text.lowercase
import kotlin.to

class MapBoxSearchCompleteActivity : AppCompatActivity() {
    private lateinit var placeAutocomplete: PlaceAutocomplete
    private lateinit var searchResultsView: SearchResultsView
    private lateinit var loadingOverlay: ConstraintLayout
    private lateinit var placeAutocompleteUiAdapter: PlaceAutocompleteUiAdapter
    private lateinit var queryEditText: EditText
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapMarkersManager: MapMarkersManager
    private lateinit var searchPlaceView: SearchPlaceBottomSheetView
    private var ignoreNextQueryUpdate = false
    lateinit var imageViewSearch: ImageView

    lateinit var linearLayoutBannerSearchActivity: FrameLayout
    private lateinit var imageViewVoice: ImageView
    private val REQUEST_CODE_SPEECH_INPUT = 100

    private lateinit var recyclerView: RecyclerView
    private lateinit var db: AppDatabase
    private lateinit var dao: RecentDao
    private lateinit var adapter: RecentAdapter
    private var isNavigating = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map_box_search_complete)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val voiceText = intent.getStringExtra("VOICE_TEXT")
        placeAutocomplete = PlaceAutocomplete.create()
        queryEditText = findViewById(R.id.query_text)
        imageViewSearch = findViewById(R.id.imageViewSearch)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        imageViewVoice = findViewById(R.id.imageViewVoice)
        linearLayoutBannerSearchActivity =
            findViewById(R.id.linearLayoutBannerSearchActivity)

        queryEditText.requestFocus()
        queryEditText.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(queryEditText, InputMethodManager.SHOW_IMPLICIT)
        }, 200)

        imageViewSearch.setDebouncedClickListener {
            logUserEvent(this, "close_button", mapOf(
                "button_name" to "close_button",
                "screen" to "MapBoxSearchCompleteActivity"
            )
            )
            finish()
        }

        mapView = findViewById(R.id.map_view)
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

        mapMarkersManager = MapMarkersManager(mapView)

        mapboxMap.addOnMapLongClickListener {
            reverseGeocoding(it)
            return@addOnMapLongClickListener true
        }

        searchResultsView = findViewById(R.id.search_results_view)

        searchResultsView.initialize(
            SearchResultsView.Configuration(
                commonConfiguration = CommonSearchViewConfiguration()
            )
        )

        placeAutocompleteUiAdapter = PlaceAutocompleteUiAdapter(
            view = searchResultsView,
            placeAutocomplete = placeAutocomplete
        )

        recyclerView = findViewById(R.id.recyclerViewFavorites)
        recyclerView.layoutManager = LinearLayoutManager(this)

        db = AppDatabase.getDatabase(this)
        dao = db.recentDao()


        searchPlaceView = findViewById<SearchPlaceBottomSheetView>(R.id.search_place_view).apply {
            initialize(CommonSearchViewConfiguration())

            isFavoriteButtonVisible = false

            addOnCloseClickListener {
                hide()
                closePlaceCard()
            }

            addOnNavigateClickListener { searchPlace ->
                startActivity(geoIntent(searchPlace.coordinate))
            }

            addOnShareClickListener { searchPlace ->
                startActivity(shareIntent(searchPlace))
            }
        }

        if (voiceText != null) {
            if (voiceText.isNotEmpty()) {
                queryEditText.setText(voiceText)
                lifecycleScope.launchWhenStarted {
                    placeAutocompleteUiAdapter.search(voiceText)
                    searchResultsView.isVisible = voiceText.isNotEmpty()
                }
            }
        }

        placeAutocompleteUiAdapter.addSearchListener(object :
            PlaceAutocompleteUiAdapter.SearchListener {

            override fun onSuggestionsShown(suggestions: List<PlaceAutocompleteSuggestion>) {
                // Nothing to do
            }

            /*override fun onSuggestionSelected(suggestion: PlaceAutocompleteSuggestion) {

                if (!isNavigating) {
                    isNavigating = true
                    logUserEvent(this@MapBoxSearchCompleteActivity, "searched_item_button", mapOf(
                        "button_name" to "searched_item_button",
                        "screen" to "MapBoxSearchCompleteActivity"
                    ))
                    val coord = suggestion.coordinate
                    if (coord != null) {
                        val lat = coord.latitude()
                        val lng = coord.longitude()
                        Log.d("MapBoxSearch", "Direct coords → ${suggestion.name} at $lat, $lng")

                        // Example: save in constants
                        MyConstants.destLatLng = LatLng(lat, lng)
                        MyConstants.destName = suggestion.name
                        val recent = Recent(
                            address = MyConstants.destName,
                            lat = MyConstants.destLatLng.latitude,
                            lon = MyConstants.destLatLng.longitude,
                            dateAndTime = DateFormat.getDateTimeInstance().format(Date())
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            val existing = dao.getRecentByAddress(recent.address)
                            if (existing != null) {
                                // Update existing record (keep same id, update timestamp)
                                val updated = existing.copy(dateAndTime = recent.dateAndTime)
                                dao.updateRecent(updated)
                            } else {
                                // Insert new record
                                dao.insertRecent(recent)
                            }
                        }

                        val intent = Intent(
                            this@MapBoxSearchCompleteActivity,
                            RouteDrawActivity::class.java
                        )
                        intent.putExtra("recent_item", recent)
                        startActivity(intent)


                    } else {
                        lifecycleScope.launchWhenStarted {
                            placeAutocomplete.select(suggestion).onValue { result ->

                                Log.d("MapBoxSearch", "Selected suggestion → ${result.coordinate}")
                                MyConstants.destLatLng =
                                    LatLng(
                                        result.coordinate.latitude(),
                                        result.coordinate.longitude()
                                    )
                                MyConstants.destName = suggestion.name

                                val recent = Recent(
                                    address = MyConstants.destName,
                                    lat = MyConstants.destLatLng.latitude,
                                    lon = MyConstants.destLatLng.longitude,
                                    dateAndTime = DateFormat.getDateTimeInstance().format(Date())
                                )
// Save to DB in background
                                CoroutineScope(Dispatchers.IO).launch {
                                    val existing = dao.getRecentByAddress(recent.address)
                                    if (existing != null) {
                                        // Update existing record (keep same id, update timestamp)
                                        val updated = existing.copy(dateAndTime = recent.dateAndTime)
                                        dao.updateRecent(updated)
                                    } else {
                                        // Insert new record
                                        dao.insertRecent(recent)
                                    }
                                }

                                val intent = Intent(
                                    this@MapBoxSearchCompleteActivity,
                                    RouteDrawActivity::class.java
                                )
                                intent.putExtra("recent_item", recent)
                                startActivity(intent)
                            }.onError { error ->
                                Log.d(LOG_TAG, "Suggestion selection error", error)
                                showToast("Auto Complete Error")
                            }
                        }
                    }
                    // Reset after a short delay (optional)
                    Handler(Looper.getMainLooper()).postDelayed({
                        isNavigating = false
                    }, 1000)

                }
//                openPlaceCard(suggestion)
            }*/

            override fun onSuggestionSelected(suggestion: PlaceAutocompleteSuggestion) {
                if (isNavigating) return
                isNavigating = true

                logUserEvent(
                    this@MapBoxSearchCompleteActivity,
                    "searched_item_button",
                    mapOf(
                        "button_name" to "searched_item_button",
                        "screen" to "MapBoxSearchCompleteActivity"
                    )
                )

                val coord = suggestion.coordinate
                if (coord != null) {
                    proceedWithDestination(
                        lat = coord.latitude(),
                        lng = coord.longitude(),
                        name = suggestion.name
                    )
                    return
                }

                lifecycleScope.launchWhenStarted {
                    placeAutocomplete.select(suggestion)
                        .onValue { result ->
                            proceedWithDestination(
                                lat = result.coordinate.latitude(),
                                lng = result.coordinate.longitude(),
                                name = suggestion.name
                            )
                        }
                        .onError { error ->
                            isNavigating = false
                            Log.d(LOG_TAG, "Suggestion selection error", error)
                            showToast("Auto Complete Error")
                        }
                }
            }

            override fun onPopulateQueryClick(suggestion: PlaceAutocompleteSuggestion) {
                queryEditText.setText(suggestion.name)
            }

            override fun onError(e: Exception) {
                // Nothing to do
            }
        })

        queryEditText.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
                if (ignoreNextQueryUpdate) {
                    ignoreNextQueryUpdate = false
                } else {
                    closePlaceCard()
                }

                lifecycleScope.launchWhenStarted {
                    placeAutocompleteUiAdapter.search(text.toString())
                    searchResultsView.isVisible = text.isNotEmpty()
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun afterTextChanged(s: Editable) {
                // Nothing to do
            }
        })

        imageViewVoice.setDebouncedClickListener {
            promptSpeechInput()
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

            queryEditText.requestFocus()
            queryEditText.postDelayed({
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(queryEditText, InputMethodManager.SHOW_IMPLICIT)
            }, 200) // small delay so layout is ready
        }


        /*if (AdsRemoteConfig.searchlocationbannerid_control){
            if (BuildConfig.DEBUG){
                linearLayoutBannerSearchActivity.loadAdaptiveBanner(this,"ca-app-pub-3940256099942544/6300978111")
            }else{
                logUserEvent(this@MapBoxSearchCompleteActivity, "search_screen_banner", mapOf(
                    "button_name" to "MapBoxSearchCompleteActivity",
                    "screen" to "MapBoxSearchCompleteActivity"
                )
                )
                linearLayoutBannerSearchActivity.loadAdaptiveBanner(this,AdsRemoteConfig.searchlocationbannerid)
            }

        }*/


        fetchFavorites()

    }

    private fun proceedWithDestination(lat: Double, lng: Double, name: String) {
        MyConstants.destLatLng = LatLng(lat, lng)
        MyConstants.destName = name

        val recent = Recent(
            address = MyConstants.destName,
            lat = MyConstants.destLatLng.latitude,
            lon = MyConstants.destLatLng.longitude,
            dateAndTime = DateFormat.getDateTimeInstance().format(Date())
        )

        // Save recent in background (same as your code)
        CoroutineScope(Dispatchers.IO).launch {
            val existing = dao.getRecentByAddress(recent.address)
            if (existing != null) {
                dao.updateRecent(existing.copy(dateAndTime = recent.dateAndTime))
            } else {
                dao.insertRecent(recent)
            }
        }

        // BEFORE NAVIGATING: check route
        lifecycleScope.launch {
            try {
                val current = MyConstants.currentLatLng
                if (current == null) {
                    showToast("Current location not available")
                    return@launch
                }

                // Use same routeType/profile logic as RouteDrawActivity
                val profile = getProfileForRouteType("car") // make sure routeType is available here
                // If you don't have routeType in this Activity, just use: val profile = "driving"

                // (Optional) show loader
                showRouteLoading(true)

                val hasRoute = isRouteAvailable(
                    profile = profile,
                    originLat = current.latitude,
                    originLng = current.longitude,
                    destLat = lat,
                    destLng = lng
                )
                if (!hasRoute) {
                    showNoRouteUI()
                    return@launch
                }
                // showLoading(false)

                if (!hasRoute) {
                    showNoRouteUI()
                    return@launch
                }

                // Route exists => now navigate
                val intent = Intent(this@MapBoxSearchCompleteActivity, RouteDrawActivity::class.java)
                intent.putExtra("recent_item", recent)
                intent.putExtra("profile", profile) // optional
                startActivity(intent)

            } finally {
                showRouteLoading(false)
                isNavigating = false
            }
        }
    }
    private fun getProfileForRouteType(routeType: String): String {
        return when (routeType.lowercase()) {
            "car" -> "driving"
            "bike" -> "cycling"
            "walk" -> "walking"
            else -> "driving"
        }
    }

    private fun showNoRouteUI() {
        showToast("No route available")
        // or AlertDialog if you want
        // alertMessageNoRoute() // if you already have a method, call it here
    }

    private fun showRouteLoading(show: Boolean) {
        loadingOverlay.isVisible = show

        // Block touches while loading (prevents double taps / multiple navigations)
        if (show) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }

    private suspend fun isRouteAvailable(
        profile: String,
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): Boolean = suspendCancellableCoroutine { cont ->

        val accessToken = getString(R.string.mapbox_access_token)

        val url =
            "https://api.mapbox.com/directions/v5/mapbox/$profile/" +
                    "$originLng,$originLat;$destLng,$destLat" +
                    "?alternatives=false&geometries=geojson&overview=full&steps=false&access_token=$accessToken"

        AndroidNetworking.get(url)
            .setPriority(Priority.HIGH)
            .build()
            .getAsOkHttpResponseAndString(object : OkHttpResponseAndStringRequestListener {

                override fun onResponse(okHttpResponse: Response?, responseString: String?) {
                    if (cont.isCancelled) return

                    if (responseString.isNullOrBlank()) {
                        cont.resume(false)
                        return
                    }

                    try {
                        val json = JSONObject(responseString)
                        val routes = json.optJSONArray("routes")
                        val hasRoutes = routes != null && routes.length() > 0
                        cont.resume(hasRoutes)
                    } catch (e: Exception) {
                        cont.resume(false)
                    }
                }

                override fun onError(anError: ANError?) {
                    if (cont.isCancelled) return
                    cont.resume(false)
                }
            })
    }

    private fun fetchFavorites() {
        CoroutineScope(Dispatchers.IO).launch {
            val recentList = dao.getAllRecent()

            withContext(Dispatchers.Main) {
                if (recentList.isNotEmpty()) {
                    adapter = RecentAdapter(
                        recentList.toMutableList()
                    ) { clickedRecent ->

                        // Set selected destination in constants (same as before)
                        MyConstants.destLatLng = LatLng(clickedRecent.lat, clickedRecent.lon)
                        MyConstants.destName = clickedRecent.address

                        // Route check + loading + navigate only if route exists
                        openRouteFromRecentItem(clickedRecent)
                    }

                    recyclerView.adapter = adapter
                }
            }
        }
    }


//    private fun deleteFavorite(favorite: Favorite) {
//        CoroutineScope(Dispatchers.IO).launch {
//            dao.deleteFavoriteByAddress(favorite.address)
//
//            withContext(Dispatchers.Main) {
//                adapter.removeItem(favorite)
//            }
//        }
//    }


    private fun openRouteFromRecentItem(selected: Recent) {
        if (isNavigating) return
        isNavigating = true

        val current = MyConstants.currentLatLng
        if (current == null) {
            showToast("Current location not available")
            isNavigating = false
            return
        }

        // If you have routeType in this activity use it, otherwise default to driving
        val profile = try {
            getProfileForRouteType("car") // <-- if routeType exists
        } catch (e: Exception) {
            "driving"
        }

        lifecycleScope.launch {
            try {
                showRouteLoading(true)

                val hasRoute = isRouteAvailable(
                    profile = profile,
                    originLat = current.latitude,
                    originLng = current.longitude,
                    destLat = selected.lat,
                    destLng = selected.lon
                )

                if (!hasRoute) {
                    showNoRouteUI()
                    return@launch
                }

                // Update recent timestamp (avoid duplicate inserts)
                val updatedRecent = selected.copy(
                    dateAndTime = DateFormat.getDateTimeInstance().format(Date())
                )

                CoroutineScope(Dispatchers.IO).launch {
                    val existing = dao.getRecentByAddress(updatedRecent.address)
                    if (existing != null) {
                        dao.updateRecent(existing.copy(dateAndTime = updatedRecent.dateAndTime))
                    } else {
                        dao.insertRecent(updatedRecent)
                    }
                }

                val intent = Intent(this@MapBoxSearchCompleteActivity, RouteDrawActivity::class.java)
                intent.putExtra("recent_item", updatedRecent)
                intent.putExtra("profile", profile) // optional
                startActivity(intent)

            } catch (e: Exception) {
                showToast("Route check failed")
            } finally {
                showRouteLoading(false)
                isNavigating = false
            }
        }
    }


    private fun promptSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (_: Exception) {
            Toast.makeText(this, "Your device does not support speech input", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SPEECH_INPUT -> {
                if (resultCode == RESULT_OK && data != null) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (result != null && result.isNotEmpty()) {
                        queryEditText.setText(result[0])
                    }
                }
            }
        }
    }


    private fun reverseGeocoding(point: Point) {
        val types: List<PlaceAutocompleteType> = when (mapboxMap.cameraState.zoom) {
            in 0.0..4.0 -> REGION_LEVEL_TYPES
            in 4.0..6.0 -> DISTRICT_LEVEL_TYPES
            in 6.0..12.0 -> LOCALITY_LEVEL_TYPES
            else -> ALL_TYPES
        }

        lifecycleScope.launchWhenStarted {
            val response = placeAutocomplete.reverse(point, PlaceAutocompleteOptions(types = types))
            response.onValue { suggestions ->
                if (suggestions.isEmpty()) {
                    showToast("Place AutoComplete Geocoding error")
                } else {
                    openPlaceCard(suggestions.first())
                }
            }.onError { error ->
                Log.d(LOG_TAG, "Reverse geocoding error", error)
                showToast("Place AutoComplete Reverse Geocoding error")
            }
        }
    }

    private fun Context.showToast(msg: String): Unit =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun Context.isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun geoIntent(point: Point): Intent =
        Intent(Intent.ACTION_VIEW, "geo:0,0?q=${point.latitude()}, ${point.longitude()}".toUri())

    private fun shareIntent(searchPlace: SearchPlace): Intent {
        val text = "${searchPlace.name}. " +
                "Address: ${searchPlace.address?.formattedAddress(SearchAddress.FormatStyle.Short) ?: "unknown"}. " +
                "Geo coordinate: (lat=${searchPlace.coordinate.latitude()}, lon=${searchPlace.coordinate.longitude()})"

        return Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    private fun openPlaceCard(suggestion: PlaceAutocompleteSuggestion) {
        ignoreNextQueryUpdate = true
        queryEditText.setText("")

        lifecycleScope.launchWhenStarted {
            placeAutocomplete.select(suggestion).onValue { result ->
                mapMarkersManager.showMarker(result.coordinate)

                // ✅ Get distance in meters
                val distanceMeters = result.distanceMeters

                // ✅ Convert to km if available
                val distanceKm = distanceMeters?.div(1000.0)

                // ✅ Format nicely
                val distanceText = if (distanceKm != null) {
                    String.format(Locale.getDefault(), "%.1f km", distanceKm)
                } else {
                    "N/A"
                }

                searchPlaceView.open(SearchPlace.createFromPlaceAutocompleteResult(result))

                queryEditText.hideKeyboard()
                searchResultsView.isVisible = false
            }.onError { error ->
                Log.d(LOG_TAG, "Suggestion selection error", error)
                showToast("Auto Complete Error")
            }
        }
    }

    private fun closePlaceCard() {
        searchPlaceView.hide()
        mapMarkersManager.clearMarkers()
    }

    private fun View.hideKeyboard() =
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            windowToken,
            0
        )

    private class MapMarkersManager(mapView: MapView) {

        private val mapboxMap = mapView.mapboxMap

        private val annotationPlugin: AnnotationPlugin =
            mapView.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID)!!

        private val circleAnnotationManager: CircleAnnotationManager =
            annotationPlugin.createCircleAnnotationManager()

        private val markers = mutableMapOf<String, Point>()

        fun clearMarkers() {
            markers.clear()
            circleAnnotationManager.deleteAll()
        }

        fun showMarker(coordinate: Point) {
            clearMarkers()

            val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                .withPoint(coordinate)
                .withCircleRadius(8.0)
                .withCircleColor("#ee4e8b")
                .withCircleStrokeWidth(2.0)
                .withCircleStrokeColor("#ffffff")

            val annotation = circleAnnotationManager.create(circleAnnotationOptions)
//            markers[annotation.id] set coordinate

            CameraOptions.Builder()
                .center(coordinate)
                .padding(MARKERS_INSETS_OPEN_CARD)
                .zoom(14.0)
                .build().also {
                    mapboxMap.setCamera(it)
                }
        }
    }

    private companion object {

        const val PERMISSIONS_REQUEST_LOCATION = 0

        const val LOG_TAG = "AutocompleteUiActivity"

        val MARKERS_EDGE_OFFSET = dpToPx(64).toDouble()
        val PLACE_CARD_HEIGHT = dpToPx(300).toDouble()
        val MARKERS_TOP_OFFSET = dpToPx(88).toDouble()

        val MARKERS_INSETS_OPEN_CARD = EdgeInsets(
            MARKERS_TOP_OFFSET, MARKERS_EDGE_OFFSET, PLACE_CARD_HEIGHT, MARKERS_EDGE_OFFSET
        )

        val REGION_LEVEL_TYPES = listOf(
            PlaceAutocompleteType.AdministrativeUnit.Country,
            PlaceAutocompleteType.AdministrativeUnit.Region
        )

        val DISTRICT_LEVEL_TYPES = REGION_LEVEL_TYPES + listOf(
            PlaceAutocompleteType.AdministrativeUnit.Postcode,
            PlaceAutocompleteType.AdministrativeUnit.District
        )

        val LOCALITY_LEVEL_TYPES = DISTRICT_LEVEL_TYPES + listOf(
            PlaceAutocompleteType.AdministrativeUnit.Place,
            PlaceAutocompleteType.AdministrativeUnit.Locality
        )

        private val ALL_TYPES = listOf(
            PlaceAutocompleteType.Poi,
            PlaceAutocompleteType.AdministrativeUnit.Country,
            PlaceAutocompleteType.AdministrativeUnit.Region,
            PlaceAutocompleteType.AdministrativeUnit.Postcode,
            PlaceAutocompleteType.AdministrativeUnit.District,
            PlaceAutocompleteType.AdministrativeUnit.Place,
            PlaceAutocompleteType.AdministrativeUnit.Locality,
            PlaceAutocompleteType.AdministrativeUnit.Neighborhood,
            PlaceAutocompleteType.AdministrativeUnit.Street,
            PlaceAutocompleteType.AdministrativeUnit.Address,
        )

        private fun dpToPx(dp: Int): Int =
            (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
}