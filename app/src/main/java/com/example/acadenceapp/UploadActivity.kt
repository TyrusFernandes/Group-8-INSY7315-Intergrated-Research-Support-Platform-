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
    private lateinit var pickThumbnailButton: Button
    private lateinit var uploadButton: Button
    private lateinit var filePathText: TextView
    private lateinit var thumbnailPreview: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var checkShowInFeed: CheckBox

    private var fileUri: Uri? = null
    private var thumbnailUri: Uri? = null

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        fileTitle = findViewById(R.id.fileTitle)
        tagsInput = findViewById(R.id.tagsInput)
        pickFileButton = findViewById(R.id.pickFileButton)
        pickThumbnailButton = findViewById(R.id.pickThumbnailButton)
        uploadButton = findViewById(R.id.uploadButton)
        filePathText = findViewById(R.id.filePathText)
        thumbnailPreview = findViewById(R.id.thumbnailPreview)
        progressBar = findViewById(R.id.progressBar)
        checkShowInFeed = findViewById(R.id.checkShowInFeed)

        pickFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "image/jpeg",
                    "image/png"
                ))
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, 100)
        }

        pickThumbnailButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, 101)
        }

        uploadButton.setOnClickListener {
            val title = fileTitle.text.toString().trim()
            if (title.isEmpty() || fileUri == null) {
                Toast.makeText(this, "Please enter a title and select a file", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parse tags
            val rawTags = tagsInput.text?.toString().orEmpty()
            val tags = rawTags
                .split(',', ';', '#')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.lowercase(Locale.getDefault()) }
                .distinct()

            val isOnline = NetworkUtils.isOnline(this)

            progressBar.visibility = ProgressBar.VISIBLE

            if (isOnline) {
                // ✅ Online → upload to Storage immediately
                uploadOnline(title, tags)
            } else {
                // ✅ Offline → save placeholder Firestore doc (what you see in your screenshot)
                saveOfflinePlaceholder(title, tags)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            when (requestCode) {
                100 -> {
                    fileUri = data.data
                    filePathText.text = fileUri?.lastPathSegment ?: "File selected"
                }
                101 -> {
                    thumbnailUri = data.data
                    thumbnailPreview.setImageURI(thumbnailUri)
                    thumbnailPreview.visibility = ImageView.VISIBLE
                }
            }
        }
    }

    // === ONLINE PATH =========================================================

    private fun uploadOnline(
        title: String,
        tags: List<String>
    ) {
        val fileRef = storage.child("documents/${UUID.randomUUID()}")
        val selectedFileUri = fileUri!!

        fileRef.putFile(selectedFileUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { fileDownloadUrl ->

                    if (thumbnailUri != null) {
                        val thumbRef = storage.child("thumbnails/${UUID.randomUUID()}")
                        thumbRef.putFile(thumbnailUri!!)
                            .addOnSuccessListener {
                                thumbRef.downloadUrl.addOnSuccessListener { thumbDownloadUrl ->
                                    saveDocumentToFirestore(
                                        title,
                                        fileDownloadUrl.toString(),
                                        thumbDownloadUrl.toString(),
                                        tags,
                                        uploadPending = false,
                                        localFileUri = null,
                                        localThumbUri = null
                                    )
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Thumbnail upload failed", Toast.LENGTH_SHORT).show()
                                saveDocumentToFirestore(
                                    title,
                                    fileDownloadUrl.toString(),
                                    null,
                                    tags,
                                    uploadPending = false,
                                    localFileUri = null,
                                    localThumbUri = null
                                )
                            }
                    } else {
                        saveDocumentToFirestore(
                            title,
                            fileDownloadUrl.toString(),
                            null,
                            tags,
                            uploadPending = false,
                            localFileUri = null,
                            localThumbUri = null
                        )
                    }

                }.addOnFailureListener {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(this, "File URL fetch failed", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "File upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    // === OFFLINE PLACEHOLDER =================================================

    private fun saveOfflinePlaceholder(
        title: String,
        tags: List<String>
    ) {
        val user = auth.currentUser
        val doc = hashMapOf(
            "title" to title,
            "titleLower" to title.lowercase(Locale.getDefault()),
            "fileUrl" to "",                  // will be filled by sync
            "thumbnailUrl" to "",             // will be filled by sync
            "localFileUri" to fileUri!!.toString(),
            "localThumbnailUri" to (thumbnailUri?.toString() ?: ""),
            "uploadedBy" to (user?.displayName ?: user?.email ?: "Unknown"),
            "uploadedByUid" to (user?.uid ?: "anonymous"),
            "tags" to tags,
            "createdAt" to FieldValue.serverTimestamp(),
            "studentChecked" to checkShowInFeed.isChecked,
            "uploadPending" to true           // mark as needing sync
        )

        db.collection("documents").add(doc)
            .addOnSuccessListener {
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "Saved offline – will upload when online ✅", Toast.LENGTH_LONG).show()
                clearInputs()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "Firestore error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // === COMMON FIRESTORE SAVE ==============================================

    private fun saveDocumentToFirestore(
        title: String,
        fileUrl: String,
        thumbnailUrl: String?,
        tags: List<String>,
        uploadPending: Boolean,
        localFileUri: String?,
        localThumbUri: String?
    ) {
        val user = auth.currentUser
        val doc = hashMapOf(
            "title" to title,
            "titleLower" to title.lowercase(Locale.getDefault()),
            "fileUrl" to fileUrl,
            "thumbnailUrl" to (thumbnailUrl ?: ""),
            "uploadedBy" to (user?.displayName ?: user?.email ?: "Unknown"),
            "uploadedByUid" to (user?.uid ?: "anonymous"),
            "tags" to tags,
            "createdAt" to FieldValue.serverTimestamp(),
            "studentChecked" to checkShowInFeed.isChecked,
            "uploadPending" to uploadPending,
        ) as MutableMap<String, Any?>

        if (localFileUri != null) doc["localFileUri"] = localFileUri
        if (localThumbUri != null) doc["localThumbnailUri"] = localThumbUri

        db.collection("documents").add(doc)
            .addOnSuccessListener {
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "Uploaded successfully ✅", Toast.LENGTH_LONG).show()
                clearInputs()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "Firestore error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearInputs() {
        fileTitle.setText("")
        tagsInput.setText("")
        filePathText.text = "No file selected"
        fileUri = null
        thumbnailUri = null
        thumbnailPreview.setImageDrawable(null)
        thumbnailPreview.visibility = ImageView.GONE
    }
}
