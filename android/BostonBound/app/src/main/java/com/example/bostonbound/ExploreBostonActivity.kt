package com.example.bostonbound

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ExploreBostonActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explore_boston)

        val recycler = findViewById<RecyclerView>(R.id.recyclerExploreGrid)
        recycler.layoutManager = GridLayoutManager(this, 2)

        val tiles = listOf(
            CategoryTile("Restaurants", "food", R.drawable.resturant),
            CategoryTile("Museums", "museums", R.drawable.museum),
            CategoryTile("Historical Sites", "history", R.drawable.history),
            CategoryTile("Gardens", "outdoors", R.drawable.garden)
        )

        recycler.adapter = CategoryTileAdapter(tiles) { clicked ->
            when (clicked.categoryKey) {
                "food" -> {
                    // NEW: go to Restaurants recommendations page
                    startActivity(Intent(this, RestaurantsActivity::class.java))
                }
                "museums" -> startActivity(Intent(this, MuseumsActivity::class.java))
                "history" -> startActivity(Intent(this, HistoricalSitesActivity::class.java))
                "outdoors" -> startActivity(Intent(this, GardensActivity::class.java))

                else -> {
                    // Keep your current behavior for the other tiles for now
                    val intent = Intent(this, ResultsActivity::class.java)

                    intent.putStringArrayListExtra("prefs_like", arrayListOf(clicked.categoryKey))
                    intent.putExtra("strategy", "static_explorer")

                    intent.putExtra("days", 1)
                    intent.putExtra("has_car", false)
                    intent.putExtra("budget_total", 150)
                    intent.putExtra("max_distance_miles", 10)

                    startActivity(intent)
                }
            }
        }

        findViewById<Button>(R.id.btnBackHomeFromExplore).setOnClickListener { finish() }
    }
}
