package com.example.bostonbound

import android.os.Bundle
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val strategy = intent.getStringExtra("strategy") ?: "static_budget"

        // ---- Car Yes/No checkboxes ----
        val cbCarYes = findViewById<CheckBox>(R.id.cbCarYes)
        val cbCarNo = findViewById<CheckBox>(R.id.cbCarNo)

        // Only allow one of Yes / No to be checked at a time
        cbCarYes.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbCarNo.isChecked = false
        }

        cbCarNo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbCarYes.isChecked = false
        }

        // ---- Budget slider (0 – 10,000) ----
        val seekBudget = findViewById<SeekBar>(R.id.seekBudget)
        val tvBudgetValue = findViewById<TextView>(R.id.tvBudgetValue)

        // Make sure the max matches what you set in XML
        seekBudget.max = 10000
        tvBudgetValue.text = "$0"

        seekBudget.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Simple "$1234" formatting
                tvBudgetValue.text = "$" + progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // no-op
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // no-op
            }
        })

        // ---- Distance slider (0 – 100 miles) ----
        val seekDistance = findViewById<SeekBar>(R.id.seekDistance)
        val tvDistanceValue = findViewById<TextView>(R.id.tvDistanceValue)

        seekDistance.max = 100  // 0–100 miles
        tvDistanceValue.text = "0 miles"

        seekDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDistanceValue.text = "$progress miles"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // no-op
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // no-op
            }
        })

        // ---- Back Button ----
        val btnBack = findViewById<Button>(R.id.btnBackHome)
        btnBack.setOnClickListener {
            finish() // closes this screen and returns to HomeActivity
        }

        // ---- Build Itinerary Button ----
        val etDays = findViewById<EditText>(R.id.etDays)

        val cbMuseums = findViewById<CheckBox>(R.id.cbMuseums)
        val cbRestaurants = findViewById<CheckBox>(R.id.cbRestaurants)
        val cbNightlife = findViewById<CheckBox>(R.id.cbNightlife)
        val cbShopping = findViewById<CheckBox>(R.id.cbShopping)
        val cbParks = findViewById<CheckBox>(R.id.cbParks)

        val btnBuild = findViewById<Button>(R.id.btnBuildItinerary)
        btnBuild.setOnClickListener {
            val days = etDays.text.toString().toIntOrNull() ?: 1
            val hasCar = cbCarYes.isChecked      // from your earlier code
            val budget = seekBudget.progress     // 0–10000
            val maxDist = seekDistance.progress  // 0–100

            val prefs = arrayListOf<String>()
            if (cbMuseums.isChecked) prefs.add("museums")
            if (cbRestaurants.isChecked) prefs.add("food")
            if (cbNightlife.isChecked) prefs.add("nightlife")
            if (cbShopping.isChecked) prefs.add("shopping")
            if (cbParks.isChecked) prefs.add("outdoors")

            val intent = Intent(this, ResultsActivity::class.java)
            intent.putExtra("days", days)
            intent.putExtra("has_car", hasCar)
            intent.putExtra("budget_total", budget)
            intent.putExtra("max_distance_miles", maxDist)
            intent.putStringArrayListExtra("prefs_like", prefs)
            intent.putExtra("strategy", strategy)
            startActivity(intent)
        }

    }
}