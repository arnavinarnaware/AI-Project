package com.example.bostonbound

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri

class ResultsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvLoading: TextView
    private lateinit var tvTitle: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnCompareMetrics: Button

    private lateinit var adapter: ItineraryAdapter
    private val api = ApiService.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        recycler = findViewById(R.id.recyclerItinerary)
        progress = findViewById(R.id.progressBar)
        tvLoading = findViewById(R.id.tvLoading)
        tvTitle = findViewById(R.id.tvResultsTitle)
        btnEdit = findViewById(R.id.btnEditPreferences)
        btnCompareMetrics = findViewById(R.id.btnCompareMetrics)

        // Edit preferences = go back to preferences screen
        btnEdit.setOnClickListener {
            finish() // returns to MainActivity (your preferences screen)
        }

        adapter = ItineraryAdapter { stop ->
            val query = "${stop.name}, Boston, MA"
            val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val web = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
                )
                startActivity(web)
            }
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // ---- read inputs passed from MainActivity/HomeActivity ----
        val days = intent.getIntExtra("days", 1)
        val hasCar = intent.getBooleanExtra("has_car", false)
        val budget = intent.getIntExtra("budget_total", 0)
        val maxDist = intent.getIntExtra("max_distance_miles", 0)
        val prefsLike = intent.getStringArrayListExtra("prefs_like") ?: arrayListOf()
        val useLive = intent.getBooleanExtra("use_live_constraints", false)
        val strategy = intent.getStringExtra("strategy") ?: "static_budget"

        val mobility = if (hasCar) "rideshare" else "walk"

        val baseRequest = PlanRequest(
            city = "Boston",
            date = "2025-12-10",
            start_time = "09:00",
            end_time = "19:00",
            budget_total = budget.toDouble(),
            mobility = mobility,
            preferences = PreferencesPayload(like = prefsLike),
            strategy = "static_budget",      // overridden per-call below
            must_see = emptyList(),
            days = days,
            has_car = hasCar,
            max_distance_miles = maxDist
            // if you added this field to PlanRequest, include it:
            // use_live_constraints = useLive
        )

        // Compare metrics button → separate metrics screen
        btnCompareMetrics.setOnClickListener {
            val metricsIntent = Intent(this, MetricsActivity::class.java)
            metricsIntent.putExtra("days", days)
            metricsIntent.putExtra("has_car", hasCar)
            metricsIntent.putExtra("budget_total", budget)
            metricsIntent.putExtra("max_distance_miles", maxDist)
            metricsIntent.putStringArrayListExtra("prefs_like", prefsLike)
            metricsIntent.putExtra("use_live_constraints", useLive)
            startActivity(metricsIntent)
        }

        // Loading storytelling
        progress.visibility = View.VISIBLE
        tvLoading.visibility = View.VISIBLE
        tvLoading.text = "Designing your itinerary…"

        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Branch behavior based on strategy
                when (strategy) {
                    "ideal" -> runIdealPlanner(baseRequest, days, budget)
                    "astar_budget" -> runSinglePlanner(
                        baseRequest.copy(strategy = "astar_budget"),
                        days,
                        budget,
                        label = "A* (astar_budget)"
                    )
                    else -> runSinglePlanner(
                        baseRequest.copy(strategy = "static_budget"),
                        days,
                        budget,
                        label = "Greedy (static_budget)"
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progress.visibility = View.GONE
                    tvLoading.visibility = View.GONE
                    Toast.makeText(
                        this@ResultsActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * "Ideal" = call both planners, pick the more efficient one (by runtime_ms).
     */
    private suspend fun runIdealPlanner(baseRequest: PlanRequest, days: Int, budget: Int) {
        runOnUiThread {
            tvLoading.text = "Optimizing itinerary… testing planners…"
        }

        val greedyResp = api.planTrip(baseRequest.copy(strategy = "static_budget"))
        val astarResp = api.planTrip(baseRequest.copy(strategy = "astar_budget"))

        val (chosenResp, chosenLabel) =
            if (greedyResp.metrics.runtime_ms <= astarResp.metrics.runtime_ms) {
                greedyResp to "Greedy (static_budget)"
            } else {
                astarResp to "A* (astar_budget)"
            }

        runOnUiThread {
            progress.visibility = View.GONE
            tvLoading.visibility = View.GONE
            tvTitle.text = "Your Boston Itinerary\n(using $chosenLabel)"

            adapter.updateFromResponse(
                response = chosenResp,
                budgetTotal = budget.toDouble(),
                days = days
            )
        }
    }

    /**
     * Run exactly one planner (Greedy OR A*) and show its itinerary.
     */
    private suspend fun runSinglePlanner(
        request: PlanRequest,
        days: Int,
        budget: Int,
        label: String
    ) {
        runOnUiThread {
            tvLoading.text = "Building itinerary with $label…"
        }

        val response = api.planTrip(request)

        runOnUiThread {
            progress.visibility = View.GONE
            tvLoading.visibility = View.GONE
            tvTitle.text = "Your Boston Itinerary\n(using $label)"

            adapter.updateFromResponse(
                response = response,
                budgetTotal = budget.toDouble(),
                days = days
            )
        }
    }
}
