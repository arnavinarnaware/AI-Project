package com.example.bostonbound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItineraryAdapter(
    private var data: List<ItineraryStop>
) : RecyclerView.Adapter<ItineraryAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvPoiName)
        val time: TextView = v.findViewById(R.id.tvPoiTime)
        val cost: TextView = v.findViewById(R.id.tvPoiCost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_itinerary_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val stop = data[position]
        holder.name.text = stop.name
        holder.time.text = "${stop.start} – ${stop.end}"
        holder.cost.text = "$${stop.admission_est}"
    }

    override fun getItemCount(): Int = data.size

    fun updateData(newData: List<ItineraryStop>) {
        data = newData
        notifyDataSetChanged()
    }
}
