package com.example.acadenceapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.R
import com.example.acadenceapp.models.CommentModel
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(private var comments: List<CommentModel>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.comment_username)
        val commentText: TextView = itemView.findViewById(R.id.comment_text)
        val timestamp: TextView = itemView.findViewById(R.id.comment_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.username.text = comment.username
        holder.commentText.text = comment.text
        holder.timestamp.text = formatter.format(comment.timestamp.toDate())
    }

    override fun getItemCount(): Int = comments.size

    fun updateData(newComments: List<CommentModel>) {
        comments = newComments
        notifyDataSetChanged()
    }
}
