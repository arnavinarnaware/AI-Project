package com.example.bostonbound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RestaurantsAdapter(
    private val data: List<RestaurantRec>,
    private val onClick: (RestaurantRec) -> Unit
) : RecyclerView.Adapter<RestaurantsAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvRestName)
        val meta: TextView = v.findViewById(R.id.tvRestMeta)
        val desc: TextView = v.findViewById(R.id.tvRestDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_restaurant_rec, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = data[position]
        holder.name.text = r.name
        holder.meta.text = "⭐ ${r.rating}  •  ${r.priceRange}  •  ${r.location}"
        holder.desc.text = r.description

        holder.itemView.setOnClickListener {
            onClick(r)
        }
    }


    override fun getItemCount(): Int = data.size
}
