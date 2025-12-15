package com.example.bostonbound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MuseumsAdapter(
    private val data: List<MuseumRec>,
    private val onClick: (MuseumRec) -> Unit
) : RecyclerView.Adapter<MuseumsAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvMuseumName)
        val meta: TextView = v.findViewById(R.id.tvMuseumMeta)
        val desc: TextView = v.findViewById(R.id.tvMuseumDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_museum_rec, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = data[position]
        holder.name.text = m.name
        holder.meta.text = "⭐ ${m.rating}  •  ${m.type}  •  ${m.location}"
        holder.desc.text = m.description
        holder.itemView.setOnClickListener { onClick(m) }
    }

    override fun getItemCount(): Int = data.size
}
