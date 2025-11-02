package com.example.acadenceapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.adapters.DocumentAdapter
import com.example.acadenceapp.models.DocumentModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DocumentActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val lang = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "English"
        val context = newBase?.let { LocaleHelper.setLocale(it, lang) }
        super.attachBaseContext(context)
    }


    private val db = FirebaseFirestore.getInstance()
    private lateinit var recentFilesRecycler: RecyclerView
    private lateinit var documentListRecycler: RecyclerView
    private lateinit var recentAdapter: DocumentAdapter
    private lateinit var allAdapter: DocumentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document)

        val btnUpload: Button = findViewById(R.id.uploadButton)
        val btnRequest: Button = findViewById(R.id.requestButton)

        // Initialize RecyclerViews
        recentFilesRecycler = findViewById(R.id.recentFilesRecycler)
        documentListRecycler = findViewById(R.id.documentListRecycler)

        // Setup adapters
        recentAdapter = DocumentAdapter(this, mutableListOf())
        allAdapter = DocumentAdapter(this, mutableListOf())

        // Layout managers
        recentFilesRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        documentListRecycler.layoutManager = LinearLayoutManager(this)

        // Attach adapters
        recentFilesRecycler.adapter = recentAdapter
        documentListRecycler.adapter = allAdapter

        // Buttons
        btnUpload.setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
        }

        btnRequest.setOnClickListener {
            startActivity(Intent(this, RequestActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadDocuments()
    }

    private fun loadDocuments() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("documents")
            .whereEqualTo("uploadedByUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val docs = snapshot.toObjects(DocumentModel::class.java)
                if (docs.isNotEmpty()) {
                    recentAdapter.updateData(docs.take(5)) // first 5 → Recent Files
                    allAdapter.updateData(docs)           // all → All Files
                } else {
                    Toast.makeText(this, "No documents found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading documents", Toast.LENGTH_SHORT).show()
            }
    }
}
