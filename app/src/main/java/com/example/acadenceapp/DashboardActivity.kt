package com.example.acadenceapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.adapters.NotificationAdapter
import com.example.acadenceapp.models.NotificationModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class DashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationModel>()
    private var firestoreListener: ListenerRegistration? = null

    private lateinit var unreadCountBadge: TextView
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        firestore = FirebaseFirestore.getInstance()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        drawerLayout = findViewById(R.id.drawer_layout)
        unreadCountBadge = findViewById(R.id.unreadCountBadge)

        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        val btnNotifications: ImageButton = findViewById(R.id.btnNotifications)
        val btnCloseNotifications: ImageButton = findViewById(R.id.btn_close_notifications)
        val rvNotifications: RecyclerView = findViewById(R.id.rvNotifications)

        adapter = NotificationAdapter(notificationList)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        btnNotifications.setOnClickListener { drawerLayout.openDrawer(GravityCompat.END) }
        btnCloseNotifications.setOnClickListener { drawerLayout.closeDrawer(GravityCompat.END) }

        setupFirestoreListener()
    }

    private fun setupFirestoreListener() {
        firestoreListener = firestore.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firestore", "Error getting documents", e)
                    return@addSnapshotListener
                }

                val notifications = snapshots?.documents?.mapNotNull { doc ->
                    try {
                        val raw = doc.data ?: return@mapNotNull null
                        val readByValue = raw["readBy"]
                        val safeReadBy = when (readByValue) {
                            is String -> listOf(readByValue)
                            is List<*> -> readByValue.filterIsInstance<String>()
                            else -> emptyList()
                        }

                        NotificationModel(
                            id = doc.id,
                            title = raw["title"] as? String ?: "",
                            message = raw["message"] as? String ?: "",
                            priority = raw["priority"] as? String ?: "Low",
                            timestamp = raw["timestamp"] as? Timestamp ?: Timestamp.now(),
                            readBy = safeReadBy,
                            recipientIds = (raw["recipientIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            sentBy = raw["sentBy"] as? String ?: ""
                        )
                    } catch (ex: Exception) {
                        Log.e("Deserialization", "Error parsing notification", ex)
                        null
                    }
                } ?: emptyList()

                val filtered = notifications.filter { it.recipientIds.contains(currentUserId) }
                adapter.updateData(filtered)

                val unread = filtered.count { !it.readBy.contains(currentUserId) }
                unreadCountBadge.visibility = if (unread > 0) View.VISIBLE else View.GONE
                unreadCountBadge.text = unread.toString()
            }
    }

    override fun onDestroy() {
        firestoreListener?.remove()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}
