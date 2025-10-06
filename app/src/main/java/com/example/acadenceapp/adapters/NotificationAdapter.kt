package com.example.acadenceapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.models.NotificationModel

class NotificationAdapter(private val notifications: MutableList<NotificationModel>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        // Using a built-in simple layout (android.R.layout.simple_list_item_1)
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = notifications[position]
        // Prepend a bullet point and format the message
        holder.messageTextView.text = "• ${item.message}"
        holder.messageTextView.setTextColor(holder.itemView.context.resources.getColor(android.R.color.black, null))
    }

    override fun getItemCount() = notifications.size

    // Method to update the list
    fun updateData(newNotifications: List<NotificationModel>) {
        notifications.clear()
        notifications.addAll(newNotifications.sortedByDescending { it.timestamp }) // Display newest first
        notifyDataSetChanged()
    }
}