package com.example.acadenceapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.R
import com.example.acadenceapp.models.SupportTicket
import com.example.acadenceapp.models.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.util.UUID

class TicketAdapter(
    private val tickets: MutableList<SupportTicket>,
    private val onTicketClicked: (SupportTicket) -> Unit
) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    inner class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textSummary: TextView = itemView.findViewById(R.id.ticket_summary)
        val textStatus: TextView = itemView.findViewById(R.id.ticket_status)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticket = tickets[position]
        holder.textSummary.text = ticket.initialMessage

        val status = ticket.status.lowercase()
        holder.textStatus.text = when (status) {
            "resolved" -> "✅ Resolved"
            "in_progress" -> "🛠 In Progress"
            else -> "⏳ Pending"
        }
    }




    override fun getItemCount() = tickets.size

    fun updateTickets(newTickets: List<SupportTicket>) {
        tickets.clear()
        tickets.addAll(newTickets)
        notifyDataSetChanged()
    }
}
