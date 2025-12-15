package com.example.bostonbound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GardensAdapter(
    private val data: List<GardenRec>,
    private val onClick: (GardenRec) -> Unit
) : RecyclerView.Adapter<GardensAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvGardenName)
        val meta: TextView = v.findViewById(R.id.tvGardenMeta)
        val desc: TextView = v.findViewById(R.id.tvGardenDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_garden_rec, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val g = data[position]
        holder.name.text = g.name
        holder.meta.text = "⭐ ${g.rating}  •  ${g.location}"
        holder.desc.text = g.description
        holder.itemView.setOnClickListener { onClick(g) }
    }

    override fun getItemCount(): Int = data.size
}
