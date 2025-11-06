package com.example.acadenceapp.adapters

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.acadenceapp.R
import com.example.acadenceapp.models.DocumentModel
import com.example.acadenceapp.DocumentDetailActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ForYouDocAdapter(
    private var items: MutableList<DocumentModel>
) : RecyclerView.Adapter<ForYouDocAdapter.VH>() {

    private val df = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card: MaterialCardView = v.findViewById(R.id.docCard)
        val thumbnail: ImageView = v.findViewById(R.id.thumbnailImage)
        val title: TextView = v.findViewById(R.id.titleText)
        val uploader: TextView = v.findViewById(R.id.uploaderText)
        val tagText: TextView = v.findViewById(R.id.tagText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doc_card, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val doc = items[position]

        holder.title.text = doc.title
        holder.uploader.text = "By: ${doc.uploadedBy ?: "Unknown"} • ${doc.createdAt?.toDate()?.let(df::format) ?: ""}"
        holder.tagText.text = doc.tags.joinToString(", ")

        if (!doc.thumbnailUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(doc.thumbnailUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(holder.thumbnail)
        } else {
            holder.thumbnail.setImageResource(R.drawable.placeholder_image)
        }

        holder.card.setOnClickListener { v ->
            val context = v.context
            val intent = Intent(context, DocumentDetailActivity::class.java).apply {
                putExtra("docId", doc.id)
                putExtra("title", doc.title)
                putExtra("meta", holder.uploader.text.toString())
                putExtra("tags", holder.tagText.text.toString())
                putExtra("url", doc.fileUrl)
                putExtra("thumbnailUrl", doc.thumbnailUrl ?: "")
            }
            context.startActivity(intent)
            incrementDocumentsRead()
        }
    }

    private fun incrementDocumentsRead() {
        val currentUser = auth.currentUser ?: return
        val userRef = firestore.collection("users").document(currentUser.uid)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentCount = snapshot.getLong("documentsRead") ?: 0
            transaction.update(userRef, "documentsRead", currentCount + 1)
        }.addOnSuccessListener {
            Log.d("ForYouDocAdapter", "documentsRead successfully incremented.")
        }.addOnFailureListener { e ->
            Log.e("ForYouDocAdapter", "Failed to increment documentsRead", e)
        }
    }

    fun update(newItems: List<DocumentModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}

