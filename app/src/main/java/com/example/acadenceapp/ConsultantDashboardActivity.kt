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
import androidx.core.content.ContextCompat

class ConsultantDashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: NotificationAdapter
    private lateinit var profileViewsText: TextView
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
        profileViewsText = findViewById(R.id.profileViewsText)

        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        val btnNotifications: ImageButton = findViewById(R.id.btnNotifications)
        val btnCloseNotifications: ImageButton = findViewById(R.id.btn_close_notifications)
        val rvNotifications: RecyclerView = findViewById(R.id.rvNotifications)
        val roleLabel: TextView = findViewById(R.id.roleLabel)

        FirebaseFirestore.getInstance().collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid ?: return)
            .get()
            .addOnSuccessListener { document ->
                val role = document.getString("role") ?: "User"
                roleLabel.text = role
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Failed to fetch user role", e)
                roleLabel.text = "User"
            }


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
        fetchTopLikedDocuments()
        fetchTotalProfileViews()
        fetchOngoingRequestsCount()
        fetchTotalPriceForRequests()
        fetchUploadedDocumentsCount()
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
    private fun fetchTopLikedDocuments() {
        val documentsLayout: LinearLayout = findViewById(R.id.yourProjectsContainer)
        val context = this

        firestore.collection("documents")
            .whereEqualTo("uploadedByUid", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents.mapNotNull { doc ->
                    val likesList = doc.get("likes") as? List<*>
                    val likeCount = likesList?.size ?: 0
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val documentId = doc.id
                    Triple(documentId, title, likeCount)
                }.sortedByDescending { it.third }

                val top3 = documents.take(3)
                documentsLayout.removeAllViews()

                for ((index, doc) in top3.withIndex()) {
                    val (docId, title, _) = doc

                    val itemLayout = LinearLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 16
                        }
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(24, 24, 24, 24)
                        setBackgroundResource(R.drawable.rounded_white_bg)
                        elevation = 8f
                    }

                    val icon = ImageView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(64, 64).apply {
                            marginEnd = 16
                        }
                        setImageResource(R.drawable.ic_document)
                        val colorRes = when (index) {
                            0 -> R.color.blue
                            1 -> R.color.green
                            else -> R.color.orange
                        }
                        setColorFilter(ContextCompat.getColor(context, colorRes))
                    }

                    val titleText = TextView(context).apply {
                        text = title
                        textSize = 16f
                        setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    }

                    itemLayout.addView(icon)
                    itemLayout.addView(titleText)
                    documentsLayout.addView(itemLayout)

                    itemLayout.setOnClickListener {
                        val intent = Intent(context, DocumentDetailActivity::class.java)
                        intent.putExtra("documentId", docId)
                        startActivity(intent)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to fetch documents", e)
            }
    }

    private fun fetchTotalProfileViews() {
        firestore.collection("documents")
            .whereEqualTo("uploadedByUid", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var totalViews = 0L
                for (doc in querySnapshot.documents) {
                    val views = doc.getLong("views") ?: 0L
                    totalViews += views
                }
                profileViewsText.text = totalViews.toString()
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Failed to fetch profile views", e)
            }
    }

    private fun fetchUploadedDocumentsCount() {
        val activeProjectsCount: TextView = findViewById(R.id.activeProjectsCount)

        firestore.collection("documents")
            .whereEqualTo("uploadedByUid", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val count = querySnapshot.size()
                activeProjectsCount.text = count.toString()
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Failed to fetch uploaded documents", e)
                activeProjectsCount.text = "0"
            }
    }

    private fun fetchOngoingRequestsCount() {
        val pendingProjectsCount: TextView = findViewById(R.id.pendingProjectsCount)
        val userId = currentUserId ?: return

        firestore.collection("requests")
            .whereNotEqualTo("status", "Done")  // Filter out completed ones
            .get()
            .addOnSuccessListener { querySnapshot ->
                val ongoingRequests = querySnapshot.documents.filter { doc ->
                    val assignedTo = doc.getString("assignedToUid")
                    val requestedBy = doc.getString("requestedByUid")
                    assignedTo == userId || requestedBy == userId
                }
                pendingProjectsCount.text = ongoingRequests.size.toString()
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Failed to fetch ongoing requests", e)
                pendingProjectsCount.text = "0"
            }
    }

    private fun fetchTotalPriceForRequests() {
        val amountText: TextView = findViewById(R.id.amountText)
        val labelText: TextView = findViewById(R.id.amountLabel) // Add this if you want to set "Total Spent" or "Earned"
        val userId = currentUserId ?: return

        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val role = userDoc.getString("role") ?: return@addOnSuccessListener
                val isStudent = role.equals("student", ignoreCase = true)

                firestore.collection("requests")
                    .whereNotEqualTo("status", "Done") // Optional: only include active
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        var total = 0L // <-- Add the 'L' to make it a Long

                        for (doc in querySnapshot.documents) {
                            val price = doc.getLong("price") ?: 0
                            val requestedBy = doc.getString("requestedByUid")
                            val assignedTo = doc.getString("assignedToUid")

                            if ((isStudent && requestedBy == userId) || (!isStudent && assignedTo == userId)) {
                                total += price
                            }
                        }

                        // Update label
                        labelText.text = if (isStudent) "Total Spent" else "Total Earned"
                        amountText.text = "R$total"
                    }
                    .addOnFailureListener {
                        Log.e("Dashboard", "Failed to calculate total", it)
                        amountText.text = "R0"
                    }
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
