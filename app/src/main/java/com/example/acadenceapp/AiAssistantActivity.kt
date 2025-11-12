package com.example.acadenceapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import kotlinx.coroutines.launch
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.BufferedReader
import java.io.InputStreamReader
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig

class AiAssistantActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var userInput: EditText
    private lateinit var sendButton: Button
    private lateinit var uploadButton: ImageButton
    private lateinit var model: GenerativeModel

    private var selectedFileText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_assistant)

        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)

        chatContainer = findViewById(R.id.chatContainer)
        userInput = findViewById(R.id.userInput)
        sendButton = findViewById(R.id.sendButton)
        uploadButton = findViewById(R.id.btnUploadDoc)

        val config = generationConfig {
            maxOutputTokens = 2000  // Longer responses
            temperature = 0.7f
            topK = 40
            topP = 0.9f
        }

        model = Firebase.ai.generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = config
        )

        appendMessage("AI", "Hello! I’m your Academic Assistant. You can chat or upload a document for analysis.")

        sendButton.setOnClickListener {
            val userPrompt = userInput.text.toString().trim()
            if (userPrompt.isEmpty()) return@setOnClickListener

            appendMessage("You", userPrompt)
            userInput.setText("")

            lifecycleScope.launch {
                val blockedWords = listOf(
                    "write a full essay",
                    "write an essay",
                    "write the essay",
                    "full report",
                    "complete report",
                    "write my paper",
                    "generate full document",
                    "write everything",
                    "do my homework"
                )

                val lowered = userPrompt.lowercase().trim()

// Broader blocklist of essay/report generation intents
                val forbiddenPatterns = listOf(
                    "write an essay",
                    "write a full essay",
                    "write the essay",
                    "write my essay",
                    "compose an essay",
                    "generate an essay",
                    "complete essay",
                    "full essay",
                    "full report",
                    "complete report",
                    "generate report",
                    "write a report",
                    "produce a document",
                    "draft a document",
                    "generate a document",
                    "write the whole",
                    "write the entire",
                    "do my assignment",
                    "do my homework",
                    "finish my assignment",
                    "create a full paper",
                    "write the full paper",
                    "write a 2000 word",
                    "write a 1000 word",
                    "make a research paper",
                    "generate research paper",
                    "create a research paper",
                    "complete research paper",
                    "write me an article",
                    "write a complete article",
                    "generate full text",
                    "write everything",
                    "write it for me"
                )

// Detect numeric essay-like patterns (word counts or pages)
                val wordCountRegex = Regex("""\b\d{3,5}\s*(word|page|paragraph)s?\b""")

                val looksLikeEssayRequest =
                    forbiddenPatterns.any { lowered.contains(it) } ||
                            wordCountRegex.containsMatchIn(lowered)

// ✅ Exception: allow short guided writing help (e.g. “help me write an intro”)
                val smallHelpAllowed = lowered.contains("help me write") && !looksLikeEssayRequest

                if (lowered.split(" ").size > 80 && lowered.contains("write")) {
                    appendMessage("AI", "That looks like a long essay request. I can’t generate full documents, but I can help outline or summarize it.")
                    return@launch
                }


                if (looksLikeEssayRequest && !smallHelpAllowed) {
                    appendMessage(
                        "AI",
                        "Sorry, I can’t write full essays, reports, or papers for you — but I can help you **plan, outline, or improve** your writing.\n\n" +
                                "Try asking something like:\n• “How should I structure my report on AI ethics?”\n• “Can you help me improve this paragraph?”"
                    )
                    return@launch
                }

                if (blockedWords.any { lowered.contains(it) }) {
                    appendMessage("AI", "Sorry, I can’t write full essays or documents for you, but I can help you structure or start one!")
                    return@launch
                }

                val docText = selectedFileText
                val finalPrompt = if (!docText.isNullOrEmpty()) {
                    """
        The user has uploaded a document and asked this question:
        "$userPrompt"

        Here is the text of their document (for reference only):
        $docText

        Please give concise academic guidance and avoid writing complete essays or reports.
        """.trimIndent()
                } else {
                    userPrompt
                }

                sendToAI(finalPrompt)
            }

        }


        uploadButton.setOnClickListener {
            pickDocument()
        }
    }

    private fun pickDocument() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Select a PDF Document"), 101)
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri = data.data!!
            val fileName = getFileName(uri)
            appendMessage("You", "Uploaded: $fileName\nAnalyzing document...")
            lifecycleScope.launch { analyzeDocument(uri) }
        }
    }

    private suspend fun sendToAI(prompt: String) {
        appendMessage("AI", "Thinking...")

        try {
            // Generate AI response using Gemini model
            val response = model.generateContent(prompt)
            val text = response.text ?: "No response generated."
            replaceLastMessage("AI", text)
        } catch (e: Exception) {
            replaceLastMessage("AI", "Error: ${e.message}")
        }
    }


    private fun getFileName(uri: Uri): String {
        var name = "document"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
        return name
    }

    private suspend fun analyzeDocument(uri: Uri) {
        try {
            val text = extractTextFromPdf(uri)
            selectedFileText = text.take(8000) // limit to 8K chars
            appendMessage("AI", "PDF uploaded successfully. You can now type a question about it.")
        } catch (e: Exception) {
            replaceLastMessage("AI", "Error reading PDF: ${e.message}")
        }
    }
    private fun extractTextFromPdf(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)?.buffered() ?: return "Failed to open file."
        inputStream.use { stream ->
            val document = PDDocument.load(stream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            return text
        }
    }

    private fun appendMessage(sender: String, message: String) {
        val messageView = TextView(this).apply {
            text = message
            textSize = 16f
            setPadding(24, 16, 24, 16)

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12  // <-- spacing between messages
            }


            if (sender == "You") {
                params.gravity = Gravity.END
                background = createBubble(Color.parseColor("#007AFF"), true)
                setTextColor(Color.WHITE)
            } else {
                params.gravity = Gravity.START
                background = createBubble(Color.parseColor("#E5E5EA"), false)
                setTextColor(Color.BLACK)
            }

            layoutParams = params
        }

        chatContainer.addView(messageView)
        scrollToBottom()
    }

    private fun replaceLastMessage(sender: String, newText: String) {
        if (chatContainer.childCount == 0) return
        val lastMessage = chatContainer.getChildAt(chatContainer.childCount - 1) as? TextView
        lastMessage?.text = newText
    }

    private fun scrollToBottom() {
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun createBubble(color: Int, isUser: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = 36f
        }
    }
}
