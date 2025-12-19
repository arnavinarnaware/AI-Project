package com.example.bostonbound

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // --- "Make your ideal itinerary" → compare Greedy vs A* and pick best ---
        findViewById<Button?>(R.id.btnMakeItinerary)?.setOnClickListener {
            startItinerary(strategy = "ideal")
        }

        // --- Greedy-only itinerary ---
        findViewById<Button?>(R.id.btnGreedyBudget)?.setOnClickListener {
            startItinerary(strategy = "static_budget")
        }

        // --- A*-only itinerary ---
        findViewById<Button?>(R.id.btnAstarBudget)?.setOnClickListener {
            startItinerary(strategy = "astar_budget")
        }

        // --- Explore Boston (POI browser) ---
        findViewById<Button?>(R.id.btnExploreBoston)?.setOnClickListener {
            startActivity(Intent(this, ExploreBostonActivity::class.java))
        }
    }

    private fun startItinerary(strategy: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("strategy", strategy)
        startActivity(intent)
    }
}