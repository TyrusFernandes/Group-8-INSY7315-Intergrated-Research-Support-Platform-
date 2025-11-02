package com.example.acadenceapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.adapters.RequestCardAdapter
import com.example.acadenceapp.models.RequestModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConsultantRequestsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val lang = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "English"
        val context = newBase?.let { LocaleHelper.setLocale(it, lang) }
        super.attachBaseContext(context)
    }


    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RequestCardAdapter
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar

    private val requestList = mutableListOf<RequestModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultant_requests)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        recyclerView = findViewById(R.id.consultantRequestsRecycler)
        emptyText = findViewById(R.id.emptyRequestsText)
        progressBar = findViewById(R.id.requestsProgressBar)

        adapter = RequestCardAdapter(requestList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadAssignedRequests()
    }

    private fun loadAssignedRequests() {
        val uid = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE

        firestore.collection("requests")
            .whereEqualTo("assignedToUid", uid)
            .addSnapshotListener { snapshot, e ->
                progressBar.visibility = View.GONE
                if (e != null) {
                    Log.e("ConsultantRequests", "Error loading requests", e)
                    Toast.makeText(this, "Error loading requests", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    return@addSnapshotListener
                }

                emptyText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                val sortedDocs = snapshot.documents.sortedByDescending {
                    it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                }

                val requests = sortedDocs.map { d ->
                    RequestModel(
                        id = d.id,
                        docTitle = d.getString("docTitle") ?: "",
                        assignedToName = d.getString("assignedToName") ?: "",
                        reviewType = d.getString("reviewType") ?: "",
                        dueDate = d.getTimestamp("dueDate") ?: Timestamp.now(),
                        status = d.getString("status") ?: "Pending"
                    )
                }

                requestList.clear()
                requestList.addAll(requests)
                adapter.notifyDataSetChanged()
            }
    }
}
