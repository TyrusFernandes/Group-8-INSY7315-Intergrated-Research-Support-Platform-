package com.example.acadenceapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class ReviewListFragment : Fragment() {

    companion object {
        /**
         * Pass an ArrayList<HashMap<String, Any>> where each HashMap is a review document.
         */
        fun newInstance(reviews: ArrayList<Map<String, Any>>): ReviewListFragment {
            val fragment = ReviewListFragment()
            val args = Bundle()
            args.putSerializable("reviews", reviews)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var containerLayout: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_review_list, container, false)
        containerLayout = view.findViewById(R.id.reviewsContainer)

        // Retrieve reviews that were passed in
        @Suppress("UNCHECKED_CAST")
        val reviews = arguments?.getSerializable("reviews") as? ArrayList<HashMap<String, Any>> ?: arrayListOf()

        displayReviews(reviews)
        return view
    }

    private fun displayReviews(reviews: List<Map<String, Any>>) {
        containerLayout.removeAllViews()
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

        for (review in reviews) {
            try {
                val card = layoutInflater.inflate(R.layout.item_review, containerLayout, false)

                val txtConsultantName = card.findViewById<TextView>(R.id.txtConsultantName)
                val txtRating = card.findViewById<TextView>(R.id.txtRating)
                val txtFeedback = card.findViewById<TextView>(R.id.txtFeedback)
                val txtTimestamp = card.findViewById<TextView>(R.id.txtTimestamp)
                val btnDownload = card.findViewById<Button>(R.id.btnDownload)

                val consultantName = when {
                    review["consultantName"] is String -> review["consultantName"] as String
                    review["consultantId"] is String -> review["consultantId"] as String
                    else -> "Consultant"
                }
                txtConsultantName.text = "By: $consultantName"

                val ratingVal = when (val r = review["rating"]) {
                    is Number -> r.toDouble()
                    is String -> r.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
                txtRating.text = "Rating: ${ratingVal.toInt()}%"

                txtFeedback.text = "Feedback: ${review["feedback"] ?: ""}"

                // timestamp handling: Firestore timestamp may be stored as String, Long (ms), or a com.google.firebase.Timestamp on Android side.
                val timestampDisplay = when (val ts = review["timestamp"]) {
                    is Long -> {
                        sdf.format(Date(ts))
                    }
                    is Double -> {
                        sdf.format(Date(ts.toLong()))
                    }
                    is String -> {
                        // try to parse ISO or fallback
                        ts
                    }
                    is com.google.firebase.Timestamp -> {
                        sdf.format(ts.toDate())
                    }
                    else -> "Unknown"
                }
                txtTimestamp.text = "Submitted: $timestampDisplay"

                // fileUrl handling
                val fileUrl = (review["fileUrl"] as? String)?.takeIf { it.isNotBlank() }
                if (!fileUrl.isNullOrEmpty()) {
                    btnDownload.visibility = View.VISIBLE
                    btnDownload.setOnClickListener {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                            startActivity(intent)
                        } catch (ex: Exception) {
                            Log.e("ReviewListFragment", "Failed to open file URL", ex)
                        }
                    }
                } else {
                    btnDownload.visibility = View.GONE
                }

                containerLayout.addView(card)
            } catch (ex: Exception) {
                Log.e("ReviewListFragment", "Error rendering review card", ex)
            }
        }
    }
}
