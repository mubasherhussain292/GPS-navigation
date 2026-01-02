package com.example.myapplication.gpsappworktest.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.gpsappworktest.models.NearbyPlacesItemModel
import com.talymindapps.gps.maps.voice.navigation.driving.directions.R

class NearbyPlacesListAdapter(
    private var nearbyPlacesList: List<NearbyPlacesItemModel>,
    private val onItemClicked: (NearbyPlacesItemModel) -> Unit
) : RecyclerView.Adapter<NearbyPlacesListAdapter.NearbyPlaceViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NearbyPlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_nearby_places, parent, false)
        return NearbyPlaceViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: NearbyPlaceViewHolder,
        position: Int
    ) {
        val nearbyPlace = nearbyPlacesList[position]
        holder.ivNearbyPlaceItem.setImageResource(nearbyPlace.icon)
        holder.tvNearbyPlaceItem.text = nearbyPlace.title

        holder.itemView.setOnClickListener {
            onItemClicked(nearbyPlace)
        }
    }

    override fun getItemCount(): Int = nearbyPlacesList.size

    inner class NearbyPlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivNearbyPlaceItem: ImageView = itemView.findViewById(R.id.ivNearbyPlaceItem)
        val tvNearbyPlaceItem: TextView = itemView.findViewById(R.id.tvNearbyPlaceItem)
    }
}