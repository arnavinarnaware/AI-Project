package com.example.bostonbound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoricalAdapter(
    private val data: List<HistoricalRec>,
    private val onClick: (HistoricalRec) -> Unit
) : RecyclerView.Adapter<HistoricalAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvHistName)
        val meta: TextView = v.findViewById(R.id.tvHistMeta)
        val desc: TextView = v.findViewById(R.id.tvHistDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_historical_rec, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = data[position]
        holder.name.text = s.name
        holder.meta.text = "⭐ ${s.rating}  •  ${s.location}"
        holder.desc.text = s.description
        holder.itemView.setOnClickListener { onClick(s) }
    }

    override fun getItemCount(): Int = data.size
}
