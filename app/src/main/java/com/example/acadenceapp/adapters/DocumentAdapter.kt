package com.example.acadenceapp.adapters

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.R
import com.example.acadenceapp.models.DocumentModel
import java.text.SimpleDateFormat
import java.util.*

class DocumentAdapter(
    private val context: Context,
    private var documents: List<DocumentModel>
) : RecyclerView.Adapter<DocumentAdapter.ViewHolder>() {

    private val dateFmt = SimpleDateFormat("EEE, MMM d yyyy • HH:mm", Locale.getDefault())

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.documentTitle)
        val uploader: TextView = itemView.findViewById(R.id.documentUploader)
        val date: TextView = itemView.findViewById(R.id.documentDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_document, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = documents.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = documents[position]

        holder.title.text = doc.title
        holder.uploader.text = "By: ${doc.uploadedBy ?: "Unknown"}"
        holder.date.text = doc.createdAt?.toDate()?.let { dateFmt.format(it) } ?: ""

        holder.itemView.setOnClickListener {
            val url = doc.fileUrl ?: return@setOnClickListener
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(Intent.createChooser(intent, "Open document with"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "No app found to open this file", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun updateData(newDocs: List<DocumentModel>) {
        documents = newDocs
        notifyDataSetChanged()
    }
}
