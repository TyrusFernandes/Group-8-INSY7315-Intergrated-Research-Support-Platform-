package com.example.acadenceapp.models

data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L // For sorting and display
)