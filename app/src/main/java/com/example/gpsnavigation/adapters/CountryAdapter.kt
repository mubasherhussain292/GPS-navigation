package com.example.gpsnavigation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.gpsnavigation.R
import com.example.myapplication.gpsappworktest.models.Country

class CountryAdapter(
    private var countries: List<Country>,
    private val onItemClick: (Country) -> Unit
) :
    RecyclerView.Adapter<CountryAdapter.CountryViewHolder>() {

    inner class CountryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        val countryName: TextView = itemView.findViewById(R.id.countryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_country, parent, false)
        return CountryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        val country = countries[position]
        holder.countryName.text = country.name.replace(".jpg", "")

        Glide.with(holder.itemView.context)
            .load(country.thumbnail_url)
            .transform(CenterCrop(), RoundedCorners(16)) // 16px corner radius
            .placeholder(R.drawable.remove) // shown while loading
            .into(holder.thumbnail)

        // Handle click
        holder.itemView.setOnClickListener {
            onItemClick(country)
        }
    }

    override fun getItemCount() = countries.size

    fun updateData(newCountries: List<Country>) {
        countries = newCountries
        notifyDataSetChanged()
    }
}
