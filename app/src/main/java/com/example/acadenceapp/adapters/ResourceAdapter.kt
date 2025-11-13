package com.example.acadenceapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.R
import com.example.acadenceapp.models.ResourceItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResourceAdapter(
    private val resources: List<ResourceItem>,
    private val onClick: (ResourceItem) -> Unit
) : RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder>() {

    inner class ResourceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.resourceTitle)
        val uploader = itemView.findViewById<TextView>(R.id.resourceUploader)
        val date = itemView.findViewById<TextView>(R.id.resourceDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resource, parent, false)
        return ResourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        val item = resources[position]

        holder.title.text = item.title
        holder.uploader.text = "Uploaded by: ${item.uploadedBy}"

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.date.text = "Uploaded: ${dateFormat.format(Date(item.uploadedAt))}"

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = resources.size
}
