package com.example.acadenceapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class UploadActivity : AppCompatActivity() {

    private lateinit var fileTitle: EditText
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
        pickFileButton = findViewById(R.id.pickFileButton)
        uploadButton = findViewById(R.id.uploadButton)
        filePathText = findViewById(R.id.filePathText)
        progressBar = findViewById(R.id.progressBar)

        pickFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*" // all file types; restrict to "application/pdf" if needed
            }
            startActivityForResult(intent, 100)
        }

        uploadButton.setOnClickListener {
            val title = fileTitle.text.toString().trim()
            if (title.isEmpty() || fileUri == null) {
                Toast.makeText(this, "Please enter a title and select a file", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = ProgressBar.VISIBLE
            val fileRef = storage.child("documents/${UUID.randomUUID()}")

            Toast.makeText(this, "Starting upload...", Toast.LENGTH_SHORT).show()

            fileRef.putFile(fileUri!!)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        val user = auth.currentUser
                        val doc = hashMapOf(
                            "title" to title,
                            "fileUrl" to uri.toString(),
                            "uploadedBy" to (user?.displayName ?: user?.email ?: "Unknown"),
                            "uploadedByUid" to (user?.uid ?: "anonymous"),
                            "createdAt" to Date()
                        )
                        db.collection("documents").add(doc)
                            .addOnSuccessListener {
                                progressBar.visibility = ProgressBar.GONE
                                Toast.makeText(this, "Document saved to Firestore ✅", Toast.LENGTH_LONG).show()
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
