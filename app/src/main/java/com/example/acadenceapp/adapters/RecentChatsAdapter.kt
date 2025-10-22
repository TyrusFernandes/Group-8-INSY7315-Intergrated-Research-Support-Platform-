package com.example.acadenceapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.R
import com.example.acadenceapp.models.ChatModel
import java.text.SimpleDateFormat
import java.util.*

class RecentChatsAdapter(
    private val chats: List<ChatModel>,
    private val clickListener: (ChatModel) -> Unit // Lambda function for click handling
) : RecyclerView.Adapter<RecentChatsAdapter.ChatViewHolder>() {

    // Helper to format the timestamp
    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatCard: CardView = itemView.findViewById(R.id.chatCard)
        val chatName: TextView = itemView.findViewById(R.id.textViewChatName)
        val lastMessage: TextView = itemView.findViewById(R.id.textViewLastMessage)
        val timestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        holder.chatName.text = chat.otherUsername
        holder.lastMessage.text = chat.lastMessage

        // Convert timestamp to a readable format
        val date = Date(chat.timestamp)
        holder.timestamp.text = dateFormat.format(date)

        // Handle click to open the chat page
        holder.chatCard.setOnClickListener {
            clickListener(chat)
        }
    }

    override fun getItemCount() = chats.size
}