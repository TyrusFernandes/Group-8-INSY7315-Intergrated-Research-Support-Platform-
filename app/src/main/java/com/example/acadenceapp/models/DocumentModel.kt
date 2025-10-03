package com.example.acadenceapp.models

import java.util.*

data class DocumentModel(
    val title: String = "",
    val owner: String = "",
    val url: String = "",
    val createdAt: Date? = null
)
