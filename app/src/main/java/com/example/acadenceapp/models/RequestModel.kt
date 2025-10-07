package com.example.acadenceapp.models

import com.google.firebase.Timestamp

data class RequestModel(
    val id: String = "",
    val docTitle: String = "",
    val assignedToName: String = "",
    val reviewType: String = "",
    val dueDate: com.google.firebase.Timestamp? = null,
    val createdAt: com.google.firebase.Timestamp? = null,
    val status: String = "Pending"
)
