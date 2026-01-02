package com.example.myapplication.gpsappworktest.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.gpsappworktest.db.Favorite
import com.example.myapplication.gpsappworktest.utilities.setDebouncedClickListener
import com.talymindapps.gps.maps.voice.navigation.driving.directions.R

class FavoriteAdapter(
    private var favoriteList: MutableList<Favorite>,
    private val onDeleteClicked: (Favorite) -> Unit,
    private val onItemClicked: (Favorite) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewAddress: TextView = itemView.findViewById(R.id.textViewAddress)
        val imageViewDelete: ImageView = itemView.findViewById(R.id.imageViewFavouriteSelector)
        var imageViewFavourite: ImageView = itemView.findViewById(R.id.imageViewFavourite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val favorite = favoriteList[position]
        holder.textViewAddress.text = favorite.address

        holder.imageViewFavourite.setImageResource(R.drawable.locationfavouritebg)

        holder.imageViewDelete.visibility = View.VISIBLE

        holder.itemView.setDebouncedClickListener {
            onItemClicked(favorite)
        }

        holder.imageViewDelete.setDebouncedClickListener {
            onDeleteClicked(favorite)
        }
    }

    override fun getItemCount(): Int = favoriteList.size

    fun removeItem(favorite: Favorite) {
        val index = favoriteList.indexOf(favorite)
        if (index != -1) {
            favoriteList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateList(newList: List<Favorite>) {
        favoriteList = newList.toMutableList()
        notifyDataSetChanged()
    }
}

