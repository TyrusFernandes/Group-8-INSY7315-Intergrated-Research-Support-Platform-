package com.example.acadenceapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var inputFullName: TextInputEditText
    private lateinit var inputUsername: TextInputEditText
    private lateinit var inputEmail: TextInputEditText
    private lateinit var inputPhone: TextInputEditText
    private lateinit var inputField: TextInputEditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Bind views
        inputFullName = findViewById(R.id.inputFullName)
        inputUsername = findViewById(R.id.inputUsername)
        inputEmail = findViewById(R.id.inputEmail)
        inputPhone = findViewById(R.id.inputPhone)
        inputField = findViewById(R.id.inputField)
        btnSave = findViewById(R.id.btnSave)

        // Load current user profile
        loadUserProfile()

        // Save button click
        btnSave.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        // Set FirebaseAuth displayName as username
        inputUsername.setText(user.displayName)

        // Set email
        inputEmail.setText(user.email)

        // Load other fields from Firestore
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    inputFullName.setText(document.getString("fullName") ?: "")
                    inputPhone.setText(document.getString("phone") ?: "")
                    inputField.setText(document.getString("field") ?: "")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        val fullName = inputFullName.text.toString().trim()
        val username = inputUsername.text.toString().trim()
        val phone = inputPhone.text.toString().trim()
        val field = inputField.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Update display name in Firebase Auth (optional)
        if (fullName.isNotEmpty() || username.isNotEmpty()) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(username) // display name is username
                .build()

            user?.updateProfile(profileUpdates)
                ?.addOnSuccessListener {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                }
                ?.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // Update Firestore document
        val userMap = hashMapOf(
            "fullName" to fullName,
            "username" to username,
            "phone" to phone,
            "field" to field
        )

        user?.uid?.let { uid ->
            db.collection("users").document(uid)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile details saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save details: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}