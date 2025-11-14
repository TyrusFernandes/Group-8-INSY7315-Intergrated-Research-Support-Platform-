package com.example.acadenceapp

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FeedbackActivity : AppCompatActivity() {

    private lateinit var consultantSpinner: Spinner
    private lateinit var commentsBox: EditText
    private lateinit var submitBtn: Button
    private lateinit var firestore: FirebaseFirestore
    private var selectedConsultantUid: String? = null

    private lateinit var star1: ImageView
    private lateinit var star2: ImageView
    private lateinit var star3: ImageView
    private lateinit var star4: ImageView
    private lateinit var star5: ImageView

    private var rating = 0

    private val consultantMap = mutableMapOf<String, String>() // name to uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        consultantSpinner = findViewById(R.id.consultantSpinner)
        commentsBox = findViewById(R.id.commentsBox)
        submitBtn = findViewById(R.id.submitBtn)
        firestore = FirebaseFirestore.getInstance()
        star1 = findViewById(R.id.star1)
        star2 = findViewById(R.id.star2)
        star3 = findViewById(R.id.star3)
        star4 = findViewById(R.id.star4)
        star5 = findViewById(R.id.star5)

        setupStarRating()
        loadConsultants()

        submitBtn.setOnClickListener {
            val ratingValue = rating
            if (ratingValue == 0) {
                Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val comment = commentsBox.text.toString()
            val studentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val consultantUid = selectedConsultantUid ?: return@setOnClickListener

            val feedback = hashMapOf(
                "studentUid" to studentUid,
                "consultantUid" to consultantUid,
                "rating" to rating,
                "comment" to comment,
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("feedbacks").add(feedback)
                .addOnSuccessListener {
                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to submit feedback.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupStarRating() {
        val stars = listOf(star1, star2, star3, star4, star5)

        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                rating = index + 1
                updateStars()
            }
        }
    }

    private fun updateStars() {
        val stars = listOf(star1, star2, star3, star4, star5)
        stars.forEachIndexed { index, star ->
            if (index < rating) star.setColorFilter(getColor(R.color.light_yellow))
            else star.setColorFilter(getColor(R.color.gray_light))
        }
    }


    private fun loadConsultants() {
        firestore.collection("users").whereEqualTo("role", "consultant").get()
            .addOnSuccessListener { querySnapshot ->
                val names = mutableListOf<String>()
                for (doc in querySnapshot.documents) {
                    val name = doc.getString("displayName") ?: doc.getString("username") ?: doc.getString("email") ?: "Unknown"
                    val uid = doc.id
                    consultantMap[name] = uid
                    names.add(name)
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
                consultantSpinner.adapter = adapter

                consultantSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        selectedConsultantUid = consultantMap[names[position]]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not load consultants", Toast.LENGTH_SHORT).show()
            }
    }
}
