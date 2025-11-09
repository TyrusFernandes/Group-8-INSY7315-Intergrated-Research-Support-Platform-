package com.example.acadenceapp.models

data class SupportTicket(
    val ticketId: String = "",
    val status: String = "pending",
    val studentId: String = "",
    val studentName: String? = null,   // 👈 add this
    val initialMessage: String = "",
    val timestamp: com.google.firebase.Timestamp? = null

)
