package com.example.acadenceapp

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

object OfflineSyncManager {

    fun syncPendingDocuments(context: Context) {
        if (!NetworkUtils.isOnline(context)) return

        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance().reference
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("documents")
            .whereEqualTo("uploadedByUid", uid)
            .whereEqualTo("uploadPending", true)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val localFileUri = doc.getString("localFileUri")
                    if (localFileUri.isNullOrEmpty()) continue

                    val localThumbUri = doc.getString("localThumbnailUri")
                    val docRef = doc.reference

                    val fileRef = storage.child("documents/${UUID.randomUUID()}")
                    val fileUri = Uri.parse(localFileUri)

                    fileRef.putFile(fileUri)
                        .continueWithTask { t ->
                            if (!t.isSuccessful) throw t.exception ?: Exception("Upload failed")
                            fileRef.downloadUrl
                        }
                        .addOnSuccessListener { fileUrl ->
                            if (!localThumbUri.isNullOrEmpty()) {
                                val thumbRef = storage.child("thumbnails/${UUID.randomUUID()}")
                                val thumbUri = Uri.parse(localThumbUri)

                                thumbRef.putFile(thumbUri)
                                    .continueWithTask { t ->
                                        if (!t.isSuccessful) throw t.exception ?: Exception("Thumb upload failed")
                                        thumbRef.downloadUrl
                                    }
                                    .addOnSuccessListener { thumbUrl ->
                                        updateFirestoreAfterSync(
                                            docRef,
                                            fileUrl.toString(),
                                            thumbUrl.toString()
                                        )
                                    }
                            } else {
                                updateFirestoreAfterSync(
                                    docRef,
                                    fileUrl.toString(),
                                    ""
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("OfflineSync", "Failed to upload pending doc ${doc.id}", e)
                        }
                }
            }
    }

    private fun updateFirestoreAfterSync(
        docRef: com.google.firebase.firestore.DocumentReference,
        fileUrl: String,
        thumbUrl: String
    ) {
        docRef.update(
            mapOf(
                "fileUrl" to fileUrl,
                "thumbnailUrl" to thumbUrl,
                "uploadPending" to false,
                "localFileUri" to FieldValue.delete(),
                "localThumbnailUri" to FieldValue.delete()
            )
        ).addOnSuccessListener {
            Log.d("OfflineSync", "Synced doc ${docRef.id}")
        }.addOnFailureListener {
            Log.e("OfflineSync", "Failed to update doc after sync", it)
        }
    }
}
