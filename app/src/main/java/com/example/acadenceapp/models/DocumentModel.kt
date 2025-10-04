package com.example.acadenceapp.models

import com.google.firebase.Timestamp

data class DocumentModel(
    val title: String = "",
    val fileUrl: String = "",
    val uploadedBy: String? = null,      // <- show this in UI
    val uploadedByUid: String? = null,   // <- used for filtering
    val createdAt: Timestamp? = null
)

