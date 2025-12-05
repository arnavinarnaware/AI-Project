package com.example.bostonbound

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // This is the button on your home screen
        val makeItineraryButton = findViewById<Button>(R.id.btnMakeItinerary)

        // When clicked, go to the inputs page your partner made (MainActivity)
        makeItineraryButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Optional: hook up Explore Boston too (for now same destination)
        val exploreButton = findViewById<Button>(R.id.btnExploreBoston)
        exploreButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
