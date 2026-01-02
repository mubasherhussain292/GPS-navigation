package com.example.gpsnavigation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gpsnavigation.R
import com.example.gpsnavigation.db.Recent
import com.example.gpsnavigation.utils.setDebouncedClickListener

class RecentAdapter(
    private var recentList: MutableList<Recent>,
    private val onItemClicked: (Recent) -> Unit
) : RecyclerView.Adapter<RecentAdapter.RecentViewHolder>() {

    inner class RecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewAddress: TextView = itemView.findViewById(R.id.textViewAddress)
        val imageViewDelete: ImageView = itemView.findViewById(R.id.imageViewFavouriteSelector)
        var imageViewFavourite: ImageView = itemView.findViewById(R.id.imageViewFavourite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return RecentViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        val favorite = recentList[position]
        holder.textViewAddress.text = favorite.address

        holder.imageViewFavourite.setImageResource(R.drawable.history_icon)

        holder.imageViewDelete.visibility = View.GONE

        holder.itemView.setDebouncedClickListener {
            onItemClicked(favorite)
        }

    }

    override fun getItemCount(): Int = recentList.size

}

