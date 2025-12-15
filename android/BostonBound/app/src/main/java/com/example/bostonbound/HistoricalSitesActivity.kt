package com.example.bostonbound

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoricalSitesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historical_sites)

        val recycler = findViewById<RecyclerView>(R.id.recyclerHistorical)
        recycler.layoutManager = LinearLayoutManager(this)

        val sites = listOf(
            HistoricalRec(
                name = "Freedom Trail",
                rating = 4.8,
                location = "Downtown Boston (2.5-mile trail across the city)",
                description = "Connects 16 nationally significant Revolutionary-era sites. Best way to experience Boston’s colonial history on foot."
            ),
            HistoricalRec(
                name = "Boston Common",
                rating = 4.7,
                location = "Downtown Boston",
                description = "Established in 1634, the oldest public park in the U.S. with deep historical significance and scenic surroundings."
            ),
            HistoricalRec(
                name = "Paul Revere House",
                rating = 4.7,
                location = "North End, Boston",
                description = "Built in 1680, former home of patriot Paul Revere and one of Boston’s oldest surviving buildings."
            ),
            HistoricalRec(
                name = "Old North Church",
                rating = 4.7,
                location = "North End, Boston",
                description = "Famous for the “one if by land, two if by sea” signal; oldest standing church building in Boston."
            )
        )

        recycler.adapter = HistoricalAdapter(sites) { s ->
            val mapsIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=${Uri.encode(s.name + ", Boston")}")
            )
            startActivity(mapsIntent)
        }

        findViewById<Button>(R.id.btnBackFromHistorical).setOnClickListener { finish() }
    }
}

data class HistoricalRec(
    val name: String,
    val rating: Double,
    val location: String,
    val description: String
)
