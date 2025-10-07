package com.example.acadenceapp.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.R
import com.example.acadenceapp.models.DocumentModel
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class ForYouDocAdapter(
    private var items: MutableList<DocumentModel>
) : RecyclerView.Adapter<ForYouDocAdapter.VH>() {

    private val df = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card: MaterialCardView = v.findViewById(R.id.docCard)
        val title: TextView       = v.findViewById(R.id.docTitle)
        val meta: TextView        = v.findViewById(R.id.docMeta)
        val tags: TextView        = v.findViewById(R.id.docTags)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doc_card, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]

        holder.title.text = it.title

        val who  = it.uploadedBy ?: "Unknown"
        val whenTxt = it.createdAt?.toDate()?.let(df::format) ?: ""
        holder.meta.text = if (whenTxt.isNotEmpty()) "By: $who • $whenTxt" else "By: $who"

        // show tags if present (e.g., "ai, nlp, research")
        val tagLine = it.tags?.takeIf { t -> t.isNotEmpty() }?.joinToString(", ")
        holder.tags.text = tagLine ?: ""

        holder.card.setOnClickListener { v ->
            val url = it.fileUrl
            if (!url.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                v.context.startActivity(intent)
            }
        }
    }

    fun update(newItems: List<DocumentModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
