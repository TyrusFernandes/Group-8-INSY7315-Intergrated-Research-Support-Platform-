package com.example.acadenceapp.models

import com.google.firebase.Timestamp

data class AiFeedbackModel(
    val summary: String = "",
    val reviewedBy: String = "AI",
    val createdAt: Timestamp? = null
)
