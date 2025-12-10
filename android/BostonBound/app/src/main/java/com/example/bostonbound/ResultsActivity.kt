package com.example.bostonbound

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ResultsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var adapter: ItineraryAdapter
    private val api = ApiService.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        recycler = findViewById(R.id.recyclerItinerary)
        progress = findViewById(R.id.progressBar)

        adapter = ItineraryAdapter(emptyList())
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // ---- read inputs passed from MainActivity ----
        val days = intent.getIntExtra("days", 1)
        val hasCar = intent.getBooleanExtra("has_car", false)
        val budget = intent.getIntExtra("budget_total", 0)
        val maxDist = intent.getIntExtra("max_distance_miles", 0)
        val prefsLike = intent.getStringArrayListExtra("prefs_like") ?: arrayListOf()
        val strategy = intent.getStringExtra("strategy") ?: "static_budget"

        val mobility = if (hasCar) "car" else "walk"

        val request = PlanRequest(
            city = "Boston",
            date = "2025-12-10",         // simple default
            start_time = "09:00",
            end_time = "19:00",
            budget_total = budget.toDouble(),
            mobility = mobility,
            preferences = PreferencesPayload(like = prefsLike),
            strategy = strategy,
            must_see = emptyList(),
            days = days,
            has_car = hasCar,
            max_distance_miles = maxDist
        )

        progress.visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = api.planTrip(request)
                val stops = response.stops
                runOnUiThread {
                    progress.visibility = View.GONE
                    adapter.updateData(stops)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progress.visibility = View.GONE
                    Toast.makeText(
                        this@ResultsActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}