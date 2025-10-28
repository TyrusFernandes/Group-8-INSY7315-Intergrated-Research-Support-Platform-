package com.example.acadenceapp.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.R
import com.example.acadenceapp.ConsultantRequestDetailActivity
import com.example.acadenceapp.models.RequestModel
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class RequestCardAdapter(
    private var items: MutableList<RequestModel>
) : RecyclerView.Adapter<RequestCardAdapter.VH>() {

    private val df = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card: MaterialCardView = v.findViewById(R.id.reqCard)
        val title: TextView = v.findViewById(R.id.reqTitle)
        val assigned: TextView = v.findViewById(R.id.reqAssigned)
        val type: TextView = v.findViewById(R.id.reqType)
        val dates: TextView = v.findViewById(R.id.reqDates)
        val status: TextView = v.findViewById(R.id.reqStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request_card, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.title.text = item.docTitle
        holder.assigned.text = "Assigned: ${item.assignedToName}"
        holder.type.text = "Type: ${item.reviewType}"

        val due = item.dueDate?.toDate()?.let(df::format) ?: "—"
        val createdText = try {
            val createdAtField = item::class.java.getDeclaredField("createdAt")
            createdAtField.isAccessible = true
            val ts = createdAtField.get(item) as? com.google.firebase.Timestamp
            ts?.toDate()?.let(df::format)
        } catch (_: Throwable) { null }

        holder.dates.text =
            if (!createdText.isNullOrEmpty()) "Due: $due   •   Created: $createdText"
            else "Due: $due"

        // Status + color
        holder.status.text = item.status
        val color = when (item.status.lowercase(Locale.getDefault())) {
            "approved", "done", "completed" -> 0xFF2FD732.toInt()
            "rejected" -> 0xFFCC2828.toInt()
            "in progress", "reviewing" -> 0xFF6A1B9A.toInt()
            else -> 0xFF0066FF.toInt()
        }
        holder.status.setTextColor(color)

        holder.card.strokeWidth = 2
        holder.card.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.black)

        // ✅ Make card clickable
        holder.card.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ConsultantRequestDetailActivity::class.java)
            intent.putExtra("requestId", item.id)
            context.startActivity(intent)
        }
    }

    fun update(newItems: List<RequestModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
