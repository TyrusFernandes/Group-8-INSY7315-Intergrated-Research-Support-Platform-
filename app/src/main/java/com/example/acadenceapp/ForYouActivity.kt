package com.example.acadenceapp

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.adapters.ForYouDocAdapter
import com.example.acadenceapp.models.DocumentModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ForYouActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun attachBaseContext(newBase: Context?) {
        val lang = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "English"
        val context = newBase?.let { LocaleHelper.setLocale(it, lang) }
        super.attachBaseContext(context)
    }

    private lateinit var search: EditText
    private lateinit var chips: ChipGroup
    private lateinit var chipScroll: HorizontalScrollView
    private lateinit var list: RecyclerView
    private lateinit var progress: ProgressBar

    private lateinit var adapter: ForYouDocAdapter
    private val allCurrent = mutableListOf<DocumentModel>()   // items from current tag
    private var activeTag: String? = null                     // null => "For you" (recent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_for_you)

        search = findViewById(R.id.searchBar)
        chips = findViewById(R.id.tagChipGroup)
        chipScroll = findViewById(R.id.tagScroll)
        list = findViewById(R.id.feedRecycler)
        progress = findViewById(R.id.progressBar)

        adapter = ForYouDocAdapter(mutableListOf())
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        setupSearch()
        loadTagsAndInit()
    }

    private fun setupSearch() {
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim()?.lowercase() ?: ""
                if (q.isEmpty()) {
                    adapter.update(allCurrent)
                } else {
                    val filtered = allCurrent.filter {
                        it.title.lowercase().contains(q) ||
                                (it.uploadedBy ?: "").lowercase().contains(q) ||
                                it.tags.any { t -> t.lowercase().contains(q) }
                    }
                    adapter.update(filtered)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadTagsAndInit() {
        progress.visibility = View.VISIBLE
        // Pull a page of docs to gather distinct tags; feel free to raise the limit.
        db.collection("documents")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
            .get()
            .addOnSuccessListener { snap ->
                val distinct = linkedSetOf<String>()
                snap.documents.forEach { d ->
                    val arr = d.get("tags") as? List<*> ?: emptyList<Any>()
                    arr.forEach { any ->
                        val s = any?.toString()?.trim().orEmpty()
                        if (s.isNotEmpty()) distinct += s
                    }
                }

                buildChipBar(distinct.take(10))
                // default feed = recent (“For you”)
                activeTag = null
                loadFeedForTag(null)
            }
            .addOnFailureListener {
                progress.visibility = View.GONE
                Toast.makeText(this, "Failed to load tags: ${it.message}", Toast.LENGTH_LONG).show()
                // still load default feed
                activeTag = null
                loadFeedForTag(null)
            }
    }

    private fun buildChipBar(tags: List<String>) {
        chips.removeAllViews()

        // First chip = “For you”
        chips.addView(makeChip("For you", isChecked = true) {
            if (activeTag != null) {
                activeTag = null
                loadFeedForTag(null)
            }
        })

        tags.forEach { tag ->
            chips.addView(makeChip(tag, isChecked = false) {
                if (activeTag != tag) {
                    activeTag = tag
                    loadFeedForTag(tag)
                }
            })
        }
    }

    private fun makeChip(text: String, isChecked: Boolean, onClick: () -> Unit): Chip {
        val chip = Chip(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice)
        chip.text = text
        chip.isCheckable = true
        chip.isChecked = isChecked
        chip.isClickable = true
        chip.isFocusable = true
        chip.setOnClickListener {
            // Ensure only one chip stays selected
            chips.children.forEach { (it as? Chip)?.isChecked = false }
            chip.isChecked = true
            onClick()
        }
        chip.chipBackgroundColor = getColorStateList(R.color.chip_bg_selector)
        chip.setTextColor(getColorStateList(R.color.chip_text_selector))
        chip.setRippleColorResource(R.color.chip_ripple)
        return chip
    }

    private fun loadFeedForTag(tag: String?) {
        progress.visibility = View.VISIBLE
        var q = db.collection("documents") as Query

        q = if (tag == null) {
            // “For you” = recent uploads
            q.orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
        } else {
            // Filter by tag
            // NOTE: if Firestore asks for an index when you also orderBy,
            // you can remove orderBy or create the suggested index.
            q.whereArrayContains("tags", tag)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
        }

        q.get()
            .addOnSuccessListener { snap ->
                progress.visibility = View.GONE
                val docs = snap.documents.map { d ->
                    DocumentModel(
                        id = d.id,
                        title = d.getString("title") ?: "Untitled",
                        fileUrl = d.getString("fileUrl") ?: "",
                        thumbnailUrl = d.getString("thumbnailUrl"), // adding this
                        uploadedBy = d.getString("uploadedBy"),
                        uploadedByUid = d.getString("uploadedByUid"),
                        createdAt = d.getTimestamp("createdAt"),
                        tags = (d.get("tags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    )
                }

                allCurrent.clear()
                allCurrent.addAll(docs)
                // apply current search, if any
                val qText = search.text?.toString()?.trim().orEmpty()
                if (qText.isEmpty()) {
                    adapter.update(allCurrent)
                } else {
                    val filtered = allCurrent.filter {
                        it.title.contains(qText, ignoreCase = true) ||
                                (it.uploadedBy ?: "").contains(qText, ignoreCase = true) ||
                                it.tags.any { t -> t.contains(qText, ignoreCase = true) }
                    }
                    adapter.update(filtered)
                }
            }
            .addOnFailureListener {
                progress.visibility = View.GONE
                Toast.makeText(this, "Failed to load feed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
