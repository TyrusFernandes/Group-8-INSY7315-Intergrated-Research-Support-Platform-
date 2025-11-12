package com.example.acadenceapp.utils

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.ai
import kotlinx.coroutines.tasks.await

object AiFeedbackHelper {

    // Create a reusable generative model instance with configuration
    private fun generativeModel(): GenerativeModel {
        val config = generationConfig {
            maxOutputTokens = 400
            temperature = 0.7f
            topK = 16
            topP = 0.1f
        }

        return Firebase.ai
            .generativeModel(
                modelName = "gemini-2.5-flash",
                generationConfig = config
            )
    }

    // Suspend function to generate feedback using the AI model
    suspend fun generateAiFeedback(targetId: String, content: String, isDocument: Boolean = false) {
        val model = generativeModel()

        var prompt = """
            You are an academic writing assistant.
            Review the following academic text for grammar, clarity, coherence, and factual accuracy.
            Provide a concise summary of issues and recommended improvements.
            
            Text:
            $content
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val textResult = response.text.orEmpty()

            val feedback = hashMapOf(
                "summary" to textResult,
                "reviewedBy" to "AI",
                "createdAt" to Timestamp.now()
            )

            val db = FirebaseFirestore.getInstance()
            val collection = if (isDocument) "documents" else "requests"

            db.collection(collection)
                .document(targetId)
                .collection("ai_feedback")
                .add(feedback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
