package com.example.bostonbound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class CategoryTile(val title: String, val categoryKey: String, val imageRes: Int)

class CategoryTileAdapter(
    private val tiles: List<CategoryTile>,
    private val onClick: (CategoryTile) -> Unit
) : RecyclerView.Adapter<CategoryTileAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgTile)
        val title: TextView = v.findViewById(R.id.tvTileTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_category_tile, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = tiles[position]
        holder.img.setImageResource(t.imageRes)
        holder.title.text = t.title
        holder.itemView.setOnClickListener { onClick(t) }
    }

    override fun getItemCount(): Int = tiles.size
}
