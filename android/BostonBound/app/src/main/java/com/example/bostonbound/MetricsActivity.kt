package com.example.bostonbound

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MetricsActivity : AppCompatActivity() {

    private lateinit var tvGreedyMetrics: TextView
    private lateinit var tvAstarMetrics: TextView
    private lateinit var tvMetricsSummary: TextView
    private lateinit var progress: ProgressBar

    private val api = ApiService.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metrics)

        // Back button
        val btnBack = findViewById<Button>(R.id.btnBackMetrics)
        btnBack.setOnClickListener {
            finish()   // closes MetricsActivity and returns to ResultsActivity
        }

        tvGreedyMetrics = findViewById(R.id.tvGreedyMetrics)
        tvAstarMetrics = findViewById(R.id.tvAstarMetrics)
        tvMetricsSummary = findViewById(R.id.tvMetricsSummary)
        progress = findViewById(R.id.progressMetrics)

        // Read the same parameters used to build the itinerary
        val days = intent.getIntExtra("days", 1)
        val hasCar = intent.getBooleanExtra("has_car", false)
        val budget = intent.getIntExtra("budget_total", 0)
        val maxDist = intent.getIntExtra("max_distance_miles", 0)
        val prefsLike = intent.getStringArrayListExtra("prefs_like") ?: arrayListOf()
        val useLive = intent.getBooleanExtra("use_live_constraints", false)

        val mobility = if (hasCar) "car" else "walk"

        val baseRequest = PlanRequest(
            city = "Boston",
            date = "2025-12-10",
            start_time = "09:00",
            end_time = "19:00",
            budget_total = budget.toDouble(),
            mobility = mobility,
            preferences = PreferencesPayload(like = prefsLike),
            strategy = "static_budget", // will override per call below
            must_see = emptyList(),
            days = days,
            has_car = hasCar,
            max_distance_miles = maxDist
            // if your PlanRequest has use_live_constraints, add it here:
            // use_live_constraints = useLive
        )

        progress.visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Greedy (static_budget)
                val greedyResp = api.planTrip(baseRequest.copy(strategy = "static_budget"))
                // A* (astar_budget)
                val astarResp = api.planTrip(baseRequest.copy(strategy = "astar_budget"))

                val greedyMetrics = greedyResp.metrics
                val astarMetrics = astarResp.metrics

                // Decide which is more efficient (lower runtime)
                val moreEfficientName: String
                val diffMs: Double
                if (greedyMetrics.runtime_ms <= astarMetrics.runtime_ms) {
                    moreEfficientName = "Greedy (static_budget)"
                    diffMs = astarMetrics.runtime_ms - greedyMetrics.runtime_ms
                } else {
                    moreEfficientName = "A* (astar_budget)"
                    diffMs = greedyMetrics.runtime_ms - astarMetrics.runtime_ms
                }

                runOnUiThread {
                    progress.visibility = View.GONE

                    tvGreedyMetrics.text = formatMetrics(
                        title = "Greedy (static_budget)",
                        m = greedyMetrics
                    )
                    tvAstarMetrics.text = formatMetrics(
                        title = "A* (astar_budget)",
                        m = astarMetrics
                    )

                    tvMetricsSummary.text =
                        "More efficient (by runtime): $moreEfficientName\n" +
                        "(Δ ≈ ${"%.2f".format(diffMs)} ms)"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progress.visibility = View.GONE
                    Toast.makeText(
                        this@MetricsActivity,
                        "Error loading metrics: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun formatMetrics(title: String, m: Metrics): String {
        return buildString {
            appendLine(title)
            appendLine("Runtime: ${"%.2f".format(m.runtime_ms)} ms")
            appendLine("Total stops: ${m.total_stops}")
            appendLine("Total travel time: ${m.total_travel_min} min")
            appendLine("Search effort: ${m.search_effort}")
            appendLine("Total score: ${"%.2f".format(m.total_score)}")
        }
    }
}
