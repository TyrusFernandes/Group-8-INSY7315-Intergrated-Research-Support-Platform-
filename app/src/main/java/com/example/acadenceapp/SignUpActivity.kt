package com.example.acadenceapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun attachBaseContext(newBase: Context?) {
        val language = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "English"
        val context = newBase?.let { LocaleHelper.setLocale(it, language) }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        // --- Google sign-in config ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val btnSignUp: Button = findViewById(R.id.btnSignUpSubmit)
        val btnGoogle: Button = findViewById(R.id.btnGoogleSignUp)
        val linkLogIn: TextView = findViewById(R.id.linkLogIn)

        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val etConfirmPassword: EditText = findViewById(R.id.etConfirmPassword)
        val etUsername: EditText = findViewById(R.id.etUsername)

        // === Normal email/password sign-up ===
        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val username = etUsername.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser ?: return@addOnCompleteListener

                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()
                        user.updateProfile(profileUpdates)

                        val role = getSelectedRole()

                        val userData = hashMapOf(
                            "uid" to user.uid,
                            "email" to user.email,
                            "username" to username,
                            "displayName" to username,
                            "role" to role,
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                            .set(userData)

                        Toast.makeText(this, "Sign up successful, please log in", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // === Google button ===
        btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 1001)
        }

        linkLogIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun getSelectedRole(): String {
        return when {
            findViewById<RadioButton>(R.id.radioConsultant).isChecked -> "Consultant"
            findViewById<RadioButton>(R.id.radioStudent).isChecked -> "Student"
            else -> "Student"
        }
    }

    // === Helper: go to dashboard based on role ===
    private fun goToDashboard(role: String) {
        val intent = if (role == "Consultant") {
            Intent(this, ConsultantDashboardActivity::class.java)
        } else {
            Intent(this, DashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    // === Google sign-in result ===
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // This was previously saying "Google Sign-In Success" by mistake
                Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Authentication Failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnCompleteListener
                }

                val user = auth.currentUser ?: return@addOnCompleteListener
                val uid = user.uid
                val usersRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)

                // Check if user doc already exists
                usersRef.get().addOnSuccessListener { snap ->
                    if (snap.exists()) {
                        // Existing user – keep their role (or default to Student)
                        val role = snap.getString("role") ?: "Student"
                        Toast.makeText(this, "Google Sign-In Success", Toast.LENGTH_SHORT).show()
                        goToDashboard(role)
                    } else {
                        // New Google user – ALWAYS Student as requested
                        val usernameInput =
                            findViewById<EditText>(R.id.etUsername).text.toString().trim()
                        val username = when {
                            usernameInput.isNotEmpty() -> usernameInput
                            !user.displayName.isNullOrEmpty() -> user.displayName!!
                            !user.email.isNullOrEmpty() -> user.email!!.substringBefore("@")
                            else -> "StudentUser"
                        }

                        val role = "Student" // <- force Student for Google sign-up

                        val userData = hashMapOf(
                            "uid" to uid,
                            "email" to user.email,
                            "username" to username,
                            "displayName" to username,
                            "role" to role,
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        usersRef.set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Signed in with Google: ${user.email}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                goToDashboard(role)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to save user: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to load user profile: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
