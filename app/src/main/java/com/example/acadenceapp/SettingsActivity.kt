package com.example.acadenceapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.TextView
import android.widget.LinearLayout // Import for list item containers

class SettingsActivity : AppCompatActivity() {
    // Saves the settings details to the device
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun attachBaseContext(newBase: Context?) {
        val language = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "English"
        val context = newBase?.let { LocaleHelper.setLocale(it, language) }
        super.attachBaseContext(context)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val backButton = findViewById<View>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        // Initialize SharedPreferences
        sharedPrefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)
        editor = sharedPrefs.edit()

        // --- Find Views ---
        val profileName = findViewById<TextView>(R.id.profile_name)
        val profileEmail = findViewById<TextView>(R.id.profile_email)

        val spinnerLanguage = findViewById<Spinner>(R.id.spinner_language)
        val logoutButton = findViewById<Button>(R.id.btn_logout)
        val editProfileButton = findViewById<Button>(R.id.edit_profile_button)
        val currentUser = FirebaseAuth.getInstance().currentUser

        // NEW: Find the clickable logout item container to restore functionality
        val logoutItem = findViewById<LinearLayout>(R.id.logout_item)


        // displays current user details
        if (currentUser != null) {
            val displayName = currentUser.displayName ?: "User"
            val email = currentUser.email ?: "No email found"
            profileName.text = displayName
            profileEmail.text = email
        } else {
            profileName.text = "Guest"
            profileEmail.text = "Not signed in"
        }


        val savedLanguage = sharedPrefs.getString("language_selected", "English")
        val languageAdapter = spinnerLanguage.adapter
        for (i in 0 until languageAdapter.count) {
            if (languageAdapter.getItem(i).toString() == savedLanguage) {
                spinnerLanguage.setSelection(i)
                break
            }
        }


        // --- Spinner Logic (Original, simplified as we removed the TextView) ---
        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLanguage = parent.getItemAtPosition(position).toString()
                val currentLang = LocaleHelper.getSavedLanguage(this@SettingsActivity)

                if (selectedLanguage != currentLang) {
                    LocaleHelper.setLocale(this@SettingsActivity, selectedLanguage)

                    // Reload activity to apply language
                    recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        // --- Logout button logic (triggered by LinearLayout click) ---

        // Listener for the clickable LinearLayout item
        logoutItem.setOnClickListener {
            // Trigger the action associated with the hidden Button's ID (btn_logout)
            logoutButton.performClick()
        }

        // Original listener on the hidden button (this performs the actual action)
        logoutButton.setOnClickListener {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut()


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