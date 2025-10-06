package com.example.acadenceapp.models

import com.google.firebase.Timestamp

data class NotificationModel(
    val id: String = "",
    val message: String = "",
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)