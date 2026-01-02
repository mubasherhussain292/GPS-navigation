package com.example.myapplication.gpsappworktest.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gpsnavigation.databinding.ItemDetailRowBinding
import com.example.myapplication.gpsappworktest.models.DetailRow

class DetailRowAdapter : RecyclerView.Adapter<DetailRowAdapter.VH>() {

    private val items = mutableListOf<DetailRow>()

    fun submitList(list: List<DetailRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(val binding: ItemDetailRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        return VH(ItemDetailRowBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.binding.tvTitle.text = row.title
        holder.binding.tvValue.text = row.value
    }
}
