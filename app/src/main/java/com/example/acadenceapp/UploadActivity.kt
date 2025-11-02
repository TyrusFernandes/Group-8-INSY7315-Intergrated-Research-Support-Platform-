package com.example.acadenceapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class UploadActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val lang = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "English"
        val context = newBase?.let { LocaleHelper.setLocale(it, lang) }
        super.attachBaseContext(context)
    }

    private lateinit var fileTitle: EditText
    private lateinit var tagsInput: EditText
    private lateinit var pickFileButton: Button
    private lateinit var uploadButton: Button
    private lateinit var filePathText: TextView
    private lateinit var progressBar: ProgressBar

    private var fileUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        fileTitle = findViewById(R.id.fileTitle)
        tagsInput = findViewById(R.id.tagsInput)
        pickFileButton = findViewById(R.id.pickFileButton)
        uploadButton = findViewById(R.id.uploadButton)
        filePathText = findViewById(R.id.filePathText)
        progressBar = findViewById(R.id.progressBar)

        pickFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*" // keep generic; you can change to "application/pdf"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, 100)
        }

        uploadButton.setOnClickListener {
            val title = fileTitle.text.toString().trim()
            if (title.isEmpty() || fileUri == null) {
                Toast.makeText(this, "Please enter a title and select a file", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parse tags → lowercase, trimmed, unique
            val rawTags = tagsInput.text?.toString().orEmpty()
            val tags = rawTags
                .split(',', ';', '#')                     // separators the user might try
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.lowercase(Locale.getDefault()) }
                .distinct()

            progressBar.visibility = ProgressBar.VISIBLE
            val fileRef = storage.child("documents/${UUID.randomUUID()}")

            Toast.makeText(this, "Starting upload...", Toast.LENGTH_SHORT).show()

            fileRef.putFile(fileUri!!)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        val user = auth.currentUser
                        val doc = hashMapOf(
                            "title" to title,
                            "titleLower" to title.lowercase(Locale.getDefault()), // helper for search
                            "fileUrl" to uri.toString(),
                            "uploadedBy" to (user?.displayName ?: user?.email ?: "Unknown"),
                            "uploadedByUid" to (user?.uid ?: "anonymous"),
                            "tags" to tags,                                        // NEW
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("documents").add(doc)
                            .addOnSuccessListener {
                                progressBar.visibility = ProgressBar.GONE
                                Toast.makeText(this, "Document saved to Firestore ✅", Toast.LENGTH_LONG).show()
                                // Optional: clear inputs
                                fileTitle.setText("")
                                tagsInput.setText("")
                                filePathText.text = "No file selected"
                                fileUri = null
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = ProgressBar.GONE
                                Toast.makeText(this, "Firestore error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }.addOnFailureListener { e ->
                        progressBar.visibility = ProgressBar.GONE
                        Toast.makeText(this, "Failed to get download URL: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            fileUri = data?.data
            filePathText.text = fileUri?.lastPathSegment ?: "File selected"
        }
    }
}
