package com.example.acadenceapp

import android.content.Context
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
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import android.widget.ImageView


class ProfileActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val lang = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "English"
        val context = newBase?.let { LocaleHelper.setLocale(it, lang) }
        super.attachBaseContext(context)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var inputFullName: TextInputEditText
    private lateinit var inputUsername: TextInputEditText
    private lateinit var inputEmail: TextInputEditText
    private lateinit var inputPhone: TextInputEditText
    private lateinit var inputField: TextInputEditText
    private lateinit var btnSave: Button

    private val PICK_IMAGE_REQUEST = 1
    private lateinit var profileImageView: ImageView
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        inputFullName = findViewById(R.id.inputFullName)
        inputUsername = findViewById(R.id.inputUsername)
        inputEmail = findViewById(R.id.inputEmail)
        inputPhone = findViewById(R.id.inputPhone)
        inputField = findViewById(R.id.inputField)
        btnSave = findViewById(R.id.btnSave)
        profileImageView = findViewById(R.id.profileImage)

        loadUserProfile()

        btnSave.setOnClickListener {
            saveUserProfile()
        }

        // ✅ Click to pick image
        profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    // ✅ Receive image and upload
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            profileImageView.setImageURI(imageUri) // show image
            uploadImageToFirebase() // upload to Firebase
        }
    }

    private fun uploadImageToFirebase() {
        val user = auth.currentUser ?: return
        val storageRef = FirebaseStorage.getInstance().reference
            .child("documents/${user.uid}.jpg") // 👈 Uploads to /documents/

        imageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setPhotoUri(downloadUri)
                            .build()

                        user.updateProfile(profileUpdates)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // ✅ Load image and fields
    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        inputUsername.setText(user.displayName)
        inputEmail.setText(user.email)

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    inputFullName.setText(document.getString("fullName") ?: "")
                    inputPhone.setText(document.getString("phone") ?: "")
                    inputField.setText(document.getString("field") ?: "")
                }
            }

        val profilePicRef = FirebaseStorage.getInstance().reference.child("documents/$userId.jpg")
        profilePicRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(profileImageView)
        }
    }

    // ✅ Save text profile
    private fun saveUserProfile() {
        val user = auth.currentUser
        val fullName = inputFullName.text.toString().trim()
        val username = inputUsername.text.toString().trim()
        val phone = inputPhone.text.toString().trim()
        val field = inputField.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build()

        user?.updateProfile(profileUpdates)

        val userMap = hashMapOf(
            "fullName" to fullName,
            "username" to username,
            "phone" to phone,
            "field" to field
        )

        user?.uid?.let { uid ->
            db.collection("users").document(uid)
                .set(userMap, SetOptions.merge())
        }
    }
}