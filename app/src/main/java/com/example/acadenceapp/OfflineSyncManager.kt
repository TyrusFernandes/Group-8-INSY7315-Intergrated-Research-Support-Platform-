package com.example.acadenceapp

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object OfflineSyncManager {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    fun syncPendingDocuments(context: Context) {
        if (!NetworkUtils.isOnline(context)) return

        val user = auth.currentUser ?: return

        db.collection("documents")
            .whereEqualTo("uploadedByUid", user.uid)
            .whereEqualTo("uploadPending", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) return@addOnSuccessListener

                Toast.makeText(
                    context,
                    "Syncing ${snapshot.size()} offline uploads…",
                    Toast.LENGTH_SHORT
                ).show()

                snapshot.documents.forEach { doc ->
                    val localUriStr = doc.getString("localUri") ?: return@forEach
                    val uri = Uri.parse(localUriStr)
                    val docId = doc.id

                    val fileRef = storage.child("documents/$docId")

                    fileRef.putFile(uri)
                        .continueWithTask { task ->
                            if (!task.isSuccessful) {
                                throw task.exception ?: Exception("Upload failed")
                            }
                            fileRef.downloadUrl
                        }
                        .addOnSuccessListener { downloadUri ->
                            doc.reference.update(
                                mapOf(
                                    "fileUrl" to downloadUri.toString(),
                                    "uploadPending" to false,
                                    "localUri" to null
                                )
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e("OfflineSync", "Failed to sync document $docId", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("OfflineSync", "Failed to load pending docs", e)
            }
    }

    // Requests are pure Firestore writes, so Firestore already syncs them for us.
    // You don't *need* extra logic here for requests.
}
