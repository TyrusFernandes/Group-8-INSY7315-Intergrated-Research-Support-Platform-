package com.example.acadenceapp.models

import com.google.firebase.Timestamp

data class CommentModel(
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
