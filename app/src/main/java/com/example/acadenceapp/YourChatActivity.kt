package com.example.acadenceapp

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.models.Message
import com.example.acadenceapp.adapters.ChatAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.util.*

class YourChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var chatPartnerName: TextView
    private lateinit var btnBack: ImageButton

    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<Message>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    private var targetUserId: String = ""
    private var targetUsername: String = "Unknown User"
    private lateinit var chatRoomId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_your_chat)

        // 1. Get user data passed from MessagesActivity
        targetUserId = intent.getStringExtra("TARGET_USER_ID") ?: run {
            Toast.makeText(this, "Error: Target User ID missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        targetUsername = intent.getStringExtra("TARGET_USERNAME") ?: "Unknown User"

        // 2. Determine the unique chat room ID
        chatRoomId = getChatRoomId(currentUserId, targetUserId)

        // 3. Initialize Views
        chatPartnerName = findViewById(R.id.chatPartnerName)
        editTextMessage = findViewById(R.id.editTextMessage)
        btnSend = findViewById(R.id.btnSend)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        btnBack = findViewById(R.id.btnBack)

        // Set the header name
        chatPartnerName.text = targetUsername

        // Setup back button
        btnBack.setOnClickListener { finish() }

        // 4. Setup RecyclerView and Listener
        setupRecyclerView()
        listenForMessages()

        // 5. Setup Send Button
        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messageList)
        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@YourChatActivity)
            adapter = chatAdapter
        }
    }

    /**
     * Creates a deterministic, unique ID for the chat room regardless of user order.
     */
    private fun getChatRoomId(user1: String, user2: String): String {
        return if (user1 < user2) {
            user1 + "_" + user2
        } else {
            user2 + "_" + user1
        }
    }

    // --- Firebase Logic: Sending and Receiving ---

    private fun listenForMessages() {
        db.collection("chats").document(chatRoomId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    messageList.clear()
                    for (doc in snapshot.documents) {
                        val message = doc.toObject(Message::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                    // Scroll to the latest message
                    chatRecyclerView.scrollToPosition(messageList.size - 1)
                }
            }
    }

    private fun sendMessage() {
        val messageText = editTextMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        val timestamp = System.currentTimeMillis()

        // 1. Create the message object
        val newMessage = Message(
            text = messageText,
            senderId = currentUserId,
            timestamp = timestamp
        )

        // 2. Add message to the chat room
        db.collection("chats").document(chatRoomId).collection("messages")
            .add(newMessage)
            .addOnSuccessListener { docRef ->
                // Message ID added after creation
                docRef.update("id", docRef.id)

                // 3. Update recent chats for BOTH users
                updateRecentChats(messageText, timestamp)

                // 4. Clear input field
                editTextMessage.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show()
                Log.e("ChatActivity", "Error sending message", e)
            }
    }

    /**
     * Updates the 'recentChats' subcollection for both the sender and the receiver.
     */
    private fun updateRecentChats(lastMessage: String, timestamp: Long) {
        // Prepare the common data for both recent chat documents
        val chatData = mapOf(
            "chatId" to chatRoomId,
            "lastMessage" to lastMessage,
            "timestamp" to timestamp
        )

        // 1. Update Sender's Recent Chat (shows targetUsername)
        val senderRecentChat = chatData + mapOf(
            "otherUserId" to targetUserId,
            "otherUsername" to targetUsername // The name we display for this chat is the TARGET's name
        )
        db.collection("users").document(currentUserId).collection("recentChats").document(targetUserId)
            .set(senderRecentChat, SetOptions.merge())
            .addOnFailureListener { Log.e("ChatActivity", "Failed to update sender's recent chat", it) }


        // 2. Update Receiver's Recent Chat (shows sender's username)
        // We need the current user's username to show to the target user
        getCurrentUserUsername { myUsername ->
            val receiverRecentChat = chatData + mapOf(
                "otherUserId" to currentUserId,
                "otherUsername" to myUsername // The name the TARGET sees is the SENDER's name (myUsername)
            )
            db.collection("users").document(targetUserId).collection("recentChats").document(currentUserId)
                .set(receiverRecentChat, SetOptions.merge())
                .addOnFailureListener { Log.e("ChatActivity", "Failed to update receiver's recent chat", it) }
        }
    }

    /**
     * Fetches the current user's username from the 'users' collection.
     */
    private fun getCurrentUserUsername(callback: (String) -> Unit) {
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                // Note: using 'username' based on the image you provided
                val username = document.getString("username") ?: "You"
                callback(username)
            }
            .addOnFailureListener {
                Log.e("ChatActivity", "Failed to fetch current user's username.", it)
                callback("You")
            }
    }
}