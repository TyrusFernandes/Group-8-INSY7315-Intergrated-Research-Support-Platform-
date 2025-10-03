package com.example.acadenceapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class RequestActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request)

        // Match IDs from XML
        val docSpinner: Spinner = findViewById(R.id.documentSpinner)
        val reviewTypeSpinner: Spinner = findViewById(R.id.reviewTypeSpinner)
        val reviewDetails: EditText = findViewById(R.id.reviewDetails)
        val reviewDueDate: EditText = findViewById(R.id.reviewDueDate)
        val btnSubmit: Button = findViewById(R.id.submitRequestButton)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)

        // Example dropdowns
        val docOptions = arrayOf("Data Analytics Research", "Machine Learning Paper", "Other")
        docSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, docOptions)

        val reviewTypes = arrayOf("Technical Review", "Supervisor Feedback", "Peer Review")
        reviewTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, reviewTypes)

        btnSubmit.setOnClickListener {
            progressBar.visibility = ProgressBar.VISIBLE

            val request = hashMapOf(
                "docTitle" to docSpinner.selectedItem.toString(),
                "reviewType" to reviewTypeSpinner.selectedItem.toString(),
                "reviewDetails" to reviewDetails.text.toString(),
                "dueDate" to reviewDueDate.text.toString(),
                "requestedBy" to (auth.currentUser?.uid ?: "anonymous"),
                "status" to "Pending",
                "createdAt" to Date()
            )

            db.collection("requests")
                .add(request)
                .addOnSuccessListener {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(this, "Request submitted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
