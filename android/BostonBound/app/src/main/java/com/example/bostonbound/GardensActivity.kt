package com.example.bostonbound

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GardensActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gardens)

        val recycler = findViewById<RecyclerView>(R.id.recyclerGardens)
        recycler.layoutManager = LinearLayoutManager(this)

        val gardens = listOf(
            GardenRec(
                name = "Boston Public Garden",
                rating = 4.8,
                location = "Downtown Boston (next to Boston Common)",
                description = "America’s first public botanical garden (1837), famous for flower beds, lagoons, statues, and Swan Boats."
            ),
            GardenRec(
                name = "Arnold Arboretum of Harvard University",
                rating = 4.8,
                location = "Jamaica Plain / Roslindale",
                description = "A 281-acre living museum of trees with thousands of plant species; great for spring blooms and fall foliage."
            ),
            GardenRec(
                name = "Rose Kennedy Greenway",
                rating = 4.7,
                location = "Downtown Boston / Waterfront",
                description = "Landscaped gardens, fountains, and public art through the heart of the city—modern green space with city views."
            ),
            GardenRec(
                name = "James P. Kelleher Rose Garden",
                rating = 4.7,
                location = "Back Bay Fens, Boston",
                description = "Seasonal rose garden with 1,000+ rose bushes in symmetric beds—perfect for walks and photography."
            ),

            // Honorable mentions
            GardenRec(
                name = "Back Bay Fens",
                rating = 4.7,
                location = "Back Bay / Fenway, Boston",
                description = "Historic parkland and waterways designed by Frederick Law Olmsted (Emerald Necklace)."
            ),
            GardenRec(
                name = "Franklin Park",
                rating = 4.6,
                location = "Roxbury / Dorchester, Boston",
                description = "Largest park in Boston and part of the Emerald Necklace—wide open spaces and trails."
            ),
            GardenRec(
                name = "Christian Science Plaza Reflecting Pool",
                rating = 4.7,
                location = "Back Bay, Boston",
                description = "Peaceful landscaped plaza with fountains and a large reflecting pool—great for a calm break."
            ),
            GardenRec(
                name = "Chestnut Hill Reservoir",
                rating = 4.7,
                location = "Brighton / Chestnut Hill",
                description = "Scenic walking loop with water views—popular for a relaxing stroll."
            )
        )

        recycler.adapter = GardensAdapter(gardens) { g ->
            val mapsIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=${Uri.encode(g.name + ", Boston")}")
            )
            startActivity(mapsIntent)
        }

        findViewById<Button>(R.id.btnBackFromGardens).setOnClickListener { finish() }
    }
}

data class GardenRec(
    val name: String,
    val rating: Double,
    val location: String,
    val description: String
)
