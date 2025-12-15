package com.example.bostonbound

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val makeItineraryButton = findViewById<Button>(R.id.btnMakeItinerary)
        val exploreButton = findViewById<Button>(R.id.btnExploreBoston)

        makeItineraryButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("strategy", "static_budget")
            startActivity(intent)
        }

        exploreButton.setOnClickListener {
            startActivity(Intent(this, ExploreBostonActivity::class.java))
        }

    }
}
