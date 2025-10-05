package com.example.acadenceapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import android.widget.TextView

class SettingsActivity : AppCompatActivity() {
    //saves the settings details to the device
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings) // replace with your layout

        // Initialize SharedPreferences
        sharedPrefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)
        editor = sharedPrefs.edit()

        // Find your views
        val profileName = findViewById<TextView>(R.id.profile_name)
        val profileEmail = findViewById<TextView>(R.id.profile_email)
        val switchNotifications = findViewById<Switch>(R.id.switch_notifications)
        val switchTheme = findViewById<Switch>(R.id.switch_theme)
        val switchBiometrics = findViewById<Switch>(R.id.switch_biometrics)
        val spinnerLanguage = findViewById<Spinner>(R.id.spinner_language)
        val logoutButton = findViewById<Button>(R.id.btn_logout)
        val editProfileButton =findViewById<Button>(R.id.edit_profile_button)
        val currentUser = FirebaseAuth.getInstance().currentUser

       //displays current user details
        if (currentUser != null) {

            val displayName = currentUser.displayName ?: "User"
            val email = currentUser.email ?: "No email found"

            // Set the text views
            profileName.text = displayName
            profileEmail.text = email
        } else {
            // If no user is signed in
            profileName.text = "Guest"
            profileEmail.text = "Not signed in"
        }

        // --- Load saved preferences ---
        switchNotifications.isChecked = sharedPrefs.getBoolean("notifications_enabled", true)
        switchTheme.isChecked = sharedPrefs.getBoolean("dark_mode_enabled", false)
        switchBiometrics.isChecked = sharedPrefs.getBoolean("biometrics_enabled", true)

        val savedLanguage = sharedPrefs.getString("language_selected", "English")
        val languageAdapter = spinnerLanguage.adapter
        for (i in 0 until languageAdapter.count) {
            if (languageAdapter.getItem(i).toString() == savedLanguage) {
                spinnerLanguage.setSelection(i)
                break
            }
        }

        // --- Set listeners to save preferences when changed ---
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("notifications_enabled", isChecked).apply()
        }

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("dark_mode_enabled", isChecked).apply()
        }

        switchBiometrics.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("biometrics_enabled", isChecked).apply()
        }

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLanguage = parent.getItemAtPosition(position).toString()
                editor.putString("language_selected", selectedLanguage).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // --- Logout button logic ---
        logoutButton.setOnClickListener {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut()

            // Show toast only if notifications enabled
            if (switchNotifications.isChecked) {
                Toast.makeText(this, "Logout successful", Toast.LENGTH_SHORT).show()
            }

            // Navigate back to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        editProfileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        }
    }