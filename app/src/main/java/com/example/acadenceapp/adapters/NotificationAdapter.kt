package com.example.acadenceapp.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.R
import com.example.acadenceapp.models.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationAdapter(private val notifications: MutableList<NotificationModel>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.tvNotificationTitle)
        val messageText: TextView = view.findViewById(R.id.tvNotificationMessage)
        val timestampText: TextView = view.findViewById(R.id.tvNotificationTimestamp)
        val priorityIndicator: View = view.findViewById(R.id.priorityIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = notifications[position]
        holder.titleText.text = item.title
        holder.messageText.text = item.message
        holder.timestampText.text = item.timestamp.toDate().toString()

        // Set priority color
        val colorRes = if (item.priority.equals("High", ignoreCase = true))
            android.R.color.holo_red_light
        else
            android.R.color.darker_gray
        holder.priorityIndicator.setBackgroundResource(colorRes)

        // Dim if read
        val isUnread = !(item.readBy.contains(currentUserId))
        holder.itemView.alpha = if (isUnread) 1.0f else 0.5f

        // Open centered dialog
        holder.itemView.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle(item.title)
                .setMessage(item.message)
                .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                .show()

            // Mark as read if not already
            if (isUnread && currentUserId != null) {
                val updatedReadBy = item.readBy.toMutableList().apply { add(currentUserId) }
                firestore.collection("notifications")
                    .document(item.id)
                    .update("readBy", updatedReadBy)
            }
        }
    }

    override fun getItemCount() = notifications.size

    fun updateData(newNotifications: List<NotificationModel>) {
        notifications.clear()
        notifications.addAll(newNotifications.sortedByDescending { it.timestamp })
        notifyDataSetChanged()
    }
}
