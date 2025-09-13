package com.example.acadenceapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Context
import com.example.acadenceapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent

class NavbarFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_navbar, container, false)

        // Find the BottomNavigationView
        val navView: BottomNavigationView = view.findViewById(R.id.nav_view)

        // Handle item clicks (for now, just show which item was tapped)
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(requireContext(), DashboardActivity::class.java))
                    true
                }
                R.id.navigation_for_you -> {
                    startActivity(Intent(requireContext(), ForYouPageActivity::class.java))
                    true
                }
                R.id.navigation_add -> {
                    startActivity(Intent(requireContext(), DocumentActivity::class.java))
                    true
                }
                R.id.navigation_messages -> {
                    startActivity(Intent(requireContext(), MessageActivity::class.java))
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(requireContext(), ProfileActivity::class.java))
                    true
                }
            }
            true
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = NavbarFragment()
    }
}