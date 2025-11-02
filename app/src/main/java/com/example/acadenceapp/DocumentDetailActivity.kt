package com.example.acadenceapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.adapters.CommentAdapter
import com.example.acadenceapp.models.CommentModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.widget.Toolbar

class DocumentDetailActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val lang = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "English"
        val context = newBase?.let { LocaleHelper.setLocale(it, lang) }
        super.attachBaseContext(context)
    }

    private lateinit var title: TextView
    private lateinit var meta: TextView
    private lateinit var tags: TextView
    private lateinit var openDocBtn: Button
    private lateinit var likeBtn: ImageButton
    private lateinit var dislikeBtn: ImageButton
    private lateinit var likeCount: TextView
    private lateinit var dislikeCount: TextView
    private lateinit var commentInput: EditText
    private lateinit var sendCommentBtn: Button
    private lateinit var commentsList: RecyclerView

    private lateinit var adapter: CommentAdapter
    private val comments = mutableListOf<CommentModel>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var documentId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_detail)

        // Initialize UI elements
        title = findViewById(R.id.detailTitle)
        meta = findViewById(R.id.detailMeta)
        tags = findViewById(R.id.detailTags)
        openDocBtn = findViewById(R.id.openDocumentButton)
        likeBtn = findViewById(R.id.likeButton)
        dislikeBtn = findViewById(R.id.dislikeButton)
        likeCount = findViewById(R.id.likeCount)
        dislikeCount = findViewById(R.id.dislikeCount)
        commentInput = findViewById(R.id.commentInput)
        sendCommentBtn = findViewById(R.id.sendCommentBtn)
        commentsList = findViewById(R.id.commentsRecycler)

        documentId = intent.getStringExtra("documentId")
            ?: intent.getStringExtra("docId")
                    ?: run {
                Toast.makeText(this, "Missing document ID", Toast.LENGTH_LONG).show()
                Log.e("DocumentDetailActivity", "docId is null or empty!")
                finish()
                return
            }

        Log.d("DocumentDetailActivity", "Resolved documentId: $documentId")

        val toolbar = findViewById<Toolbar>(R.id.globalToolbar)
        toolbar.title = "Document Detail" // or dynamic title
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)


        title.text = intent.getStringExtra("title")
        meta.text = intent.getStringExtra("meta")
        tags.text = intent.getStringExtra("tags")

        // Open document in browser
        openDocBtn.setOnClickListener {
            val url = intent.getStringExtra("url")
            url?.let { u -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) }
        }

        likeBtn.setOnClickListener { toggleReaction(isLike = true) }
        dislikeBtn.setOnClickListener { toggleReaction(isLike = false) }
        sendCommentBtn.setOnClickListener { sendComment() }

        setupCommentsRecycler()
        loadComments()
        loadReactions()
        incrementViewCount()
    }

    // === Load Like/Dislike Counts ===
    private fun loadReactions() {
        val docRef = db.collection("documents").document(documentId)
        docRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val likes = snapshot.get("likes") as? List<String> ?: emptyList()
                val dislikes = snapshot.get("dislikes") as? List<String> ?: emptyList()
                updateLikeUI(likes, dislikes)
            }
        }
        val viewCountText: TextView = findViewById(R.id.viewCount)
        docRef.get().addOnSuccessListener { snapshot ->
            val views = snapshot.getLong("views") ?: 0
            viewCountText.text = "$views views"
        }
    }

    // === Toggle Like/Dislike ===
    private fun toggleReaction(isLike: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("documents").document(documentId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val likes = snapshot.get("likes") as? MutableList<String> ?: mutableListOf()
            val dislikes = snapshot.get("dislikes") as? MutableList<String> ?: mutableListOf()

            if (isLike) {
                if (likes.contains(uid)) {
                    likes.remove(uid)
                } else {
                    likes.add(uid)
                    dislikes.remove(uid)
                }
            } else {
                if (dislikes.contains(uid)) {
                    dislikes.remove(uid)
                } else {
                    dislikes.add(uid)
                    likes.remove(uid)
                }
            }

            transaction.update(docRef, mapOf(
                "likes" to likes,
                "dislikes" to dislikes
            ))

            return@runTransaction Pair(likes, dislikes)
        }.addOnSuccessListener { (likes, dislikes) ->
            updateLikeUI(likes, dislikes)
        }.addOnFailureListener {
            Log.e("DocumentDetailActivity", "Failed to update reactions", it)
        }
    }

    // === Update UI ===
    private fun updateLikeUI(likes: List<String>, dislikes: List<String>) {
        val uid = auth.currentUser?.uid ?: return

        likeCount.text = likes.size.toString()
        dislikeCount.text = dislikes.size.toString()

        if (likes.contains(uid)) {
            likeBtn.setColorFilter(resources.getColor(R.color.blue))
        } else {
            likeBtn.setColorFilter(resources.getColor(R.color.gray))
        }

        if (dislikes.contains(uid)) {
            dislikeBtn.setColorFilter(resources.getColor(R.color.red))
        } else {
            dislikeBtn.setColorFilter(resources.getColor(R.color.gray))
        }
    }

    // === Comments ===
    private fun sendComment() {
        val text = commentInput.text.toString().trim()
        val uid = auth.currentUser?.uid ?: return
        if (text.isEmpty()) return

        val userRef = db.collection("users").document(uid)
        userRef.get().addOnSuccessListener { userSnap ->
            val username = userSnap.getString("username") ?: "Anonymous"

            val comment = hashMapOf(
                "text" to text,
                "userId" to uid,
                "username" to username,
                "timestamp" to Timestamp.now()
            )

            db.collection("documents")
                .document(documentId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener {
                    commentInput.setText("")
                    loadComments()
                }
        }
    }

    private fun setupCommentsRecycler() {
        adapter = CommentAdapter(comments)
        commentsList.layoutManager = LinearLayoutManager(this)
        commentsList.adapter = adapter
    }

    private fun loadComments() {
        db.collection("documents").document(documentId)
            .collection("comments")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { snap ->
                comments.clear()
                comments.addAll(snap.documents.mapNotNull { d ->
                    val data = d.data ?: return@mapNotNull null
                    CommentModel(
                        username = data["username"] as? String ?: "",
                        text = data["text"] as? String ?: "",
                        timestamp = data["timestamp"] as? Timestamp ?: Timestamp.now()
                    )
                })
                adapter.notifyDataSetChanged()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }


    private fun incrementViewCount() {
        val docRef = db.collection("documents").document(documentId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentViews = snapshot.getLong("views") ?: 0
            transaction.update(docRef, "views", currentViews + 1)
        }.addOnSuccessListener {
            Log.d("DocumentDetailActivity", "View count incremented.")
        }.addOnFailureListener { e ->
            Log.e("DocumentDetailActivity", "Failed to increment view count", e)
        }
    }

}
