package com.example.acadenceapp

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class AcadenceApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)   // 🔹 keep local cache for offline use
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}
