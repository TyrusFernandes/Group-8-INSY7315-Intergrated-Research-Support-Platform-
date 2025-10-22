package com.example.acadenceapp.models

import com.google.firebase.Timestamp

// Updated model to match web app structure
data class NotificationModel(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val priority: String = "Low",
    val timestamp: Timestamp = Timestamp.now(),
    val readBy: List<String> = emptyList(),
    val recipientIds: List<String> = emptyList(),
    val sentBy: String = ""
)
