package com.example.bostonbound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max

class ItineraryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_STOP = 1
        private const val TYPE_SUMMARY = 2
        private const val TYPE_EMPTY = 3
    }

    sealed class RowItem {
        data class Header(val day: Int) : RowItem()
        data class StopRow(val stop: ItineraryStop) : RowItem()
        data class SummaryRow(
            val day: Int,
            val stopCount: Int,
            val travelMin: Int,
            val admissions: Double,
            val transport: Double,
            val total: Double,
        ) : RowItem()
        data class EmptyRow(val message: String) : RowItem()
    }

    private val items: MutableList<RowItem> = mutableListOf()

    class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvDayHeader)
    }

    class StopVH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvPoiName)
        val time: TextView = v.findViewById(R.id.tvPoiTime)
        val cost: TextView = v.findViewById(R.id.tvPoiCost)
    }

    class SummaryVH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvSummaryTitle)
        val stats: TextView = v.findViewById(R.id.tvSummaryStats)
        val breakdown: TextView = v.findViewById(R.id.tvSummaryBreakdown)
    }

    class EmptyVH(v: View) : RecyclerView.ViewHolder(v) {
        val msg: TextView = v.findViewById(R.id.tvEmptyState)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is RowItem.Header -> TYPE_HEADER
            is RowItem.StopRow -> TYPE_STOP
            is RowItem.SummaryRow -> TYPE_SUMMARY
            is RowItem.EmptyRow -> TYPE_EMPTY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(inflater.inflate(R.layout.row_day_header, parent, false))
            TYPE_STOP -> StopVH(inflater.inflate(R.layout.row_itinerary_item, parent, false))
            TYPE_SUMMARY -> SummaryVH(inflater.inflate(R.layout.row_day_summary, parent, false))
            else -> EmptyVH(inflater.inflate(R.layout.row_empty_state, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is RowItem.Header -> {
                val h = holder as HeaderVH
                h.title.text = "Day ${item.day}"
            }
            is RowItem.StopRow -> {
                val h = holder as StopVH
                val stop = item.stop
                h.name.text = stop.name
                h.time.text = "${stop.start} – ${stop.end}"
                h.cost.text = if (stop.admission_est <= 0.0) "Free" else "$" + stop.admission_est.toInt()
            }
            is RowItem.SummaryRow -> {
                val h = holder as SummaryVH
                h.title.text = "Day ${item.day} Summary"
                h.stats.text = "Stops: ${item.stopCount} • Travel: ${item.travelMin} min • Daily total: $${item.total.toInt()}"
                h.breakdown.text =
                    "Admissions: $${item.admissions.toInt()} • Transport: $${item.transport.toInt()}"
            }
            is RowItem.EmptyRow -> {
                val h = holder as EmptyVH
                h.msg.text = item.message
            }
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * Call this from ResultsActivity after you get PlanResponse.
     */
    fun updateFromResponse(response: PlanResponse, budgetTotal: Double, days: Int) {
        items.clear()

        val budgetPerDay = if (days <= 0) budgetTotal else budgetTotal / days

        // group stops + legs by day
        val stopsByDay = response.stops.groupBy { it.day }
        val legsByDay = response.legs.groupBy { it.day }

        for (day in 1..days) {
            items.add(RowItem.Header(day))

            val dayStops = (stopsByDay[day] ?: emptyList()).sortedBy { it.start }
            val dayLegs = legsByDay[day] ?: emptyList()

            if (dayStops.isEmpty()) {
                items.add(
                    RowItem.EmptyRow(
                        "We couldn’t fill Day $day with your constraints. Try increasing travel distance or budget."
                    )
                )
                // still show summary for transparency
                items.add(
                    RowItem.SummaryRow(
                        day = day,
                        stopCount = 0,
                        travelMin = 0,
                        admissions = 0.0,
                        transport = 0.0,
                        total = 0.0,
                    )
                )
                continue
            }

            // stops
            for (s in dayStops) items.add(RowItem.StopRow(s))

            val admissions = dayStops.sumOf { it.admission_est }
            val transport = 0.0 // placeholder until you compute real transit cost
            val travelMin = dayLegs.sumOf { it.eta_min }
            val total = admissions + transport

            // summary at end of each day
            items.add(
                RowItem.SummaryRow(
                    day = day,
                    stopCount = dayStops.size,
                    travelMin = travelMin,
                    admissions = admissions,
                    transport = transport,
                    total = total,
                )
            )
        }

        notifyDataSetChanged()
    }
}