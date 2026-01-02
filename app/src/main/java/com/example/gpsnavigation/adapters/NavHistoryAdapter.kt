package com.example.myapplication.gpsappworktest.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gpsnavigation.R
import com.example.gpsnavigation.db.NavSessionEntity
import java.util.Locale

class NavHistoryAdapter(
    private val onClick: (NavSessionEntity) -> Unit
) : RecyclerView.Adapter<NavHistoryAdapter.VH>() {

    private val items = mutableListOf<NavSessionEntity>()

    fun submitList(newItems: List<NavSessionEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val subtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nav_session, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.title.text = buildString {
            append(item.routeType.uppercase())
            append(" • ")
            append(item.endName ?: "Destination")
        }

        val start = item.startName ?: "Start"
        val time = java.text.SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            .format(java.util.Date(item.startedAtMillis))

        holder.subtitle.text = "$start → ${item.endName ?: "End"} • $time"

        holder.itemView.setOnClickListener { onClick(item) }
    }
}
