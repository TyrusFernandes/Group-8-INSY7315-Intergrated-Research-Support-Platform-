package com.example.acadenceapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.adapters.ResourceAdapter
import com.example.acadenceapp.models.ResourceItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore


class ResourceLibraryActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ResourceAdapter
    private val resources = mutableListOf<ResourceItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resource_library)

        recycler = findViewById(R.id.resourceRecycler)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter = ResourceAdapter(resources) { openFile(it.url) }
        recycler.adapter = adapter

        loadResources()
    }

    private fun loadResources() {
        FirebaseFirestore.getInstance()
            .collection("resources")
            .get()
            .addOnSuccessListener { snap ->
                resources.clear()

                for (doc in snap.documents) {
                    val data = doc.data ?: continue

                    resources.add(
                        ResourceItem(
                            id = doc.id,
                            title = data["title"]?.toString() ?: "",
                            originalName = data["originalName"]?.toString() ?: "",
                            url = data["url"]?.toString() ?: "",
                            uploadedBy = data["uploadedBy"]?.toString() ?: "",
                            uploadedAt = (data["uploadedAt"] as? Timestamp)?.toDate()?.time ?: 0L
                        )
                    )
                }

                adapter.notifyDataSetChanged()
            }
    }

    private fun openFile(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }
}
