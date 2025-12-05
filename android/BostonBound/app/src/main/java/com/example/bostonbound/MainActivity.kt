package com.example.bostonbound

import android.os.Bundle
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import android.content.Intent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        // ---- Build Itinerary ----
        val btnBuild = findViewById<Button>(R.id.btnBuildItinerary)
        btnBuild.setOnClickListener {
            val intent = Intent(this, ResultsActivity::class.java)
            startActivity(intent)
        }

    }
}