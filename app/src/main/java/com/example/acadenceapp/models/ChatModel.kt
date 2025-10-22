package com.example.acadenceapp.models

data class ChatModel(
    val chatId: String = "",
    val otherUserId: String = "",
    val otherUsername: String = "Unknown User",
    val lastMessage: String = "No messages yet",
    val timestamp: Long = 0L // Unix timestamp for sorting and display
)