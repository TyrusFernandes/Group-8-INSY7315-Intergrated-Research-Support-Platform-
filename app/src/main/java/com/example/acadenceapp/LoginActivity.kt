package com.example.acadenceapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button
import android.widget.TextView

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val btnLogIn: Button = findViewById(R.id.btnLoginSubmit)
        val linkSignUp: TextView = findViewById(R.id.linkSignUp)

        btnLogIn.setOnClickListener {
            // For now just return to Welcome after "login"
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }

        linkSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}