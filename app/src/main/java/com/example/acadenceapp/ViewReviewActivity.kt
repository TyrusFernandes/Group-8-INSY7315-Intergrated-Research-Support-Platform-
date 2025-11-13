package com.example.acadenceapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem // Import for MenuItem
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.acadenceapp.adapters.ReviewPagerAdapter

class ViewReviewActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: ReviewPagerAdapter

    // Data structure to hold reviews grouped by request ID
    private val requestReviews = mutableMapOf<String, MutableList<Map<String, Any>>>()

    // Data structure to hold the title for each request ID, fetched from the review doc itself
    private val requestTitles = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_review)

        firestore = FirebaseFirestore.getInstance()
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // Set up the back button (Up button)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Review History" // Set a title for the activity

        fetchReviews()
    }

    override fun onSupportNavigateUp(): Boolean {
        // This handles clicks on the Up button (the back arrow in the action bar)
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // This is necessary if you're using older AppCompatActivity methods for navigation
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fetchReviews() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("reviews")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { query ->

                // Clear data structures before loading
                requestReviews.clear()
                requestTitles.clear()

                for (doc in query.documents) {
                    val review = doc.data ?: continue
                    val requestId = review["requestId"] as? String ?: continue

                    // NEW: Extract docTitle directly from the review document
                    val docTitle = review["docTitle"] as? String ?: "Untitled Request"

                    if (!requestReviews.containsKey(requestId)) {
                        requestReviews[requestId] = mutableListOf()
                    }
                    requestReviews[requestId]?.add(review)

                    // NEW: Store the title, assuming all reviews for the same request have the same title
                    requestTitles[requestId] = docTitle
                }

                if (requestReviews.isEmpty()) {
                    Log.d("ViewReviewActivity", "No reviews found for user $userId.")
                    // You might want to display a message here
                    return@addOnSuccessListener
                }

                // Setup ViewPager and Tabs
                adapter = ReviewPagerAdapter(this, requestReviews)
                viewPager.adapter = adapter

                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    val keys = requestReviews.keys.toList()
                    val requestId = keys[position]

                    // NEW: Use the fetched docTitle for the tab text
                    tab.text = requestTitles[requestId] ?: "Error: Title Missing"
                }.attach()
            }
            .addOnFailureListener { e ->
                // This will still log the FAILED_PRECONDITION error until the index is built
                Log.e("ViewReviewActivity", "Error loading reviews", e)
            }
    }
}