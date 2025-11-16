package com.example.acadenceapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val lang = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "English"
        val context = newBase?.let { LocaleHelper.setLocale(it, lang) }
        super.attachBaseContext(context)
    }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // UI references
        val btnLogIn: Button = findViewById(R.id.btnLoginSubmit)
        val linkSignUp: TextView = findViewById(R.id.linkSignUp)
        val etUsername: EditText = findViewById(R.id.etUsernameLogin)
        val etPassword: EditText = findViewById(R.id.etPasswordLogin)

        // Handle login
        btnLogIn.setOnClickListener {
            val email = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase Authentication login
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        if (uid == null) {
                            Toast.makeText(this, "Login error: user ID not found", Toast.LENGTH_LONG).show()
                            return@addOnCompleteListener
                        }

                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val role = snapshot.getString("role") ?: "Student"
                                Toast.makeText(this, "Welcome, $role", Toast.LENGTH_SHORT).show()

                                // Decide which dashboard to open
                                val dashboardIntent = when (role) {
                                    "Consultant" -> Intent(this, ConsultantDashboardActivity::class.java)
                                    "Student" -> Intent(this, DashboardActivity::class.java)
                                    else -> Intent(this, DashboardActivity::class.java)
                                }

                                // 🔹 NEW: check if biometric login is enabled
                                val prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)
                                val biometricEnabled = prefs.getBoolean("biometric_enabled", false)

                                if (biometricEnabled) {
                                    // Ask for biometrics BEFORE going to dashboard
                                    val helper = BiometricHelper(this) {
                                        startActivity(dashboardIntent)
                                        finish()
                                    }
                                    helper.authenticateOrContinue()
                                } else {
                                    // Normal behavior
                                    startActivity(dashboardIntent)
                                    finish()
                                }
                            }
                    } else {
                        Toast.makeText(
                            this,
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Handle "Sign Up" link
        linkSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}
