package com.example.acadenceapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseNetworkException
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

        auth = FirebaseAuth.getInstance()

        // UI references
        val btnLogIn: Button = findViewById(R.id.btnLoginSubmit)
        val linkSignUp: TextView = findViewById(R.id.linkSignUp)
        val etUsername: EditText = findViewById(R.id.etUsernameLogin)
        val etPassword: EditText = findViewById(R.id.etPasswordLogin)

        val userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val settingsPrefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)

        btnLogIn.setOnClickListener {
            val email = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val online = NetworkUtils.isOnline(this)
            val currentUser = auth.currentUser

            // 🔹 1) Explicit OFFLINE check – use last session if possible
            if (!online) {
                useOfflineSessionIfPossible(
                    currentUser = currentUser,
                    userPrefs = userPrefs,
                    settingsPrefs = settingsPrefs
                )
                return@setOnClickListener
            }

            // 🔹 2) ONLINE path – normal Firebase login
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

                                // Save for offline reuse
                                userPrefs.edit()
                                    .putString("last_role", role)
                                    .apply()

                                Toast.makeText(this, "Welcome, $role", Toast.LENGTH_SHORT).show()

                                goToDashboardWithBiometrics(
                                    role = role,
                                    settingsPrefs = settingsPrefs
                                )
                            }

                    } else {
                        val ex = task.exception

                        // 🔹 3) If the failure is because of network, fall back to OFFLINE mode
                        if (ex is FirebaseNetworkException) {
                            useOfflineSessionIfPossible(
                                currentUser = auth.currentUser,
                                userPrefs = userPrefs,
                                settingsPrefs = settingsPrefs
                            )
                        } else {
                            Toast.makeText(
                                this,
                                "Login failed: ${ex?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
        }

        linkSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun getDashboardIntentForRole(role: String): Intent {
        return when (role) {
            "Consultant" -> Intent(this, ConsultantDashboardActivity::class.java)
            "Student" -> Intent(this, DashboardActivity::class.java)
            else -> Intent(this, DashboardActivity::class.java)
        }
    }

    private fun goToDashboardWithBiometrics(
        role: String,
        settingsPrefs: android.content.SharedPreferences
    ) {
        val dashboardIntent = getDashboardIntentForRole(role)
        val biometricEnabled = settingsPrefs.getBoolean("biometric_enabled", false)

        if (biometricEnabled) {
            val helper = BiometricHelper(this) {
                startActivity(dashboardIntent)
                finish()
            }
            helper.authenticateOrContinue()
        } else {
            startActivity(dashboardIntent)
            finish()
        }
    }

    /**
     * Tries to log the user in using the existing Firebase session + last saved role.
     * Used when:
     *  - We detect offline before login OR
     *  - Firebase login fails with a network error.
     */
    private fun useOfflineSessionIfPossible(
        currentUser: com.google.firebase.auth.FirebaseUser?,
        userPrefs: android.content.SharedPreferences,
        settingsPrefs: android.content.SharedPreferences
    ) {
        if (currentUser != null) {
            val role = userPrefs.getString("last_role", "Student") ?: "Student"

            Toast.makeText(
                this,
                "Offline: using your last logged-in account ($role)",
                Toast.LENGTH_SHORT
            ).show()

            goToDashboardWithBiometrics(role, settingsPrefs)
        } else {
            Toast.makeText(
                this,
                "No internet and no previous login. Please log in once while online.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
