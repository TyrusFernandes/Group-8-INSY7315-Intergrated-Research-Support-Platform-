package com.example.acadenceapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.R
import com.example.acadenceapp.models.DocumentModel
import java.text.SimpleDateFormat
import java.util.*

class DocumentAdapter(
    private val ctx: Context,
    private val items: MutableList<DocumentModel>
) : RecyclerView.Adapter<DocumentAdapter.VH>() {

    private val df = SimpleDateFormat("EEE dd MMM yyyy HH:mm", Locale.getDefault())

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.titleText)
        val subtitle: TextView = v.findViewById(R.id.subtitleText)
        val date: TextView = v.findViewById(R.id.dateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(ctx).inflate(R.layout.item_document, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val doc = items[position]
        h.title.text = doc.title
        h.subtitle.text = "By: " + (doc.uploadedBy ?: "Unknown")
        val d = doc.createdAt?.toDate() ?: Date()
        h.date.text = df.format(d)
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newData: List<DocumentModel>) {
        items.clear()
        items.addAll(newData)
        notifyDataSetChanged()
    }
}
