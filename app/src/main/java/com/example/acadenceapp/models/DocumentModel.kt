package com.example.acadenceapp.models

import com.google.firebase.Timestamp

data class DocumentModel(
    val id: String = "",  // <--- ADD THIS
    val title: String = "",
    val fileUrl: String = "",
    val uploadedBy: String? = null,
    val uploadedByUid: String? = null,
    val createdAt: Timestamp? = null,
    val tags: List<String> = emptyList(),     // NEW
    val titleLower: String = "",               // NEW
    val thumbnailUrl: String? = null
)
