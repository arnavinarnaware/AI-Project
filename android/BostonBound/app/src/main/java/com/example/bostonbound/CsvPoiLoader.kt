package com.example.bostonbound

import android.content.Context

data class LocalPoi(
    val name: String,
    val category: String,
    val neighborhood: String,
    val priceTier: String,
    val rating: Double,
    val description: String
)

object CsvPoiLoader {
    fun loadPois(context: Context, filename: String = "pois_boston_seed.csv"): List<LocalPoi> {
        val pois = mutableListOf<LocalPoi>()

        context.assets.open(filename).bufferedReader().useLines { lines ->
            val iter = lines.iterator()
            if (!iter.hasNext()) return@useLines
            val header = iter.next() // skip header row

            while (iter.hasNext()) {
                val line = iter.next().trim()
                if (line.isEmpty()) continue

                // NOTE: this assumes your CSV has no commas inside quoted fields.
                val parts = line.split(",")

                // Adjust indexes to match your CSV column order
                val name = parts[0]
                val category = parts[1]
                val neighborhood = parts[2]
                val priceTier = parts[3]
                val rating = parts[4].toDoubleOrNull() ?: 0.0
                val description = parts[5]

                pois.add(LocalPoi(name, category, neighborhood, priceTier, rating, description))
            }
        }

        return pois
    }
}
