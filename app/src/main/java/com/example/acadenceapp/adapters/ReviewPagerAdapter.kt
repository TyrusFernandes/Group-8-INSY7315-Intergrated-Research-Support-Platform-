package com.example.acadenceapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.Fragment
import com.example.acadenceapp.ReviewListFragment
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class ReviewPagerAdapter(
    activity: FragmentActivity,
    private val requestReviews: Map<String, MutableList<Map<String, Any>>>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = requestReviews.size

    override fun createFragment(position: Int): Fragment {
        val requestId = requestReviews.keys.toList()[position]
        val reviews = requestReviews[requestId] ?: emptyList()
        return ReviewListFragment.newInstance(ArrayList(reviews.map { HashMap(it) }))
    }
}