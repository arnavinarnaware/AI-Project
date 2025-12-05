package com.example.bostonbound

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        val recycler = findViewById<RecyclerView>(R.id.recyclerItinerary)
        recycler.layoutManager = LinearLayoutManager(this)

        // temporary fake data so you can see the UI
        val fakeStops = listOf(
            ItineraryStop("USS Constitution Museum", "10:00–11:30"),
            ItineraryStop("Quincy Market", "11:45–13:00"),
            ItineraryStop("Boston Common", "13:30–15:00")
        )

        recycler.adapter = ItineraryAdapter(fakeStops)
    }
}

data class ItineraryStop(
    val name: String,
    val timeRange: String
)
