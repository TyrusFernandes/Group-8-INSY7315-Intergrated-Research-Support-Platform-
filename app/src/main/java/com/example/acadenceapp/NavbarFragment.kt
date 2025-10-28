package com.example.acadenceapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NavbarFragment : Fragment() {

    private lateinit var navView: BottomNavigationView
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var userRole: String = "student" // default

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_navbar, container, false)
        navView = view.findViewById(R.id.nav_view)

        fetchUserRoleAndSetupNavigation()

        return view
    }

    private fun fetchUserRoleAndSetupNavigation() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                userRole = document.getString("role") ?: "student"
                setupNavigationBar()
            }
            .addOnFailureListener { e ->
                Log.e("NavbarFragment", "Error fetching role", e)
                setupNavigationBar() // fallback
            }
    }

    private fun setupNavigationBar() {
        if (userRole == "consultant") {
            // Replace "add+" with Requests
            val menu = navView.menu
            val menuItem = menu.findItem(R.id.navigation_add)
            menuItem.title = "Requests"
            menuItem.setIcon(R.drawable.ic_document)
        }

        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(requireContext(), DashboardActivity::class.java))
                    true
                }
                R.id.navigation_for_you -> {
                    startActivity(Intent(requireContext(), ForYouActivity::class.java))
                    true
                }
                R.id.navigation_add -> {
                    if (userRole == "consultant") {
                        startActivity(Intent(requireContext(), ConsultantRequestsActivity::class.java))
                    } else {
                        startActivity(Intent(requireContext(), DocumentActivity::class.java))
                    }
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
                else -> false
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = NavbarFragment()
    }
}
