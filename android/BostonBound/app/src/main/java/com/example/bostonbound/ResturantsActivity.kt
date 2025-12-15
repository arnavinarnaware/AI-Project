package com.example.bostonbound


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView



class RestaurantsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurants)

        val recycler = findViewById<RecyclerView>(R.id.recyclerRestaurants)
        recycler.layoutManager = LinearLayoutManager(this)

        val restaurants = listOf(
            RestaurantRec(
                name = "Ostra",
                priceRange = "$$$",
                location = "Back Bay, Boston",
                rating = 4.5,
                description = "Sleek, modern seafood fine dining known for impeccable dishes and an upscale atmosphere.",
                url = "https://ostraboston.com/"
            ),
            RestaurantRec(
                name = "Mamma Maria",
                priceRange = "$$$",
                location = "North End, Boston",
                rating = 4.5,
                description = "Refined classic Italian in a romantic townhouse setting overlooking the North End.",
                url = "https://www.mammamaria.com/"
            ),
            RestaurantRec(
                name = "Yvonne’s",
                priceRange = "$$–$$$",
                location = "Downtown Crossing, Boston",
                rating = 4.5,
                description = "Stylish New American dining + creative cocktails with a lively, elegant vibe.",
                url = "https://www.yvonnesboston.com/"
            ),
            RestaurantRec(
                name = "Fox & The Knife",
                priceRange = "$$–$$$",
                location = "South Boston",
                rating = 4.5,
                description = "Bold modern Italian with handmade pasta, small plates, and an energetic atmosphere.",
                url = "https://www.foxandtheknife.com/"
            )
        )

        recycler.adapter = RestaurantsAdapter(restaurants) { restaurant ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(restaurant.url)
            }
            startActivity(intent)
        }


        findViewById<Button>(R.id.btnBackFromRestaurants).setOnClickListener { finish() }
    }
}

data class RestaurantRec(
    val name: String,
    val priceRange: String,
    val location: String,
    val rating: Double,
    val description: String,
    val url: String
)

