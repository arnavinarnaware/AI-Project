package com.example.bostonbound

import android.content.Context

// Local model for “explore” POIs
data class LocalPoi(
    val id: String,
    val name: String,
    val category: String,
    val priceTier: String,
    val lat: Double,
    val lon: Double,
    val openFrom: String,
    val openTo: String,
    val avgDwellMin: Int,
    val admissionCost: Double,
    // extra fields just for nicer UI
    val neighborhood: String,   // we’ll derive a simple neighborhood label
    val rating: Double,         // fake rating for now
    val description: String     // short description built from CSV info
)

object CsvPoiLoader {

    fun loadPois(
        context: Context,
        filename: String = "pois_boston_seed.csv"
    ): List<LocalPoi> {

        val pois = mutableListOf<LocalPoi>()

        context.assets.open(filename).bufferedReader().useLines { lines ->
            val iter = lines.iterator()
            if (!iter.hasNext()) return@useLines   // empty file

            // skip header: id,name,lat,lon,category,price_tier,open_from,open_to,avg_dwell_min,admission_cost
            iter.next()

            while (iter.hasNext()) {
                val line = iter.next().trim()
                if (line.isEmpty()) continue

                // NOTE: this assumes no commas inside fields.
                val parts = line.split(",")
                if (parts.size < 10) continue   // malformed row, skip

                val id = parts[0]
                val name = parts[1]
                val lat = parts[2].toDoubleOrNull() ?: continue
                val lon = parts[3].toDoubleOrNull() ?: continue
                val category = parts[4]
                val priceTier = parts[5]
                val openFrom = parts[6]
                val openTo = parts[7]
                val avgDwellMin = parts[8].toIntOrNull() ?: 60
                val admissionCost = parts[9].toDoubleOrNull() ?: 0.0

                // Simple “neighborhood” guess based on location or category
                val neighborhood = when (category.lowercase()) {
                    "museums" -> "Fenway / Museum District"
                    "history" -> "Freedom Trail"
                    "outdoors" -> "Parks & Greenways"
                    "food", "seafood" -> "Downtown / Waterfront"
                    else -> "Boston"
                }

                // Fake rating + description to make the details screen nicer
                val rating = 4.5
                val description = buildString {
                    append(name)
                    append(" is a ")
                    append(category.lowercase())
                    append(" spot in Boston. Typical visit ~")
                    append(avgDwellMin)
                    append(" minutes with an estimated admission of $")
                    append(admissionCost.toInt())
                    append(".")
                }

                pois.add(
                    LocalPoi(
                        id = id,
                        name = name,
                        category = category,
                        priceTier = priceTier,
                        lat = lat,
                        lon = lon,
                        openFrom = openFrom,
                        openTo = openTo,
                        avgDwellMin = avgDwellMin,
                        admissionCost = admissionCost,
                        neighborhood = neighborhood,
                        rating = rating,
                        description = description
                    )
                )
            }
        }

        return pois
    }
}