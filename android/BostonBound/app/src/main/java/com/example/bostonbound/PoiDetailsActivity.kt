package com.example.bostonbound

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PoiDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_poi_details)

        findViewById<TextView>(R.id.tvDetailName).text = intent.getStringExtra("name") ?: ""
        findViewById<TextView>(R.id.tvDetailMeta).text =
            "${intent.getStringExtra("category")} • ${intent.getStringExtra("neighborhood")} • ${intent.getStringExtra("priceTier")}"

        findViewById<TextView>(R.id.tvDetailDesc).text = intent.getStringExtra("desc") ?: ""

        findViewById<Button>(R.id.btnBackFromDetails).setOnClickListener { finish() }
    }
}
