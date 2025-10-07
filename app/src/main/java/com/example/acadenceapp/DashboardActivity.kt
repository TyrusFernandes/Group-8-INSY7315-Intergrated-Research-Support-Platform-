package com.example.acadenceapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.adapters.NotificationAdapter
import com.example.acadenceapp.models.NotificationModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class DashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationModel>()
    private var firestoreListener: ListenerRegistration? = null

    // Track the number of notifications to detect new ones
    private var initialNotificationCount = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance()

        drawerLayout = findViewById(R.id.drawer_layout)
        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        val btnNotifications: ImageButton = findViewById(R.id.btnNotifications)
        val btnCloseNotifications: ImageButton = findViewById(R.id.btn_close_notifications)
        val btnForYou: Button = findViewById(R.id.btnArticle)
        val btnJobs: Button = findViewById(R.id.btnJobs)

        // --- New UI Elements for Sending ---
        val etNotificationText: EditText = findViewById(R.id.etNotificationText)
        val btnSendNotification: Button = findViewById(R.id.btnSendNotification)
        // -----------------------------------

        // --- RecyclerView Setup ---
        val rvNotifications: RecyclerView = findViewById(R.id.rvNotifications)
        adapter = NotificationAdapter(notificationList)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter
        // --------------------------

        // Load and listen for real-time notifications
        setupFirestoreListener()

        // Click listeners
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        btnNotifications.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        btnCloseNotifications.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        btnForYou.setOnClickListener {
            val intent = Intent(this, ForYouActivity::class.java)
            startActivity(intent)
        }

        btnJobs.setOnClickListener {
            val intent = Intent(this, JobActivity::class.java)
            startActivity(intent)
        }

        // --- Send Notification Logic ---
        btnSendNotification.setOnClickListener {
            val message = etNotificationText.text.toString().trim()
            if (message.isNotEmpty()) {
                sendNotificationToFirebase(message)
                etNotificationText.text.clear() // Clear the input box
            } else {
                Toast.makeText(this, "Please enter a message.", Toast.LENGTH_SHORT).show()
            }
        }
        // --------------------------------
    }

    // Function to send data to Firestore
    private fun sendNotificationToFirebase(message: String) {
        val notificationData = hashMapOf(
            "message" to message,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("Firebase", "Notification sent successfully with ID: ${it.id}")
                Toast.makeText(this, "Message sent to Firebase!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w("Firebase", "Error adding document", e)
                Toast.makeText(this, "Error sending notification.", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to listen for real-time updates and show Toast
    private fun setupFirestoreListener() {
        firestoreListener = firestore.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Order by newest first
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val newNotifications = snapshots.documents.mapNotNull { document ->
                        // Convert document to NotificationItem data class
                        document.toObject(NotificationModel::class.java)?.copy(id = document.id)
                    }

                    // Update the RecyclerView list
                    adapter.updateData(newNotifications)

                    // Logic to show Toast for a new notification (not tied to the button)
                    if (initialNotificationCount == -1) {
                        // First run: just set the count
                        initialNotificationCount = newNotifications.size
                    } else if (newNotifications.size > initialNotificationCount) {
                        // Data size increased: a new notification arrived
                        Toast.makeText(this, "🔔 New notification received!", Toast.LENGTH_LONG).show()
                        initialNotificationCount = newNotifications.size
                    } else if (newNotifications.size < initialNotificationCount) {
                        // Case where a document was deleted
                        initialNotificationCount = newNotifications.size
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop listening for updates when the activity is destroyed
        firestoreListener?.remove()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}