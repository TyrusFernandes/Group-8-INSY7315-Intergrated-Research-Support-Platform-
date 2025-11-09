package com.example.acadenceapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.adapters.TicketAdapter
import com.example.acadenceapp.models.SupportTicket
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SupportTicketsActivity : AppCompatActivity() {
    private lateinit var ticketsAdapter: TicketAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var studentId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support_tickets)

        db = FirebaseFirestore.getInstance() // ✅ ADD THIS LINE

        val recycler = findViewById<RecyclerView>(R.id.recyclerTickets)
        val newTicketBtn = findViewById<Button>(R.id.btnNewTicket)

        ticketsAdapter = TicketAdapter(mutableListOf()) { ticket ->
            // Handle ticket click
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = ticketsAdapter

        newTicketBtn.setOnClickListener { showNewTicketDialog() }

        loadTickets() // ⬅️ This now runs safely
    }


    private fun loadTickets() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val studentId = user.uid

        db.collection("supportTickets")
            .whereEqualTo("studentId", studentId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SupportTickets", "Error loading tickets", e)
                    return@addSnapshotListener
                }
                val tickets = snapshot?.toObjects(SupportTicket::class.java) ?: listOf()
                ticketsAdapter.updateTickets(tickets)
            }
    }


    private fun showNewTicketDialog() {
        val input = EditText(this)
        input.hint = "Describe your issue..."

        AlertDialog.Builder(this)
            .setTitle("Submit Ticket")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val message = input.text.toString().trim()
                if (message.isNotEmpty()) {

                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val studentId = currentUser?.uid ?: return@setPositiveButton
                    val studentName = currentUser?.displayName ?: "Anonymous"

                    // 🔹 Generate a new document reference first
                    val newDocRef = db.collection("supportTickets").document()

                    val ticket = hashMapOf(
                        "studentId" to studentId,
                        "studentName" to studentName,
                        "initialMessage" to message,
                        "timestamp" to com.google.firebase.Timestamp.now(),
                        "status" to "pending"
                    )

                    // 🔹 Use set() instead of add()
                    newDocRef.set(ticket)
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Ticket submitted!", Toast.LENGTH_SHORT).show()
                            loadTickets()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "❌ Failed to submit ticket", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun toggleChatForTicket(ticket: SupportTicket) {
        // TODO: Implement a way to show/hide the chat container under each ticket.
        // You’ll link a layout below each ticket and toggle its visibility.
        Toast.makeText(this, "Clicked on ticket: ${ticket.ticketId}", Toast.LENGTH_SHORT).show()
    }

}
