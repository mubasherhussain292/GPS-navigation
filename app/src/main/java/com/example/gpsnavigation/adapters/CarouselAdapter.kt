package com.example.gpsnavigation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gpsnavigation.R
import com.example.myapplication.gpsappworktest.models.CarouselItem

class CarouselAdapter(private val items: List<CarouselItem>) :
    RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder>() {

    inner class CarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.pageImage)
        val title: TextView = itemView.findViewById(R.id.pageTitle)
        val subtitle: TextView = itemView.findViewById(R.id.pageSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel_page, parent, false)
        return CarouselViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        val item = items[position % items.size]
        holder.image.setImageResource(item.imageRes)
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
    }

    override fun getItemCount(): Int = Int.MAX_VALUE // huge number
}
