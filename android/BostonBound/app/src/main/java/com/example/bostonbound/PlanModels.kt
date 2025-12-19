package com.example.bostonbound

// matches: "preferences": { "like": [...] }
data class PreferencesPayload(
    val like: List<String> = emptyList()
)

// Request body for POST /plan
data class PlanRequest(
    val city: String = "Boston",
    val date: String,
    val start_time: String,
    val end_time: String,
    val budget_total: Double,
    val mobility: String, // "walk", "mbta", or "rideshare"
    val preferences: PreferencesPayload,
    val strategy: String,
    val must_see: List<String> = emptyList(),
    val days: Int? = null,
    val has_car: Boolean? = null,
    val max_distance_miles: Int? = null,
    val use_live_constraints: Boolean = false
)

// Metrics coming from backend
data class Metrics(
    val planner: String,
    val runtime_ms: Double,
    val total_stops: Int,
    val total_travel_min: Int,
    val total_score: Double,
    val search_effort: Int
)

// Response from POST /plan
data class PlanResponse(
    val itinerary_id: String,
    val stops: List<ItineraryStop>,
    val legs: List<Leg>,
    val cost_summary: CostSummary,
    val metrics: Metrics
)

data class ItineraryStop(
    val poi_id: String,
    val name: String,
    val start: String,
    val end: String,
    val dwell_min: Int,
    val admission_est: Double,
    val day: Int
)

data class Leg(
    val from: String,
    val to: String,
    val mode: String,
    val eta_min: Int,
    val day: Int // backend includes "day" in legs too
)

data class CostSummary(
    val admissions: Double,
    val transport: Double,
    val total: Double
)