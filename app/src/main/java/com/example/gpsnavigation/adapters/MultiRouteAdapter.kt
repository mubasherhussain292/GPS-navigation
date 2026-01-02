package com.example.myapplication.gpsappworktest.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.gpsnavigation.R
import com.example.gpsnavigation.utils.setDebouncedClickListener

class MultiRouteAdapter(
    private var multipleRouteList: MutableList<String>,
    private val onItemClicked: (String) -> Unit
): RecyclerView.Adapter<MultiRouteAdapter.MultiRouteViewHolder>() {

    private var selectedPosition = 0 // 👈 Default first item selected

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MultiRouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.multi_route_item, parent, false)
        return MultiRouteViewHolder(view)
    }

    override fun getItemCount(): Int = multipleRouteList.size

    override fun onBindViewHolder(
        holder: MultiRouteViewHolder,
        position: Int
    ) {
        val routeName = multipleRouteList[position]
        holder.textViewAddress.text = routeName

        // 👇 Highlight selected item
        if (position == selectedPosition) {
            holder.textViewAddress.setBackgroundResource(R.drawable.route_item_selected) // e.g., blue rounded bg
            holder.textViewAddress.setTextColor(Color.WHITE)
        } else {
            holder.textViewAddress.setBackgroundResource(R.drawable.route_item_unselected) // e.g., gray bg
            holder.textViewAddress.setTextColor("#2196F3".toColorInt())
        }

        holder.itemView.setDebouncedClickListener {

            val previousPos = selectedPosition
            selectedPosition = holder.adapterPosition

            notifyItemChanged(previousPos) // refresh old selected
            notifyItemChanged(selectedPosition) // refresh new selected

            onItemClicked(routeName)
        }
    }

    inner class MultiRouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewAddress: TextView = itemView.findViewById(R.id.tvRoute)
    }

    fun updateList(list: MutableList<String>){
        this.multipleRouteList = list
        selectedPosition = 0 // 👈 Reset selection to first route
        notifyDataSetChanged()
    }
}