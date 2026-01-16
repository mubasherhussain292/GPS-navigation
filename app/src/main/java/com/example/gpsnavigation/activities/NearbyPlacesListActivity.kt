package com.example.gpsnavigation.activities

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gpsnavigation.R
import com.example.gpsnavigation.utils.setDebouncedClickListener
import com.example.myapplication.gpsappworktest.adapters.NearbyPlacesListAdapter
import com.example.myapplication.gpsappworktest.models.NearbyPlacesItemModel

class NearbyPlacesListActivity : AppCompatActivity() {
    private lateinit var imageViewBack: ImageView
    private lateinit var rvNearbyPlaces: RecyclerView
    private lateinit var adapter: NearbyPlacesListAdapter
    private lateinit var nearbyPlacesList: ArrayList<NearbyPlacesItemModel>
    lateinit var container: FrameLayout
    lateinit var nearbyBanner: FrameLayout
    private var interstitialBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nearby_places_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        imageViewBack = findViewById(R.id.imageViewBack)
        container = findViewById(R.id.nativeAdContainer)
        nearbyBanner = findViewById(R.id.nearbyBanner)

        imageViewBack.setDebouncedClickListener {
            finish()
        }

        nearbyPlacesList = kotlin.collections.ArrayList()
        nearbyPlacesList.add(
            NearbyPlacesItemModel(
                R.drawable.gas_station_icon,
                resources.getString(R.string.pterol_pump)
            )
        )
        nearbyPlacesList.add(
            NearbyPlacesItemModel(
                R.drawable.cafe_icon,
                resources.getString(R.string.cafe)
            )
        )
        nearbyPlacesList.add(
            NearbyPlacesItemModel(
                R.drawable.grocery_store_icon,
                resources.getString(R.string.grocery_store)
            )
        )
        nearbyPlacesList.add(
            NearbyPlacesItemModel(
                R.drawable.bus_station_icon,
                resources.getString(R.string.bus_stop)
            )
        )
        nearbyPlacesList.add(
            NearbyPlacesItemModel(
                R.drawable.college_icon,
                resources.getString(R.string.college)
            )
        )
        nearbyPlacesList.add(
            NearbyPlacesItemModel(
                R.drawable.bakery_icon,
                resources.getString(R.string.bakery)
            )
        )
        nearbyPlacesList.add(
            NearbyPlacesItemModel(
                R.drawable.hospital_icon,
                resources.getString(R.string.hospital)
            )
        )



        rvNearbyPlaces = findViewById(R.id.rvNearbyPlaces)
        rvNearbyPlaces.layoutManager = LinearLayoutManager(this)
        adapter = NearbyPlacesListAdapter(nearbyPlacesList) { nearbyPlace ->

            val intent = Intent(applicationContext, DiscoverActivity::class.java)
            intent.putExtra("QUERY_VALUE", nearbyPlace.title)
            startActivity(intent)
        }
        rvNearbyPlaces.adapter = adapter

    }

}
