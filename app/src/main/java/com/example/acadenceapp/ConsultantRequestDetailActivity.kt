package com.example.acadenceapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.adapters.CommentAdapter
import com.example.acadenceapp.models.CommentModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ConsultantRequestDetailActivity : AppCompatActivity() {

    private lateinit var docTitleText: TextView
    private lateinit var reviewTypeText: TextView
    private lateinit var urgencyText: TextView
    private lateinit var visibilityText: TextView
    private lateinit var reviewDetailsText: TextView
    private lateinit var openDocBtn: Button
    private lateinit var commentInput: EditText
    private lateinit var submitCommentBtn: Button
    private lateinit var commentsRecycler: RecyclerView
    private lateinit var statusSpinner: Spinner

    private lateinit var commentsAdapter: CommentAdapter
    private val commentList = mutableListOf<CommentModel>()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var requestId: String? = null
    private var docId: String? = null
    private var docUrl: String? = null

    override fun attachBaseContext(newBase: Context?) {
        val lang = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "English"
        val context = newBase?.let { LocaleHelper.setLocale(it, lang) }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultant_request_detail)

        // Bind views
        docTitleText = findViewById(R.id.docTitleText)
        reviewTypeText = findViewById(R.id.reviewTypeText)
        urgencyText = findViewById(R.id.urgencyText)
        visibilityText = findViewById(R.id.visibilityText)
        reviewDetailsText = findViewById(R.id.reviewDetailsText)
        openDocBtn = findViewById(R.id.openDocBtn)
        commentInput = findViewById(R.id.commentInput)
        submitCommentBtn = findViewById(R.id.submitCommentBtn)
        commentsRecycler = findViewById(R.id.commentsRecycler)
        statusSpinner = findViewById(R.id.statusSpinner)

        requestId = intent.getStringExtra("requestId")
        if (requestId == null) {
            Toast.makeText(this, "Missing request ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup comments
        commentsAdapter = CommentAdapter(commentList)
        commentsRecycler.layoutManager = LinearLayoutManager(this)
        commentsRecycler.adapter = commentsAdapter

        loadRequestDetails()
        loadComments()

        openDocBtn.setOnClickListener {
            docUrl?.let { url ->
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            } ?: Toast.makeText(this, "No document URL available", Toast.LENGTH_SHORT).show()
        }

        submitCommentBtn.setOnClickListener { submitComment() }

        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val newStatus = parent?.getItemAtPosition(position).toString()
                updateStatus(newStatus)
            }
        }
    }

    private fun loadRequestDetails() {
        firestore.collection("requests").document(requestId!!)
            .get()
            .addOnSuccessListener { doc ->
                docTitleText.text = doc.getString("docTitle")
                reviewTypeText.text = doc.getString("reviewType")
                urgencyText.text = doc.getString("urgency")
                visibilityText.text = doc.getString("visibility")
                reviewDetailsText.text = doc.getString("reviewDetails")
                docId = doc.getString("docId")
                val currentStatus = doc.getString("status") ?: "Pending"
                val dueDate = doc.getTimestamp("dueDate")?.toDate()

                // Check if overdue
                val now = Timestamp.now().toDate()
                val isOverdue = dueDate != null && dueDate.before(now) && currentStatus != "Submitted"
                val finalStatus = if (isOverdue) "Overdue" else currentStatus

                // Optional: update Firestore if overdue
                if (isOverdue && currentStatus != "Overdue") {
                    firestore.collection("requests").document(requestId!!)
                        .update("status", "Overdue")
                }


                // Setup spinner with allowed statuses for consultant
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                FirebaseFirestore.getInstance().collection("users").document(userId ?: "")
                    .get()
                    .addOnSuccessListener { userDoc ->
                        val role = userDoc.getString("role") ?: "student"

                        if (role == "consultant") {
                            // Consultant can edit: show limited allowed statuses
                            val allowed = listOf("Pending", "In Progress", "Awaiting Feedback", "Submitted")
                            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, allowed)
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            statusSpinner.adapter = adapter

                            val pos = allowed.indexOfFirst { it.equals(finalStatus, ignoreCase = true) }
                            if (pos != -1) statusSpinner.setSelection(pos)

                            statusSpinner.isEnabled = true
                            statusSpinner.isClickable = true

                        } else {
                            // Student: disable spinner
                            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf(finalStatus))
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            statusSpinner.adapter = adapter

                            statusSpinner.isEnabled = false
                            statusSpinner.isClickable = false
                        }
                    }

                // Load document URL
                if (docId != null) {
                    firestore.collection("documents").document(docId!!)
                        .get()
                        .addOnSuccessListener { documentSnapshot ->
                            docUrl = documentSnapshot.getString("fileUrl")
                        }
                }
            }
    }


    private fun updateStatus(newStatus: String) {
        firestore.collection("requests").document(requestId!!)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadComments() {
        firestore.collection("requests").document(requestId!!)
            .collection("comments")
            .orderBy("timestamp") // ASCENDING by default
            .get()
            .addOnSuccessListener { snapshot ->
                commentList.clear()
                snapshot?.documents?.forEach { doc ->
                    commentList.add(
                        CommentModel(
                            userId = doc.getString("userId") ?: "",
                            username = doc.getString("username") ?: "Unknown",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                        )
                    )
                }
                commentsAdapter.notifyDataSetChanged()
            }
    }


    private fun submitComment() {
        val uid = auth.currentUser?.uid ?: return
        val name = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Consultant"
        var text = commentInput.text.toString().trim()

        if (text.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val comment = hashMapOf(
            "text" to text,
            "userId" to uid,
            "username" to name,
            "timestamp" to Timestamp.now()
        )

        firestore.collection("requests").document(requestId!!)
            .collection("comments")
            .add(comment)
            .addOnSuccessListener {
                commentInput.setText("")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to submit comment", Toast.LENGTH_SHORT).show()
            }
        firestore.collection("requests")
            .document(requestId!!)
            .collection("ai_feedback")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val feedback = snapshot.documents.first().getString("summary")
                    findViewById<TextView>(R.id.aiFeedbackText).apply {
                        visibility = View.VISIBLE
                        text = "🤖 AI Feedback:\n\n$feedback"
                    }
                }
            }
    }
    private fun loadAiFeedback(requestId: String) {
        val db = FirebaseFirestore.getInstance()
        val aiFeedbackContainer = findViewById<TextView>(R.id.aiFeedbackText) // Add this TextView in XML

        db.collection("requests")
            .document(requestId)
            .collection("ai_feedback")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, _ ->
                if (snap != null && !snap.isEmpty) {
                    val feedback = snap.documents.first().getString("summary") ?: "No feedback yet."
                    aiFeedbackContainer.text = feedback
                } else {
                    aiFeedbackContainer.text = "No AI feedback yet."
                }
            }
    }

}

