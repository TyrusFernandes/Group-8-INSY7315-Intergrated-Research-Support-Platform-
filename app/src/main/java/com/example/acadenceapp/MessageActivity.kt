package com.example.acadenceapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.adapters.RecentChatsAdapter
import com.example.acadenceapp.models.ChatModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.recyclerview.widget.LinearLayoutManager

class MessageActivity : AppCompatActivity() {

    private lateinit var searchBar: EditText
    private lateinit var recentChatsRecyclerView: RecyclerView
    private lateinit var chatsAdapter: RecentChatsAdapter
    private val recentChatsList = mutableListOf<ChatModel>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        searchBar = findViewById(R.id.searchBar)
        recentChatsRecyclerView = findViewById(R.id.recentChatsRecyclerView)

        setupRecyclerView()
        setupSearchBar()
        fetchRecentChats()
    }

    private fun setupRecyclerView() {
        // Initialize adapter with an empty list and a click listener
        chatsAdapter = RecentChatsAdapter(recentChatsList) { chat ->
            // Click Listener: Navigate to the chat page when a recent chat is clicked
            openChatPage(chat.otherUserId, chat.otherUsername)
        }
        recentChatsRecyclerView.layoutManager = LinearLayoutManager(this)
        recentChatsRecyclerView.adapter = chatsAdapter
    }

    private fun setupSearchBar() {
        // Handle the search action when the user presses 'Enter' or the Search button on the keyboard
        searchBar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val username = v.text.toString().trim()
                if (username.isNotEmpty()) {
                    searchUserAndStartChat(username)
                } else {
                    Toast.makeText(this, "Please enter a username to search.", Toast.LENGTH_SHORT).show()
                }
                true // Consume the event
            } else {
                false
            }
        }
    }

    // --- Firebase and Chat Logic ---

    /**
     * Searches for a user by username in Firebase Firestore.
     * If found, initiates a new chat (or opens an existing one).
     */
    private fun searchUserAndStartChat(username: String) {
        // 🎯 FIX: Collection is "users" and the field is "username"
        db.collection("users")
            .whereEqualTo("username", username) // Search the 'username' field
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDocument = documents.first()
                    val targetUserId = userDocument.id

                    // You can use 'username' or 'fullName' here for the display name.
                    // Using 'username' for consistency with the search, but 'fullName' is also available.
                    val targetUsername = userDocument.getString("username") ?: userDocument.getString("fullName") ?: "Unknown User"

                    if (targetUserId == currentUserId) {
                        Toast.makeText(this, "Can't chat with yourself.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // User found, open the chat page
                    openChatPage(targetUserId, targetUsername)
                } else {
                    Toast.makeText(this, "User '$username' not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // Log the error for better debugging
                Log.e("MessagesActivity", "User search failed", e)
                Toast.makeText(this, "Search failed: Check logs for details.", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Starts the Chat/Texting activity.
     */
    private fun openChatPage(targetUserId: String, targetUsername: String) {
        val intent = Intent(this, YourChatActivity::class.java).apply {
            // Pass the recipient's ID and Username to the chat activity
            putExtra("TARGET_USER_ID", targetUserId)
            putExtra("TARGET_USERNAME", targetUsername)
        }
        startActivity(intent)
        // Clear the search bar after starting a chat
        searchBar.text.clear()
    }

    /**
     * Fetches the list of recent chats for the current user.
     * This relies on a Firebase structure where each user has a 'recentChats' subcollection.
     */
    private fun fetchRecentChats() {
        currentUserId?.let { uid ->
            db.collection("users").document(uid).collection("recentChats")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Sort by most recent
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        // Handle error
                        Toast.makeText(this, "Error fetching chats: ${e.message}", Toast.LENGTH_LONG).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        recentChatsList.clear()
                        for (doc in snapshot.documents) {
                            // Assuming your recent chat document fields match the Chat data class
                            val chat = doc.toObject(ChatModel::class.java)
                            if (chat != null) {
                                recentChatsList.add(chat)
                            }
                        }
                        chatsAdapter.notifyDataSetChanged()
                    }
                }
        } ?: run {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_LONG).show()
        }
    }
}