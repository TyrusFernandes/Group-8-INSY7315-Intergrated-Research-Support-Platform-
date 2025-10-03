package com.example.acadenceapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.R
import com.example.acadenceapp.models.DocumentModel

class DocumentAdapter(
    private val context: Context,
    private var docs: List<DocumentModel>
) : RecyclerView.Adapter<DocumentAdapter.DocViewHolder>() {

    class DocViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val docTitle: TextView = itemView.findViewById(R.id.docTitle)
        val docOwner: TextView = itemView.findViewById(R.id.docOwner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_document, parent, false)
        return DocViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocViewHolder, position: Int) {
        val doc = docs[position]
        holder.docTitle.text = doc.title
        holder.docOwner.text = "Uploaded by ${doc.owner}"
    }

    override fun getItemCount() = docs.size

    fun updateData(newDocs: List<DocumentModel>) {
        docs = newDocs
        notifyDataSetChanged()
    }
}
