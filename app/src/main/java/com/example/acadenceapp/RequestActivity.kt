package com.example.acadenceapp

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.acadenceapp.adapters.RequestCardAdapter
import com.example.acadenceapp.adapters.SimpleDocAdapter
import com.example.acadenceapp.models.RequestModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class RequestActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // UI refs (make sure these IDs exist in activity_request.xml)
    private lateinit var docSpinner: Spinner
    private lateinit var teacherSpinner: Spinner
    private lateinit var reviewTypeSpinner: Spinner
    private lateinit var urgencySpinner: Spinner
    private lateinit var visibilitySpinner: Spinner
    private lateinit var reviewDetails: EditText
    private lateinit var submitRequestButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var uploadsRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var requestsRecycler: androidx.recyclerview.widget.RecyclerView

    // Adapters
    private lateinit var uploadsAdapter: SimpleDocAdapter
    private lateinit var requestsAdapter: RequestCardAdapter

    // Local caches
    private val myDocTitles = mutableListOf<String>()
    private val myDocIdByTitle = mutableMapOf<String, String>()
    private val teacherUidByName = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request)

        // --- bind views ---
        docSpinner           = findViewById(R.id.documentSpinner)
        teacherSpinner       = findViewById(R.id.teacherSpinner)
        reviewTypeSpinner    = findViewById(R.id.reviewTypeSpinner)
        urgencySpinner       = findViewById(R.id.urgencySpinner)
        visibilitySpinner    = findViewById(R.id.visibilitySpinner)
        reviewDetails        = findViewById(R.id.reviewDetails)
        submitRequestButton  = findViewById(R.id.submitRequestButton)
        progressBar          = findViewById(R.id.progressBar)

        uploadsRecycler      = findViewById(R.id.uploadsRecycler)
        requestsRecycler     = findViewById(R.id.requestsRecycler)

        // --- static spinners ---
        reviewTypeSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Technical Review", "Supervisor Feedback", "Language/Clarity", "Formatting/Referencing", "Plagiarism Check")
        )
        urgencySpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Low", "Normal", "High", "Critical")
        )
        visibilitySpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Private (You + Assignee)", "Your Supervisors", "Your Cohort", "Public")
        )

        // --- recyclers ---
        uploadsAdapter = SimpleDocAdapter(mutableListOf())
        uploadsRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        uploadsRecycler.adapter = uploadsAdapter

        requestsAdapter = RequestCardAdapter(mutableListOf())
        requestsRecycler.layoutManager = LinearLayoutManager(this)
        requestsRecycler.adapter = requestsAdapter

        // --- data ---
        loadMyDocumentsIntoSpinnerAndList()
        loadTeachersIntoSpinner()
        observeMyRequests()

        // --- actions ---
        submitRequestButton.setOnClickListener { submitRequest() }
    }

    private fun loadMyDocumentsIntoSpinnerAndList() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("documents")
            .whereEqualTo("uploadedByUid", uid)
            .get()
            .addOnSuccessListener { snap ->
                myDocTitles.clear()
                myDocIdByTitle.clear()

                val df = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault())
                val cardItems = mutableListOf<Pair<String, String>>() // title, dateText

                for (doc in snap.documents) {
                    val title = doc.getString("title") ?: "Untitled"
                    val createdAt = doc.getTimestamp("createdAt")?.toDate()
                    val dateText = createdAt?.let { df.format(it) } ?: ""

                    myDocTitles.add(title)
                    myDocIdByTitle[title] = doc.id
                    cardItems.add(title to dateText)
                }

                docSpinner.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    if (myDocTitles.isEmpty()) listOf("No uploads yet") else myDocTitles
                )

                uploadsAdapter.update(cardItems)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Couldn't load your uploads", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadTeachersIntoSpinner() {
        db.collection("users")
            .whereEqualTo("role", "consultant")
            .get()
            .addOnSuccessListener { snap ->
                val names = mutableListOf<String>()
                teacherUidByName.clear()

                for (u in snap.documents) {
                    val name = u.getString("displayName") ?: u.getString("username") ?: "Unnamed Consultant"
                    names.add(name)
                    teacherUidByName[name] = u.id
                }

                // If no consultants found
                if (names.isEmpty()) {
                    names.add("No consultants available")
                    teacherUidByName["No consultants available"] = ""
                }

                teacherSpinner.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not load consultants", Toast.LENGTH_SHORT).show()
                teacherSpinner.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf("Error loading consultants")
                )
                teacherUidByName["Error loading consultants"] = ""
            }
    }


    /**
     * Avoids composite-index requirement by sorting client-side.
     * (If you build the index, you can switch to whereEqualTo + orderBy("createdAt", DESC) + addSnapshotListener.)
     */
    private fun observeMyRequests() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("requests")
            .whereEqualTo("requestedByUid", uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                // sort first, then map → no index required
                val sortedDocs = snap.documents.sortedByDescending {
                    it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                }

                val items = sortedDocs.map { d ->
                    RequestModel(
                        id = d.id,
                        docTitle = d.getString("docTitle") ?: "",
                        assignedToName = d.getString("assignedToName") ?: "",
                        reviewType = d.getString("reviewType") ?: "",
                        dueDate = d.getTimestamp("dueDate"),
                        status = d.getString("status") ?: "Pending"
                    )
                }
                requestsAdapter.update(items)
            }
    }


    private fun submitRequest() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }
        if (myDocTitles.isEmpty()) {
            Toast.makeText(this, "Please upload a document first", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedTitle = docSpinner.selectedItem?.toString() ?: ""
        val docId = myDocIdByTitle[selectedTitle] ?: ""

        val teacherName = teacherSpinner.selectedItem?.toString() ?: ""
        val teacherUid = teacherUidByName[teacherName] ?: ""

        val reviewType = reviewTypeSpinner.selectedItem?.toString() ?: "Review"
        val urgency = urgencySpinner.selectedItem?.toString() ?: "Normal"
        val visibility = visibilitySpinner.selectedItem?.toString() ?: "Private"
        val notes = reviewDetails.text.toString().trim()

        showPb(true)

        val now = Timestamp.now()
        val payload = hashMapOf(
            "docId"           to docId,
            "docTitle"        to selectedTitle,
            "assignedToUid"   to teacherUid,
            "assignedToName"  to teacherName,
            "reviewType"      to reviewType,
            "urgency"         to urgency,
            "visibility"      to visibility,
            "reviewDetails"   to notes,
            "dueDate"         to now,        // date picker removed → save "now"
            "requestedByUid"  to uid,
            "status"          to "Pending",
            "createdAt"       to now
        )

        db.collection("requests")
            .add(payload)
            .addOnSuccessListener {
                showPb(false)
                Toast.makeText(this, "Request submitted", Toast.LENGTH_SHORT).show()
                reviewDetails.setText("")
            }
            .addOnFailureListener { e ->
                showPb(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showPb(show: Boolean) {
        // Defensive: only touch if initialized
        if (::progressBar.isInitialized) {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }
}
