package com.example.acadenceapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to the activity_main.xml layout file.
        setContentView(R.layout.activity_dashboard)
        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        val btnForYou: Button = findViewById(R.id.btnArticle)
        val btnJobs: Button = findViewById(R.id.btnJobs)

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        btnForYou.setOnClickListener {
            val intent = Intent(this, ForYouPageActivity::class.java)
            startActivity(intent)
        }
        btnJobs.setOnClickListener {
            val intent = Intent(this, JobActivity::class.java)
            startActivity(intent)
        }
    }
}