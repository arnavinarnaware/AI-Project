package com.example.bostonbound

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MuseumsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_museums)

        val recycler = findViewById<RecyclerView>(R.id.recyclerMuseums)
        recycler.layoutManager = LinearLayoutManager(this)

        val museums = listOf(
            MuseumRec(
                name = "Museum of Fine Arts, Boston",
                rating = 4.8,
                type = "Art museum",
                location = "Fenway, Boston",
                description = "One of the largest and most prestigious art museums in the U.S., featuring a vast collection from ancient to contemporary works across cultures."
            ),
            MuseumRec(
                name = "Museum of Science",
                rating = 4.7,
                type = "Science museum",
                location = "Science Park, Boston",
                description = "Popular museum with hands-on exhibits across physics, biology, engineering, and space. Don’t miss the planetarium and the IMAX at Mugar Omni Theater."
            ),
            MuseumRec(
                name = "Isabella Stewart Gardner Museum",
                rating = 4.7,
                type = "Art museum",
                location = "Fenway, Boston",
                description = "Intimate museum in a Venetian-style palazzo with rotating exhibitions and a famous courtyard garden; also known for the unsolved 1990 art heist."
            ),
            MuseumRec(
                name = "Boston Children's Museum",
                rating = 4.7,
                type = "Children's museum",
                location = "Fort Point, Boston",
                description = "One of the oldest children’s museums in the country with interactive exhibits designed to spark curiosity through play—great for families."
            )
        )

        // Optional: tap a museum -> open in Google Maps
        recycler.adapter = MuseumsAdapter(museums) { m ->
            val mapsIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=${Uri.encode(m.name + ", Boston")}")
            )
            startActivity(mapsIntent)
        }

        findViewById<Button>(R.id.btnBackFromMuseums).setOnClickListener { finish() }
    }
}

data class MuseumRec(
    val name: String,
    val rating: Double,
    val type: String,
    val location: String,
    val description: String
)
