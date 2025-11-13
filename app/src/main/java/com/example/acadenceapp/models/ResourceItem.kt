package com.example.acadenceapp.models

data class ResourceItem(
    val id: String = "",
    val title: String = "",
    val originalName: String = "",
    val url: String = "",
    val uploadedAt: Long = 0L,
    val uploadedBy: String = ""
)
